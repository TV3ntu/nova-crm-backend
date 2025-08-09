package com.nova.crm.controller

import com.nova.crm.dto.ErrorResponse
import com.nova.crm.dto.LoginRequest
import com.nova.crm.dto.LoginResponse
import com.nova.crm.security.CustomUserDetailsService
import com.nova.crm.security.JwtUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["*"])
@Tag(name = "Authentication", description = "Authentication endpoints for NOVA CRM")
class AuthController(
    private val jwtUtil: JwtUtil,
    private val userDetailsService: CustomUserDetailsService
) {

    @Value("\${jwt.expiration}")
    private var jwtExpiration: Long = 0

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticate user and return JWT token for accessing protected endpoints"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Login successful",
                content = [Content(schema = Schema(implementation = LoginResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid credentials",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request format",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun login(@Valid @RequestBody loginRequest: LoginRequest): ResponseEntity<Any> {
        return try {
            if (userDetailsService.validateCredentials(loginRequest.username, loginRequest.password)) {
                val token = jwtUtil.generateToken(loginRequest.username)
                val response = LoginResponse(
                    token = token,
                    username = loginRequest.username,
                    expiresIn = jwtExpiration
                )
                ResponseEntity.ok(response)
            } else {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse("Invalid credentials", 401))
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse("Authentication failed: ${e.message}", 500))
        }
    }

    @PostMapping("/validate")
    @Operation(
        summary = "Validate JWT token",
        description = "Validate if the provided JWT token is still valid"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Token is valid"
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token is invalid or expired"
            )
        ]
    )
    fun validateToken(@RequestHeader("Authorization") authHeader: String): ResponseEntity<Any> {
        return try {
            if (authHeader.startsWith("Bearer ")) {
                val token = authHeader.substring(7)
                val username = jwtUtil.getUsernameFromToken(token)
                
                if (jwtUtil.validateToken(token, username)) {
                    ResponseEntity.ok(mapOf("valid" to true, "username" to username))
                } else {
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse("Token is invalid or expired", 401))
                }
            } else {
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse("Invalid Authorization header format", 400))
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse("Token validation failed: ${e.message}", 401))
        }
    }
}
