package com.nova.crm.config

import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.net.URI
import javax.sql.DataSource

@Configuration
@Profile("prod")
class DatabaseConfig {

    @Bean
    @Primary
    fun dataSource(): DataSource {
        val databaseUrl = System.getenv("DATABASE_URL")
        
        return if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
            try {
                // Parse the malformed URL and fix it
                val fixedUrl = parseAndFixPostgreSQLUrl(databaseUrl)
                val uri = URI(fixedUrl.replace("postgresql://", ""))
                
                val jdbcUrl = "jdbc:postgresql://${uri.host}:${uri.port}${uri.path}"
                val username = uri.userInfo?.split(":")?.get(0) ?: "postgres"
                val password = uri.userInfo?.split(":")?.get(1) ?: ""
                
                println(" DatabaseConfig: Original URL: $databaseUrl")
                println(" DatabaseConfig: Fixed URL: $fixedUrl")
                println(" DatabaseConfig: JDBC URL: $jdbcUrl")
                println(" DatabaseConfig: Username: $username")
                
                DataSourceBuilder.create()
                    .url(jdbcUrl)
                    .username(username)
                    .password(password)
                    .driverClassName("org.postgresql.Driver")
                    .build()
            } catch (e: Exception) {
                println(" DatabaseConfig: Error parsing URL: ${e.message}")
                println("  DatabaseConfig: Using fallback configuration")
                createFallbackDataSource()
            }
        } else {
            println("  DatabaseConfig: DATABASE_URL not found or invalid format")
            println("  DatabaseConfig: Using fallback localhost configuration")
            createFallbackDataSource()
        }
    }
    
    private fun parseAndFixPostgreSQLUrl(url: String): String {
        // Handle malformed URLs from Render that might be missing port
        var fixedUrl = url
        
        // Check if URL is missing port (common Render issue)
        if (!url.contains(":5432") && !url.contains("@") && url.contains("dpg-")) {
            // This is likely a malformed Render URL, try to fix it
            val parts = url.split("@")
            if (parts.size == 2) {
                val userPart = parts[0] // postgresql://user:pass
                val hostPart = parts[1] // host/database
                
                // Add default port if missing
                if (!hostPart.contains(":")) {
                    val hostAndDb = hostPart.split("/")
                    if (hostAndDb.size == 2) {
                        fixedUrl = "$userPart@${hostAndDb[0]}:5432/${hostAndDb[1]}"
                    }
                }
            }
        }
        
        return fixedUrl
    }
    
    private fun createFallbackDataSource(): DataSource {
        return DataSourceBuilder.create()
            .url("jdbc:postgresql://localhost:5432/nova_crm")
            .driverClassName("org.postgresql.Driver")
            .username("nova_user")
            .password("nova_password")
            .build()
    }
}
