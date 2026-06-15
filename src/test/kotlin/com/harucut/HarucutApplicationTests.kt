package com.harucut

import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class HarucutApplicationTests {

    @MockkBean
    private lateinit var redisConnectionFactory: RedisConnectionFactory

    @Test
    fun contextLoads() {
    }
}
