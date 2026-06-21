package com.harucut.config

import com.harucut.storage.property.AwsProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient
import java.net.URI

@Configuration
class MediaConvertConfig(
    private val awsProperties: AwsProperties
) {
    @Bean
    fun mediaConvertClient(): MediaConvertClient =
        MediaConvertClient.builder()
            .region(Region.of(awsProperties.region))
            .endpointOverride(URI.create(awsProperties.mediaconvert.endpoint))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        awsProperties.credentials.accessKey,
                        awsProperties.credentials.secretKey
                    )
                )
            )
            .build()
}