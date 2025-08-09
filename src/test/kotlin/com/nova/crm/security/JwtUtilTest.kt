package com.nova.crm.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.util.*

class JwtUtilTest {

    private lateinit var jwtUtil: JwtUtil

    @BeforeEach
    fun setUp() {
        jwtUtil = JwtUtil()
        // Set test values using reflection
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-for-jwt-token-generation-2024")
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L) // 24 hours
    }

    @Test
    fun `should generate valid JWT token`() {
        // Given
        val username = "admin"

        // When
        val token = jwtUtil.generateToken(username)

        // Then
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        assertTrue(token.contains(".")) // JWT has dots separating header, payload, signature
    }

    @Test
    fun `should extract username from token`() {
        // Given
        val username = "admin"
        val token = jwtUtil.generateToken(username)

        // When
        val extractedUsername = jwtUtil.getUsernameFromToken(token)

        // Then
        assertEquals(username, extractedUsername)
    }

    @Test
    fun `should validate token correctly`() {
        // Given
        val username = "admin"
        val token = jwtUtil.generateToken(username)

        // When
        val isValid = jwtUtil.validateToken(token, username)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `should reject token with wrong username`() {
        // Given
        val username = "admin"
        val wrongUsername = "wronguser"
        val token = jwtUtil.generateToken(username)

        // When
        val isValid = jwtUtil.validateToken(token, wrongUsername)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `should detect non-expired token`() {
        // Given
        val username = "admin"
        val token = jwtUtil.generateToken(username)

        // When
        val isExpired = jwtUtil.isTokenExpired(token)

        // Then
        assertFalse(isExpired)
    }

    @Test
    fun `should get expiration date from token`() {
        // Given
        val username = "admin"
        val beforeGeneration = Date()
        val token = jwtUtil.generateToken(username)
        val afterGeneration = Date(System.currentTimeMillis() + 86400000L + 1000L) // Add 1 second buffer

        // When
        val expirationDate = jwtUtil.getExpirationDateFromToken(token)

        // Then
        assertNotNull(expirationDate)
        assertTrue(expirationDate.after(beforeGeneration))
        assertTrue(expirationDate.before(afterGeneration))
    }

    @Test
    fun `should throw exception for invalid token`() {
        // Given
        val invalidToken = "invalid.token.here"

        // When & Then
        assertThrows(Exception::class.java) {
            jwtUtil.getUsernameFromToken(invalidToken)
        }
    }

    @Test
    fun `should throw exception for malformed token`() {
        // Given
        val malformedToken = "not-a-jwt-token"

        // When & Then
        assertThrows(Exception::class.java) {
            jwtUtil.validateToken(malformedToken, "admin")
        }
    }
}
