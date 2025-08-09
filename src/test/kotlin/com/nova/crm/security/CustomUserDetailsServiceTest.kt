package com.nova.crm.security

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.util.ReflectionTestUtils

class CustomUserDetailsServiceTest {

    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var userDetailsService: CustomUserDetailsService

    @BeforeEach
    fun setUp() {
        passwordEncoder = mockk()
        userDetailsService = CustomUserDetailsService(passwordEncoder)
        
        // Set test values using reflection
        ReflectionTestUtils.setField(userDetailsService, "adminUsername", "admin")
        ReflectionTestUtils.setField(userDetailsService, "adminPassword", "nova2024")
        
        // Mock password encoder
        every { passwordEncoder.encode("nova2024") } returns "\$2a\$10\$encodedPassword"
    }

    @Test
    fun `should load admin user successfully`() {
        // When
        val userDetails = userDetailsService.loadUserByUsername("admin")

        // Then
        assertNotNull(userDetails)
        assertEquals("admin", userDetails.username)
        assertEquals("\$2a\$10\$encodedPassword", userDetails.password)
        assertTrue(userDetails.authorities.any { it.authority == "ROLE_ADMIN" })
        assertTrue(userDetails.isEnabled)
        assertTrue(userDetails.isAccountNonExpired)
        assertTrue(userDetails.isAccountNonLocked)
        assertTrue(userDetails.isCredentialsNonExpired)
    }

    @Test
    fun `should throw exception for non-existent user`() {
        // When & Then
        assertThrows<UsernameNotFoundException> {
            userDetailsService.loadUserByUsername("nonexistent")
        }
    }

    @Test
    fun `should validate correct admin credentials`() {
        // When
        val isValid = userDetailsService.validateCredentials("admin", "nova2024")

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `should reject invalid username`() {
        // When
        val isValid = userDetailsService.validateCredentials("wronguser", "nova2024")

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `should reject invalid password`() {
        // When
        val isValid = userDetailsService.validateCredentials("admin", "wrongpassword")

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `should reject empty username`() {
        // When
        val isValid = userDetailsService.validateCredentials("", "nova2024")

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `should reject empty password`() {
        // When
        val isValid = userDetailsService.validateCredentials("admin", "")

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `should reject null credentials`() {
        // When & Then
        assertThrows<UsernameNotFoundException> {
            userDetailsService.loadUserByUsername("")
        }
    }
}
