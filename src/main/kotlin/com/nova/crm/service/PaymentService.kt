package com.nova.crm.service

import com.nova.crm.entity.DanceClass
import com.nova.crm.entity.Payment
import com.nova.crm.entity.PaymentMethod
import com.nova.crm.entity.Student
import com.nova.crm.entity.StudentEnrollment
import com.nova.crm.exception.DanceClassNotFoundException
import com.nova.crm.exception.DuplicatePaymentException
import com.nova.crm.exception.InsufficientAmountException
import com.nova.crm.exception.PaymentNotFoundException
import com.nova.crm.exception.StudentNotEnrolledException
import com.nova.crm.exception.StudentNotEnrolledInAnyClassException
import com.nova.crm.exception.StudentNotFoundException
import com.nova.crm.repository.DanceClassRepository
import com.nova.crm.repository.PaymentRepository
import com.nova.crm.repository.StudentEnrollmentRepository
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
    private val danceClassRepository: DanceClassRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository
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
        paymentMethod: PaymentMethod,
        notes: String? = null
    ): Payment {
        val student = studentRepository.findByIdOrNull(studentId)
            ?: throw StudentNotFoundException(studentId)
        
        val danceClass = danceClassRepository.findByIdOrNull(classId)
            ?: throw DanceClassNotFoundException(classId)

        // Verify student is enrolled in the class
        if (!student.classes.contains(danceClass)) {
            throw StudentNotEnrolledException(student.fullName, danceClass.name)
        }

        // Check if payment already exists for this student, class, and month
        val existingPayment = paymentRepository.findByStudentAndClassAndMonth(studentId, classId, paymentMonth)
        if (existingPayment != null) {
            throw DuplicatePaymentException(
                student.fullName, 
                danceClass.name, 
                paymentMonth.toString(), 
                existingPayment.id
            )
        }

        // Determine if payment is late
        val isLate = paymentDate.dayOfMonth > 10 && YearMonth.from(paymentDate) == paymentMonth

        val payment = Payment(
            student = student,
            danceClass = danceClass,
            amount = amount,
            paymentDate = paymentDate,
            paymentMonth = paymentMonth,
            paymentMethod = paymentMethod,
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
        paymentMethod: PaymentMethod,
        notes: String? = null
    ): List<Payment> {
        val student = studentRepository.findByIdOrNull(studentId)
            ?: throw StudentNotFoundException(studentId)

        if (student.classes.isEmpty()) {
            throw StudentNotEnrolledInAnyClassException(student.fullName)
        }

        // Get classes that don't have payments for this month
        val unpaidClasses = student.classes.filter { danceClass ->
            paymentRepository.findByStudentAndClassAndMonth(studentId, danceClass.id, paymentMonth) == null
        }

        if (unpaidClasses.isEmpty()) {
            throw IllegalStateException("Todas las clases del estudiante ${student.fullName} ya tienen pagos registrados para $paymentMonth")
        }

        // Calculate total expected amount
        val totalExpectedAmount = unpaidClasses.sumOf { it.price }
        
        // Validate that the payment amount covers all classes
        if (totalAmount < totalExpectedAmount) {
            throw InsufficientAmountException(
                totalExpectedAmount.toString(), 
                totalAmount.toString()
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
                paymentMethod = paymentMethod,
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
            
            // Get active enrollments for this student
            val activeEnrollments = studentEnrollmentRepository.findByStudentIdAndIsActive(student.id, true)
            
            for (enrollment in activeEnrollments) {
                val danceClass = enrollment.danceClass
                
                // Check if the requested month is after or equal to the enrollment month
                val enrollmentMonth = YearMonth.from(enrollment.enrollmentDate)
                if (month.isBefore(enrollmentMonth)) {
                    // Skip this class for this month since student wasn't enrolled yet
                    continue
                }
                
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

    fun updatePayment(
        paymentId: Long,
        amount: BigDecimal? = null,
        paymentDate: LocalDate? = null,
        paymentMethod: PaymentMethod? = null,
        notes: String? = null
    ): Payment {
        val existingPayment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw PaymentNotFoundException(paymentId)

        // Create a new Payment instance with updated values
        val updatedPayment = existingPayment.copy(
            amount = amount ?: existingPayment.amount,
            paymentDate = paymentDate ?: existingPayment.paymentDate,
            paymentMethod = paymentMethod ?: existingPayment.paymentMethod,
            notes = notes ?: existingPayment.notes,
            // Recalculate if payment is late based on new payment date
            isLatePayment = if (paymentDate != null) {
                paymentDate.dayOfMonth > 10 && YearMonth.from(paymentDate) == existingPayment.paymentMonth
            } else {
                existingPayment.isLatePayment
            }
        )

        return paymentRepository.save(updatedPayment)
    }

    fun deletePayment(paymentId: Long) {
        val payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw PaymentNotFoundException(paymentId)
        
        paymentRepository.delete(payment)
    }
}

data class OutstandingPayment(
    val danceClass: DanceClass,
    val expectedAmount: BigDecimal,
    val isLate: Boolean
)
