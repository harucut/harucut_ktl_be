package com.harucut.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient
import java.net.URI

@Configuration
class MediaConvertConfig(
    @Value("\${aws.credentials.access-key}") private val accessKey: String,
    @Value("\${aws.credentials.secret-key}") private val secretKey: String,
    @Value("\${aws.mediaconvert.endpoint}") private val mediaConvertEndpoint: String,
) {

    @Bean
    fun mediaConvertClient(): MediaConvertClient =
        MediaConvertClient.builder()
            .region(Region.AP_NORTHEAST_2)
            .endpointOverride(URI.create(mediaConvertEndpoint))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                )
            )
            .build()
}