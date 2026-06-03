package com.harucut.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
class S3Config(
    @Value("\${aws.s3.region}") private val region: String,
    @Value("\${aws.credentials.access-key}") private val accessKey: String,
    @Value("\${aws.credentials.secret-key}") private val secretKey: String,
) {

    private fun credentialsProvider(): StaticCredentialsProvider =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))

    @Bean
    fun s3Client(): S3Client =
        S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider())
            .build()

    @Bean
    fun s3Presigner(): S3Presigner =
        S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider())
            .build()
}