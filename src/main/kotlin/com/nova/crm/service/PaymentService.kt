package com.nova.crm.service

import com.nova.crm.entity.DanceClass
import com.nova.crm.entity.Payment
import com.nova.crm.entity.Student
import com.nova.crm.repository.DanceClassRepository
import com.nova.crm.repository.PaymentRepository
import com.nova.crm.repository.StudentRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Service
@Transactional
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val studentRepository: StudentRepository,
    private val danceClassRepository: DanceClassRepository
) {

    fun findAll(): List<Payment> = paymentRepository.findAll()

    fun findById(id: Long): Payment? = paymentRepository.findByIdOrNull(id)

    fun findByStudentId(studentId: Long): List<Payment> = paymentRepository.findByStudentId(studentId)

    fun findByClassId(classId: Long): List<Payment> = paymentRepository.findByDanceClassId(classId)

    fun findByMonth(month: YearMonth): List<Payment> = paymentRepository.findByPaymentMonth(month)

    fun registerPayment(
        studentId: Long,
        classId: Long,
        amount: BigDecimal,
        paymentMonth: YearMonth,
        paymentDate: LocalDate = LocalDate.now(),
        notes: String? = null
    ): Payment {
        val student = studentRepository.findByIdOrNull(studentId)
            ?: throw IllegalArgumentException("Student not found with id: $studentId")
        
        val danceClass = danceClassRepository.findByIdOrNull(classId)
            ?: throw IllegalArgumentException("Class not found with id: $classId")

        // Verify student is enrolled in the class
        if (!student.classes.contains(danceClass)) {
            throw IllegalStateException("Student ${student.fullName} is not enrolled in class ${danceClass.name}")
        }

        // Check if payment already exists for this student, class, and month
        val existingPayment = paymentRepository.findByStudentAndClassAndMonth(studentId, classId, paymentMonth)
        if (existingPayment != null) {
            throw IllegalStateException("Payment already exists for ${student.fullName} in ${danceClass.name} for $paymentMonth")
        }

        // Determine if payment is late
        val isLate = paymentDate.dayOfMonth > 10 && YearMonth.from(paymentDate) == paymentMonth

        val payment = Payment(
            student = student,
            danceClass = danceClass,
            amount = amount,
            paymentDate = paymentDate,
            paymentMonth = paymentMonth,
            isLatePayment = isLate,
            notes = notes
        )

        return paymentRepository.save(payment)
    }

    fun registerMultiClassPayment(
        studentId: Long,
        totalAmount: BigDecimal,
        paymentMonth: YearMonth,
        paymentDate: LocalDate = LocalDate.now(),
        notes: String? = null
    ): List<Payment> {
        val student = studentRepository.findByIdOrNull(studentId)
            ?: throw IllegalArgumentException("Student not found with id: $studentId")

        if (student.classes.isEmpty()) {
            throw IllegalStateException("Student ${student.fullName} is not enrolled in any classes")
        }

        // Get classes that don't have payments for this month
        val unpaidClasses = student.classes.filter { danceClass ->
            paymentRepository.findByStudentAndClassAndMonth(studentId, danceClass.id, paymentMonth) == null
        }

        if (unpaidClasses.isEmpty()) {
            throw IllegalStateException("All classes for ${student.fullName} are already paid for $paymentMonth")
        }

        // Calculate total expected amount
        val totalExpectedAmount = unpaidClasses.sumOf { it.price }
        
        // Validate that the payment amount covers all classes
        if (totalAmount < totalExpectedAmount) {
            throw IllegalArgumentException(
                "Payment amount $totalAmount is insufficient. Expected: $totalExpectedAmount for classes: ${unpaidClasses.map { it.name }}"
            )
        }

        // Determine if payment is late
        val isLate = paymentDate.dayOfMonth > 10 && YearMonth.from(paymentDate) == paymentMonth

        // Create payments for each unpaid class
        val payments = unpaidClasses.map { danceClass ->
            Payment(
                student = student,
                danceClass = danceClass,
                amount = danceClass.price,
                paymentDate = paymentDate,
                paymentMonth = paymentMonth,
                isLatePayment = isLate,
                notes = notes
            )
        }

        return paymentRepository.saveAll(payments)
    }

    fun getTotalRevenueForMonth(month: YearMonth): BigDecimal {
        return paymentRepository.getTotalRevenueForMonth(month) ?: BigDecimal.ZERO
    }

    fun getTotalRevenueForClassAndMonth(classId: Long, month: YearMonth): BigDecimal {
        return paymentRepository.getTotalRevenueForClassAndMonth(classId, month) ?: BigDecimal.ZERO
    }

    fun getLatePaymentsForMonth(month: YearMonth): List<Payment> {
        return paymentRepository.findLatePaymentsForMonth(month)
    }

    fun calculateOutstandingPayments(month: YearMonth): Map<Student, List<OutstandingPayment>> {
        val allStudents = studentRepository.findAll()
        val outstandingMap = mutableMapOf<Student, List<OutstandingPayment>>()

        for (student in allStudents) {
            val outstandingPayments = mutableListOf<OutstandingPayment>()
            
            for (danceClass in student.classes) {
                val existingPayment = paymentRepository.findByStudentAndClassAndMonth(
                    student.id, danceClass.id, month
                )
                
                if (existingPayment == null) {
                    // Calculate expected amount (with late fee if applicable)
                    val currentDate = LocalDate.now()
                    val isLate = currentDate.dayOfMonth > 10 && YearMonth.from(currentDate) == month
                    val expectedAmount = if (isLate) {
                        danceClass.price.multiply(BigDecimal("1.15"))
                    } else {
                        danceClass.price
                    }
                    
                    outstandingPayments.add(
                        OutstandingPayment(
                            danceClass = danceClass,
                            expectedAmount = expectedAmount,
                            isLate = isLate
                        )
                    )
                }
            }
            
            if (outstandingPayments.isNotEmpty()) {
                outstandingMap[student] = outstandingPayments
            }
        }

        return outstandingMap
    }

    fun deletePayment(paymentId: Long) {
        if (!paymentRepository.existsById(paymentId)) {
            throw IllegalArgumentException("Payment not found with id: $paymentId")
        }
        paymentRepository.deleteById(paymentId)
    }
}

data class OutstandingPayment(
    val danceClass: DanceClass,
    val expectedAmount: BigDecimal,
    val isLate: Boolean
)
