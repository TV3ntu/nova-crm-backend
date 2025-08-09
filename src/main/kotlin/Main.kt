package com.nova.crm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NovaCrmApplication

fun main(args: Array<String>) {
    runApplication<NovaCrmApplication>(*args)
}