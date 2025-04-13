package com.example.majorapplication

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.example.majorapplication"])
@ConfigurationPropertiesScan
class MajorapplicationApplication

fun main(args: Array<String>) {
    runApplication<MajorapplicationApplication>(*args)
}
