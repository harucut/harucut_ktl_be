package com.harucut.common.controller

import com.harucut.util.response.Response
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController {

    @GetMapping("/")
    fun root(): ResponseEntity<Response<Map<String, String>>> =
        Response.ok(
            mapOf(
                "service" to "harucut-api",
                "status" to "UP",
            )
        ).toResponseEntity()
}