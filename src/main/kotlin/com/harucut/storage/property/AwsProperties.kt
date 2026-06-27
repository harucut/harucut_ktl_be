package com.harucut.storage.property

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cloud.aws")
data class AwsProperties(
    val region: String,
    val credentials: Credentials,
    val s3: S3
) {
    data class Credentials(
        val accessKey: String,
        val secretKey: String
    )

    data class S3(
        val bucket: String
    )
}
