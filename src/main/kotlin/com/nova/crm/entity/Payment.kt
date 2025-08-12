package com.nova.crm.entity

import com.nova.crm.entity.PaymentMethod
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Entity
@Table(name = "payments")
data class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @NotNull(message = "Student is required")
    val student: Student,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    @NotNull(message = "Dance class is required")
    val danceClass: DanceClass,

    @Column(nullable = false, precision = 10, scale = 2)
    @Positive(message = "Amount must be positive")
    val amount: BigDecimal,

    @Column(nullable = false)
    val paymentDate: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    val paymentMonth: YearMonth,

    @Column(nullable = false)
    val isLatePayment: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,

    val notes: String? = null
) {
    /**
     * Calculate if this payment was made after the 10th of the month
     */
    fun isPaymentLate(): Boolean {
        return paymentDate.dayOfMonth > 10 && 
               YearMonth.from(paymentDate) == paymentMonth
    }

    /**
     * Calculate the expected amount based on class price and late fee
     */
    fun calculateExpectedAmount(): BigDecimal {
        val baseAmount = danceClass.price
        return if (isLatePayment) {
            baseAmount.multiply(BigDecimal("1.15")) // 15% late fee
        } else {
            baseAmount
        }
    }

    override fun toString(): String {
        return "Payment(id=$id, student=${student.fullName}, class=${danceClass.name}, amount=$amount, month=$paymentMonth)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Payment
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
