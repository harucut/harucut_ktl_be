package com.harucut

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HarucutApplication

fun main(args: Array<String>) {
    runApplication<HarucutApplication>(*args)
}
