package com.nova.crm.dto

import com.nova.crm.entity.Payment
import com.nova.crm.entity.PaymentMethod
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class CreatePaymentRequest(
    @field:NotNull(message = "Student ID is required")
    val studentId: Long,
    
    @field:NotNull(message = "Class ID is required")
    val classId: Long,
    
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal,
    
    @field:NotNull(message = "Payment month is required")
    val paymentMonth: YearMonth,
    
    val paymentDate: LocalDate = LocalDate.now(),
    
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    
    val notes: String? = null
)

data class CreateMultiClassPaymentRequest(
    @field:NotNull(message = "Student ID is required")
    val studentId: Long,
    
    @field:Positive(message = "Total amount must be positive")
    val totalAmount: BigDecimal,
    
    @field:NotNull(message = "Payment month is required")
    val paymentMonth: YearMonth,
    
    val paymentDate: LocalDate = LocalDate.now(),
    
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    
    val notes: String? = null
)

data class UpdatePaymentRequest(
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal? = null,
    
    val paymentDate: LocalDate? = null,
    
    val paymentMethod: PaymentMethod? = null,
    
    val notes: String? = null
)

data class PaymentResponse(
    val id: Long,
    val studentId: Long,
    val studentName: String,
    val classId: Long,
    val className: String,
    val amount: BigDecimal,
    val paymentDate: LocalDate,
    val paymentMonth: YearMonth,
    val isLatePayment: Boolean,
    val paymentMethod: PaymentMethod,
    val notes: String?
) {
    companion object {
        fun from(payment: Payment): PaymentResponse {
            return PaymentResponse(
                id = payment.id,
                studentId = payment.student.id,
                studentName = payment.student.fullName,
                classId = payment.danceClass.id,
                className = payment.danceClass.name,
                amount = payment.amount,
                paymentDate = payment.paymentDate,
                paymentMonth = payment.paymentMonth,
                isLatePayment = payment.isLatePayment,
                paymentMethod = payment.paymentMethod,
                notes = payment.notes
            )
        }
    }
}
