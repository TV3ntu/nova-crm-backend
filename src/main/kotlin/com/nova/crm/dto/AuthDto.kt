package com.nova.crm.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Login request")
data class LoginRequest(
    @field:NotBlank(message = "Username is required")
    @Schema(description = "Username for authentication", example = "admin")
    val username: String,
    
    @field:NotBlank(message = "Password is required")
    @Schema(description = "Password for authentication", example = "nova2024")
    val password: String
)

@Schema(description = "Login response with JWT token")
data class LoginResponse(
    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val token: String,
    
    @Schema(description = "Token type", example = "Bearer")
    val tokenType: String = "Bearer",
    
    @Schema(description = "Username of authenticated user", example = "admin")
    val username: String,
    
    @Schema(description = "Token expiration time in milliseconds", example = "86400000")
    val expiresIn: Long
)

@Schema(description = "Error response")
data class ErrorResponse(
    @Schema(description = "Error message", example = "Invalid credentials")
    val message: String,
    
    @Schema(description = "HTTP status code", example = "401")
    val status: Int,
    
    @Schema(description = "Timestamp", example = "2024-02-15T10:30:00")
    val timestamp: String = java.time.LocalDateTime.now().toString()
)
