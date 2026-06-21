package com.harucut.storage.property

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cloud.aws")
data class AwsProperties(
    val region: String,
    val credentials: Credentials,
    val s3: S3,
    val mediaconvert: MediaConvert
) {
    data class Credentials(
        val accessKey: String,
        val secretKey: String
    )

    data class S3(
        val bucket: String
    )

    data class MediaConvert(
        val endpoint: String,
        val roleArn: String,
        val templateName: String
    )
}
