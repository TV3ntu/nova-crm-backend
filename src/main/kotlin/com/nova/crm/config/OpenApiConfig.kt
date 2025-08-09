package com.nova.crm.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info as ModelInfo
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme as ModelSecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "NOVA Dance Studio CRM API",
        version = "1.0.0",
        description = "Comprehensive Customer Relationship Management system for NOVA dance studio with student management, teacher management, class scheduling, payment processing, and comprehensive reporting features.",
        contact = Contact(
            name = "NOVA Dance Studio",
            email = "admin@novadanza.com"
        ),
        license = License(
            name = "Proprietary",
            url = "https://novadanza.com/license"
        )
    ),
    servers = [
        Server(
            url = "http://localhost:8080",
            description = "Development Server"
        )
    ]
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
    description = "JWT token for API authentication. Login with admin credentials to obtain token."
)
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                ModelInfo()
                    .title("NOVA Dance Studio CRM API")
                    .version("1.0.0")
                    .description("Comprehensive Customer Relationship Management system for NOVA dance studio")
            )
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
            .components(
                io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        ModelSecurityScheme()
                            .type(ModelSecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT token for API authentication")
                    )
            )
    }
}
