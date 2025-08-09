package com.nova.crm.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

@Configuration
@Profile("prod")
class DatabaseConfig {

    @Bean
    @ConfigurationProperties("spring.datasource")
    fun dataSource(): DataSource {
        val databaseUrl = System.getenv("DATABASE_URL")
        
        return if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
            // Transform Render's postgresql:// URL to jdbc:postgresql://
            val jdbcUrl = databaseUrl.replace("postgresql://", "jdbc:postgresql://")
            
            DataSourceBuilder.create()
                .url(jdbcUrl)
                .driverClassName("org.postgresql.Driver")
                .build()
        } else {
            // Fallback to default configuration
            DataSourceBuilder.create()
                .url("jdbc:postgresql://localhost:5432/nova_crm")
                .driverClassName("org.postgresql.Driver")
                .username("nova_user")
                .password("nova_password")
                .build()
        }
    }
}
