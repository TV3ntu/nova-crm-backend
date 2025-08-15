package com.nova.crm.entity

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Payment method options")
enum class PaymentMethod(val displayName: String) {
    @Schema(description = "Bank transfer payment")
    TRANSFERENCIA("Transferencia"),
    
    @Schema(description = "Credit or debit card payment")
    TARJETA("Tarjeta de Crédito/Débito"),
    
    @Schema(description = "Cash payment")
    EFECTIVO("Efectivo");
    
    companion object {
        fun fromDisplayName(displayName: String): PaymentMethod? {
            return values().find { it.displayName.equals(displayName, ignoreCase = true) }
        }
    }
}
