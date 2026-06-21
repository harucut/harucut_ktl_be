package com.harucut.config

import com.harucut.storage.property.AwsProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
@EnableConfigurationProperties(AwsProperties::class)
class S3Config(
    private val awsProperties: AwsProperties
) {

    private fun credentialsProvider(): StaticCredentialsProvider =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                awsProperties.credentials.accessKey,
                awsProperties.credentials.secretKey
            )
        )

    @Bean
    fun s3Client(): S3Client =
        S3Client.builder()
            .region(Region.of(awsProperties.region))
            .credentialsProvider(credentialsProvider())
            .build()

    @Bean
    fun s3Presigner(): S3Presigner =
        S3Presigner.builder()
            .region(Region.of(awsProperties.region))
            .credentialsProvider(credentialsProvider())
            .build()
}