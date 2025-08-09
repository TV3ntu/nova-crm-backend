package com.nova.crm.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val passwordEncoder: PasswordEncoder
) : UserDetailsService {

    @Value("\${admin.username}")
    private lateinit var adminUsername: String

    @Value("\${admin.password}")
    private lateinit var adminPassword: String

    override fun loadUserByUsername(username: String): UserDetails {
        if (username == adminUsername) {
            return User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .authorities(listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
                .build()
        }
        
        throw UsernameNotFoundException("User not found: $username")
    }

    fun validateCredentials(username: String, password: String): Boolean {
        return username == adminUsername && password == adminPassword
    }
}
