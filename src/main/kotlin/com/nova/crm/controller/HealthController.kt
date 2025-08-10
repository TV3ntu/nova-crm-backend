package com.nova.crm.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class HealthController {

    @GetMapping("/")
    fun root(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "application" to "NOVA Dance Studio CRM",
            "version" to "1.0.0",
            "timestamp" to LocalDateTime.now(),
            "message" to "Welcome to NOVA CRM API! Visit /swagger-ui.html for documentation."
        )
    }

    @GetMapping("/health")
    fun health(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "database" to "Connected"
        )
    }

    @GetMapping("/api/health")
    fun apiHealth(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "api" to "Ready",
            "endpoints" to listOf(
                "POST /api/auth/login",
                "GET /api/students",
                "GET /api/teachers", 
                "GET /api/classes",
                "GET /api/payments",
                "GET /swagger-ui.html"
            )
        )
    }
}
