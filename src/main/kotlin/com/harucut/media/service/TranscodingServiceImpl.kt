package com.harucut.media.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.media.dto.TranscodeTaskState
import com.harucut.media.dto.TranscodeTaskStatusResponse
import com.harucut.media.dto.TranscodeTaskSubmitResponse
import com.harucut.media.policy.MediaSubscriptionPolicy
import com.harucut.media.repository.TranscodeTaskRepository
import com.harucut.storage.exception.StorageErrorCode
import com.harucut.storage.property.AwsProperties
import com.harucut.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient
import software.amazon.awssdk.services.mediaconvert.model.*
import java.time.LocalDateTime
import java.util.*

@Service
class TranscodingServiceImpl(
    private val mediaConvertClient: MediaConvertClient,
    private val userMediaService: UserMediaService,
    private val transcodeTaskRepository: TranscodeTaskRepository,
    private val userRepository: UserRepository,
    private val subscriptionPolicy: MediaSubscriptionPolicy,
    private val awsProperties: AwsProperties
) : TranscodingService {

    private val log = LoggerFactory.getLogger(javaClass)

    private val bucket get() = awsProperties.s3.bucket
    private val roleArn get() = awsProperties.mediaconvert.roleArn
    private val templateName get() = awsProperties.mediaconvert.templateName

    // 변환 작업 제출 (업로드 쿼터 차감 → QUEUED 저장 → job 생성 → SUBMITTED)
    override fun submitTranscodeTask(
        userPublicId: String,
        fileName: String
    ): TranscodeTaskSubmitResponse {
        val user = userRepository.findByPublicId(userPublicId)
            ?: throw BusinessException(GlobalErrorCode.NOT_FOUND, "User not found.")
        subscriptionPolicy.assertAndConsumeVideoUploadQuota(user)

        val taskId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        val queued = TranscodeTaskState.queued(taskId, userPublicId, fileName, now)
        transcodeTaskRepository.save(queued)

        val jobId = createConversionJob(userPublicId, fileName)
        val submittedAt = LocalDateTime.now()
        val submitted = queued.submitted(jobId, submittedAt)
        transcodeTaskRepository.save(submitted)
        transcodeTaskRepository.linkJobToTask(jobId, taskId)

        return TranscodeTaskSubmitResponse(taskId, jobId, submitted.status, submittedAt)
    }

    // taskId 기준 변환 상태 조회 (소유권 검증)
    override fun getTaskStatus(taskId: String, userPublicId: String): TranscodeTaskStatusResponse {
        val state = getTaskState(taskId, userPublicId)
        return TranscodeTaskStatusResponse(
            state.taskId, state.jobId, state.status, state.errorMessage,
            state.media, state.createdAt, state.updatedAt
        )
    }

    // 웹훅: 진행 중 상태 반영
    override fun markProgressing(jobId: String) =
        updateStateByJobId(jobId) { it.progressing(LocalDateTime.now()) }

    // 웹훅: 변환 완료 처리 (변환본/썸네일 영속화 후 COMPLETE 전이)
    override fun handleCompletedJob(
        jobId: String,
        userPublicId: String?,
        originalFileName: String?,
        outputS3Path: String,
        thumbnailS3Path: String?
    ) = updateStateByJobId(jobId) { state ->
        val media = userMediaService.saveTranscodedVideo(
            userPublicId ?: state.userPublicId,
            originalFileName,
            outputS3Path,
            thumbnailS3Path,
            jobId
        )
        state.completed(media, LocalDateTime.now())
    }

    // 웹훅: 변환 실패 상태 반영
    override fun handleFailedJob(jobId: String, errorMessage: String) =
        updateStateByJobId(jobId) { it.failed(errorMessage, LocalDateTime.now()) }

    // ── helpers ───────────────────────

    // taskId로 상태 로드 + 소유권 검증
    private fun getTaskState(taskId: String, userPublicId: String): TranscodeTaskState {
        val state = transcodeTaskRepository.findByTaskId(taskId)
            ?: throw BusinessException(GlobalErrorCode.NOT_FOUND, "Transcode task not found.")
        if (state.userPublicId != userPublicId) {
            throw BusinessException(GlobalErrorCode.FORBIDDEN, "Transcode task belongs to another user.")
        }
        return state
    }

    // jobId → taskId → 상태 로드 후 전이 적용·저장
    private fun updateStateByJobId(jobId: String, updater: (TranscodeTaskState) -> TranscodeTaskState) {
        val taskId = transcodeTaskRepository.findTaskIdByJobId(jobId)
        val state = taskId?.let { transcodeTaskRepository.findByTaskId(it) }
        if (state == null) {
            log.warn("jobId로 변환 작업을 찾을 수 없습니다. jobId={}", jobId)
            return
        }
        val updated = updater(state)
        transcodeTaskRepository.save(updated)
        log.info("변환 작업 상태 갱신. taskId={}, status={}", updated.taskId, updated.status)
    }

    // MediaConvert 변환 job 생성 (mp4 + 썸네일 2개 출력 그룹)
    private fun createConversionJob(userPublicId: String, fileName: String): String {
        val inputS3Path = "s3://$bucket/uploads/users/$userPublicId/webm/$fileName"
        val mp4OutputPath = "s3://$bucket/uploads/users/$userPublicId/mp4/"
        val thumbnailOutputPath = "s3://$bucket/uploads/users/$userPublicId/thumbnail/"

        // mp4 출력: 인코딩 설정은 Job Template(templateName)에 위임
        val mp4Group = OutputGroup.builder()
            .name("File Group")
            .outputs(Output.builder().build())
            .outputGroupSettings(
                OutputGroupSettings.builder()
                    .type(OutputGroupType.FILE_GROUP_SETTINGS)
                    .fileGroupSettings(FileGroupSettings.builder().destination(mp4OutputPath).build())
                    .build()
            )
            .build()

        // 썸네일: 단일 프레임 JPG 캡처
        val thumbnailGroup = OutputGroup.builder()
            .name("Thumbnail Group")
            .outputs(
                Output.builder()
                    .containerSettings(ContainerSettings.builder().container(ContainerType.RAW).build())
                    .videoDescription(
                        VideoDescription.builder()
                            .codecSettings(
                                VideoCodecSettings.builder()
                                    .codec(VideoCodec.FRAME_CAPTURE)
                                    .frameCaptureSettings(
                                        FrameCaptureSettings.builder()
                                            .framerateNumerator(1)
                                            .framerateDenominator(1)
                                            .maxCaptures(1)
                                            .quality(80)
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .outputGroupSettings(
                OutputGroupSettings.builder()
                    .type(OutputGroupType.FILE_GROUP_SETTINGS)
                    .fileGroupSettings(FileGroupSettings.builder().destination(thumbnailOutputPath).build())
                    .build()
            )
            .build()

        val createJobRequest = CreateJobRequest.builder()
            .role(roleArn)
            .jobTemplate(templateName)
            .settings(
                JobSettings.builder()
                    .inputs(Input.builder().fileInput(inputS3Path).build())
                    .outputGroups(mp4Group, thumbnailGroup)
                    .build()
            )
            .userMetadata(mapOf("userPublicId" to userPublicId, "originalFileName" to fileName))
            .build()

        return try {
            val response = mediaConvertClient.createJob(createJobRequest)
            val jobId = response.job().id()
            log.info("변환 작업 생성 완료. jobId={}", jobId)
            jobId
        } catch (e: MediaConvertException) {
            log.error("AWS MediaConvert 오류. error={}", e.message)
            throw BusinessException(StorageErrorCode.TRANSCODE_FAILED, "Failed to submit video transcoding job.")
        }
    }
}
