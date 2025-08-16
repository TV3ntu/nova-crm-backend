package com.nova.crm.repository

import com.nova.crm.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.YearMonth

@Repository
interface PaymentRepository : JpaRepository<Payment, Long> {
    
    fun findByStudentIdAndPaymentMonth(studentId: Long, paymentMonth: YearMonth): List<Payment>
    
    fun findByDanceClassIdAndPaymentMonth(classId: Long, paymentMonth: YearMonth): List<Payment>
    
    fun findByPaymentMonth(paymentMonth: YearMonth): List<Payment>
    
    fun findByStudentId(studentId: Long): List<Payment>
    
    fun findByDanceClassId(classId: Long): List<Payment>
    
    @Query("""
        SELECT SUM(p.amount) FROM Payment p 
        WHERE p.paymentMonth = :month
    """)
    fun getTotalRevenueForMonth(@Param("month") month: YearMonth): BigDecimal?
    
    @Query("""
        SELECT SUM(p.amount) FROM Payment p 
        WHERE p.danceClass.id = :classId AND p.paymentMonth = :month
    """)
    fun getTotalRevenueForClassAndMonth(
        @Param("classId") classId: Long,
        @Param("month") month: YearMonth
    ): BigDecimal?
    
    @Query("""
        SELECT p FROM Payment p 
        WHERE p.student.id = :studentId 
        AND p.danceClass.id = :classId 
        AND p.paymentMonth = :month
    """)
    fun findByStudentAndClassAndMonth(
        @Param("studentId") studentId: Long,
        @Param("classId") classId: Long,
        @Param("month") month: YearMonth
    ): Payment?
    
    // Delete all payments for a student (for cascade deletion)
    fun deleteByStudentId(studentId: Long)
    
    @Query("""
        SELECT p FROM Payment p 
        WHERE p.isLatePayment = true 
        AND p.paymentMonth = :month
    """)
    fun findLatePaymentsForMonth(@Param("month") month: YearMonth): List<Payment>
}
