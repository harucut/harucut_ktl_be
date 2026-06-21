package com.harucut.media.service

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.media.dto.TranscodeTaskState
import com.harucut.media.dto.UserMediaResponse
import com.harucut.media.enums.TranscodeTaskStatus
import com.harucut.media.enums.UserMediaType
import com.harucut.media.policy.MediaSubscriptionPolicy
import com.harucut.media.repository.TranscodeTaskRepository
import com.harucut.storage.exception.StorageErrorCode
import com.harucut.storage.property.AwsProperties
import com.harucut.user.entity.User
import com.harucut.user.repository.UserRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient
import software.amazon.awssdk.services.mediaconvert.model.CreateJobRequest
import software.amazon.awssdk.services.mediaconvert.model.CreateJobResponse
import software.amazon.awssdk.services.mediaconvert.model.Job
import software.amazon.awssdk.services.mediaconvert.model.MediaConvertException
import java.time.LocalDateTime

class TranscodingServiceTest {

    private val mediaConvertClient = mockk<MediaConvertClient>()
    private val userMediaService = mockk<UserMediaService>()
    private val transcodeTaskRepository = mockk<TranscodeTaskRepository>()
    private val userRepository = mockk<UserRepository>()
    private val subscriptionPolicy = mockk<MediaSubscriptionPolicy>(relaxed = true)

    private val awsProperties = AwsProperties(
        region = "ap-northeast-2",
        credentials = AwsProperties.Credentials("ak", "sk"),
        s3 = AwsProperties.S3("harucut-test"),
        mediaconvert = AwsProperties.MediaConvert("https://mc", "role-arn", "tmpl")
    )

    private val service = TranscodingServiceImpl(
        mediaConvertClient, userMediaService, transcodeTaskRepository,
        userRepository, subscriptionPolicy, awsProperties
    )

    private fun state(
        taskId: String = "t1",
        userPublicId: String = "pub-1",
        status: TranscodeTaskStatus = TranscodeTaskStatus.SUBMITTED
    ): TranscodeTaskState {
        val now = LocalDateTime.now()
        return TranscodeTaskState(taskId, userPublicId, "v.webm", "job-1", status, null, null, now, now)
    }

    @Nested
    inner class SubmitTranscodeTask {

        @Test
        @DisplayName("쿼터 차감 후 mp4·썸네일 2개 출력 그룹으로 job을 생성하고 SUBMITTED 응답을 반환한다")
        fun success() {
            // given
            val user = mockk<User>()
            every { userRepository.findByPublicId("pub-1") } returns user
            every { transcodeTaskRepository.save(any()) } just runs
            every { transcodeTaskRepository.linkJobToTask(any(), any()) } just runs
            val jobReq = slot<CreateJobRequest>()
            every { mediaConvertClient.createJob(capture(jobReq)) } returns
                CreateJobResponse.builder().job(Job.builder().id("job-123").build()).build()

            // when
            val res = service.submitTranscodeTask("pub-1", "v.webm")

            // then
            assertThat(res.jobId).isEqualTo("job-123")
            assertThat(res.status).isEqualTo(TranscodeTaskStatus.SUBMITTED)
            assertThat(res.taskId).isNotBlank()
            verify { subscriptionPolicy.assertAndConsumeVideoUploadQuota(user) }
            verify { transcodeTaskRepository.linkJobToTask("job-123", any()) }

            val captured = jobReq.captured
            assertThat(captured.settings().outputGroups()).hasSize(2)
            assertThat(captured.jobTemplate()).isEqualTo("tmpl")
            assertThat(captured.role()).isEqualTo("role-arn")
            assertThat(captured.settings().inputs()[0].fileInput())
                .isEqualTo("s3://harucut-test/uploads/users/pub-1/webm/v.webm")
            assertThat(captured.userMetadata())
                .containsEntry("userPublicId", "pub-1")
                .containsEntry("originalFileName", "v.webm")
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 NOT_FOUND 예외를 던진다")
        fun userNotFound() {
            every { userRepository.findByPublicId("pub-1") } returns null

            assertThatThrownBy { service.submitTranscodeTask("pub-1", "v.webm") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.NOT_FOUND)
        }

        @Test
        @DisplayName("MediaConvert 호출이 실패하면 TRANSCODE_FAILED 예외를 던진다")
        fun mediaConvertFails() {
            val user = mockk<User>()
            every { userRepository.findByPublicId("pub-1") } returns user
            every { transcodeTaskRepository.save(any()) } just runs
            every { mediaConvertClient.createJob(any<CreateJobRequest>()) } throws
                (MediaConvertException.builder().message("boom").build() as MediaConvertException)

            assertThatThrownBy { service.submitTranscodeTask("pub-1", "v.webm") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(StorageErrorCode.TRANSCODE_FAILED)
        }
    }

    @Nested
    inner class GetTaskStatus {

        @Test
        @DisplayName("소유한 작업이면 상태 응답을 반환한다")
        fun success() {
            every { transcodeTaskRepository.findByTaskId("t1") } returns state()

            val res = service.getTaskStatus("t1", "pub-1")

            assertThat(res.taskId).isEqualTo("t1")
            assertThat(res.status).isEqualTo(TranscodeTaskStatus.SUBMITTED)
        }

        @Test
        @DisplayName("작업이 없으면 NOT_FOUND 예외를 던진다")
        fun notFound() {
            every { transcodeTaskRepository.findByTaskId("t1") } returns null

            assertThatThrownBy { service.getTaskStatus("t1", "pub-1") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.NOT_FOUND)
        }

        @Test
        @DisplayName("다른 사용자의 작업이면 FORBIDDEN 예외를 던진다")
        fun forbidden() {
            every { transcodeTaskRepository.findByTaskId("t1") } returns state(userPublicId = "other")

            assertThatThrownBy { service.getTaskStatus("t1", "pub-1") }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.FORBIDDEN)
        }
    }

    @Nested
    inner class WebhookCallbacks {

        @Test
        @DisplayName("진행 알림이면 상태를 PROGRESSING으로 저장한다")
        fun markProgressing() {
            every { transcodeTaskRepository.findTaskIdByJobId("job-1") } returns "t1"
            every { transcodeTaskRepository.findByTaskId("t1") } returns state()
            val saved = slot<TranscodeTaskState>()
            every { transcodeTaskRepository.save(capture(saved)) } just runs

            service.markProgressing("job-1")

            assertThat(saved.captured.status).isEqualTo(TranscodeTaskStatus.PROGRESSING)
        }

        @Test
        @DisplayName("매핑되지 않은 jobId면 아무 것도 저장하지 않는다")
        fun unknownJobId() {
            every { transcodeTaskRepository.findTaskIdByJobId("job-x") } returns null

            service.markProgressing("job-x")

            verify(exactly = 0) { transcodeTaskRepository.save(any()) }
        }

        @Test
        @DisplayName("완료 알림이면 변환본을 저장하고 상태를 COMPLETE로 만든다")
        fun completed() {
            every { transcodeTaskRepository.findTaskIdByJobId("job-1") } returns "t1"
            every { transcodeTaskRepository.findByTaskId("t1") } returns state()
            val media = UserMediaResponse(
                1L, UserMediaType.VIDEO, "uploads/users/pub-1/mp4/v.mp4", "v.mp4",
                null, "https://thumb", null, "v.webm", "job-1", LocalDateTime.now()
            )
            every {
                userMediaService.saveTranscodedVideo("pub-1", "v.webm", "mp4path", "thumbpath", "job-1")
            } returns media
            val saved = slot<TranscodeTaskState>()
            every { transcodeTaskRepository.save(capture(saved)) } just runs

            service.handleCompletedJob("job-1", "pub-1", "v.webm", "mp4path", "thumbpath")

            assertThat(saved.captured.status).isEqualTo(TranscodeTaskStatus.COMPLETE)
            assertThat(saved.captured.media).isEqualTo(media)
            verify { userMediaService.saveTranscodedVideo("pub-1", "v.webm", "mp4path", "thumbpath", "job-1") }
        }

        @Test
        @DisplayName("실패 알림이면 상태를 ERROR로 만들고 메시지를 저장한다")
        fun failed() {
            every { transcodeTaskRepository.findTaskIdByJobId("job-1") } returns "t1"
            every { transcodeTaskRepository.findByTaskId("t1") } returns state()
            val saved = slot<TranscodeTaskState>()
            every { transcodeTaskRepository.save(capture(saved)) } just runs

            service.handleFailedJob("job-1", "변환 실패")

            assertThat(saved.captured.status).isEqualTo(TranscodeTaskStatus.ERROR)
            assertThat(saved.captured.errorMessage).isEqualTo("변환 실패")
        }
    }
}
