package com.nova.crm.controller

import com.nova.crm.dto.LoginRequest
import com.nova.crm.security.CustomUserDetailsService
import com.nova.crm.security.JwtUtil
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.util.ReflectionTestUtils

class AuthControllerTest {

    private lateinit var jwtUtil: JwtUtil
    private lateinit var userDetailsService: CustomUserDetailsService
    private lateinit var authController: AuthController

    @BeforeEach
    fun setUp() {
        jwtUtil = mockk()
        userDetailsService = mockk()
        authController = AuthController(jwtUtil, userDetailsService)
        
        // Set JWT expiration for the controller
        ReflectionTestUtils.setField(authController, "jwtExpiration", 86400000L)
    }

    @Test
    fun `should login successfully with valid credentials`() {
        // Given
        val loginRequest = LoginRequest("admin", "nova2024")
        val expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token"

        every { userDetailsService.validateCredentials("admin", "nova2024") } returns true
        every { jwtUtil.generateToken("admin") } returns expectedToken

        // When
        val response = authController.login(loginRequest)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        val loginResponse = response.body as Map<*, *>
        assertEquals(expectedToken, loginResponse["token"])
        assertEquals("Bearer", loginResponse["tokenType"])
        assertEquals("admin", loginResponse["username"])
        assertEquals(86400000L, loginResponse["expiresIn"])
    }

    @Test
    fun `should reject login with invalid credentials`() {
        // Given
        val loginRequest = LoginRequest("admin", "wrongpassword")

        every { userDetailsService.validateCredentials("admin", "wrongpassword") } returns false

        // When
        val response = authController.login(loginRequest)

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        val errorResponse = response.body as Map<*, *>
        assertEquals("Invalid credentials", errorResponse["message"])
        assertEquals(401, errorResponse["status"])
    }

    @Test
    fun `should validate valid JWT token`() {
        // Given
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token"
        val authHeader = "Bearer $token"

        every { jwtUtil.getUsernameFromToken(token) } returns "admin"
        every { jwtUtil.validateToken(token, "admin") } returns true

        // When
        val response = authController.validateToken(authHeader)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        val validationResponse = response.body as Map<*, *>
        assertEquals(true, validationResponse["valid"])
        assertEquals("admin", validationResponse["username"])
    }

    @Test
    fun `should reject invalid JWT token`() {
        // Given
        val token = "invalid.jwt.token"
        val authHeader = "Bearer $token"

        every { jwtUtil.getUsernameFromToken(token) } throws RuntimeException("Invalid token")

        // When
        val response = authController.validateToken(authHeader)

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        val errorResponse = response.body as Map<*, *>
        assertTrue(errorResponse["message"].toString().contains("Token validation failed"))
        assertEquals(401, errorResponse["status"])
    }

    @Test
    fun `should reject malformed authorization header`() {
        // Given
        val authHeader = "InvalidFormat token"

        // When
        val response = authController.validateToken(authHeader)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val errorResponse = response.body as Map<*, *>
        assertEquals("Invalid Authorization header format", errorResponse["message"])
        assertEquals(400, errorResponse["status"])
    }

    @Test
    fun `should handle JWT generation exception`() {
        // Given
        val loginRequest = LoginRequest("admin", "nova2024")

        every { userDetailsService.validateCredentials("admin", "nova2024") } returns true
        every { jwtUtil.generateToken("admin") } throws RuntimeException("Token generation failed")

        // When
        val response = authController.login(loginRequest)

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        val errorResponse = response.body as Map<*, *>
        assertTrue(errorResponse["message"].toString().contains("Authentication failed"))
        assertEquals(500, errorResponse["status"])
    }
}
