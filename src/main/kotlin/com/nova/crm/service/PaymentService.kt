package com.nova.crm.service

import com.nova.crm.entity.DanceClass
import com.nova.crm.entity.Payment
import com.nova.crm.entity.PaymentMethod
import com.nova.crm.entity.Student
import com.nova.crm.exception.DanceClassNotFoundException
import com.nova.crm.exception.DuplicatePaymentException
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
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val studentEnrollmentService: StudentEnrollmentService
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
        if (!studentEnrollmentService.isStudentEnrolled(studentId, classId)) {
            throw StudentNotEnrolledException(student.fullName, danceClass.name)
        }

        // Validate enrollment date and check for duplicate payments
        validatePaymentEligibility(studentId, classId, paymentMonth, student.fullName, danceClass.name)

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
        notes: String? = null,
        classIds: List<Long> = emptyList()
    ): List<Payment> {
        val student = studentRepository.findByIdOrNull(studentId)
            ?: throw StudentNotFoundException(studentId)

        val targetClasses = determineTargetClasses(studentId, student.fullName, paymentMonth, classIds)
        validateTargetClasses(targetClasses, student.fullName, paymentMonth, classIds)
        
        val payments = createProportionalPayments(
            student, targetClasses, totalAmount, paymentMonth, paymentDate, paymentMethod, notes
        )

        return paymentRepository.saveAll(payments)
    }

    /**
     * Determines which classes the student should pay for based on provided class IDs or active enrollments.
     */
    private fun determineTargetClasses(
        studentId: Long,
        studentName: String,
        paymentMonth: YearMonth,
        classIds: List<Long>
    ): List<DanceClass> {
        return if (classIds.isNotEmpty()) {
            getSpecifiedClasses(studentId, studentName, paymentMonth, classIds)
        } else {
            getActiveEnrollmentClasses(studentId, studentName, paymentMonth)
        }
    }

    /**
     * Gets and validates specified classes for payment.
     */
    private fun getSpecifiedClasses(
        studentId: Long,
        studentName: String,
        paymentMonth: YearMonth,
        classIds: List<Long>
    ): List<DanceClass> {
        val danceClasses = mutableListOf<DanceClass>()
        
        for (classId in classIds) {
            val danceClass = danceClassRepository.findByIdOrNull(classId)
                ?: throw DanceClassNotFoundException(classId)
            
            if (!studentEnrollmentService.isStudentEnrolled(studentId, classId)) {
                throw StudentNotEnrolledException(studentName, danceClass.name)
            }

            // Validate enrollment date and check for duplicate payments
            validatePaymentEligibility(studentId, classId, paymentMonth, studentName, danceClass.name)

            danceClasses.add(danceClass)
        }
        
        return danceClasses
    }

    /**
     * Gets classes from active enrollments that are eligible for payment.
     */
    private fun getActiveEnrollmentClasses(
        studentId: Long,
        studentName: String,
        paymentMonth: YearMonth
    ): List<DanceClass> {
        val activeEnrollments = studentEnrollmentService.getStudentEnrollments(studentId)
        
        if (activeEnrollments.isEmpty()) {
            throw StudentNotEnrolledInAnyClassException(studentName)
        }

        // Get classes that don't have payments for this month and validate enrollment dates
        return activeEnrollments
            .filter { enrollment ->
                val enrollmentMonth = YearMonth.from(enrollment.enrollmentDate)
                !paymentMonth.isBefore(enrollmentMonth) // Only include classes where student was enrolled
            }
            .map { it.danceClass }
            .filter { danceClass ->
                paymentRepository.findByStudentAndClassAndMonth(studentId, danceClass.id, paymentMonth) == null
            }
    }

    /**
     * Validates that there are classes available for payment.
     */
    private fun validateTargetClasses(
        targetClasses: List<DanceClass>,
        studentName: String,
        paymentMonth: YearMonth,
        classIds: List<Long>
    ) {
        if (targetClasses.isEmpty()) {
            val message = if (classIds.isNotEmpty()) {
                "Todas las clases especificadas ya tienen pagos registrados para $paymentMonth"
            } else {
                "Todas las clases del estudiante $studentName ya tienen pagos registrados para $paymentMonth"
            }
            throw IllegalStateException(message)
        }
    }

    /**
     * Creates proportional payments for each target class.
     */
    private fun createProportionalPayments(
        student: Student,
        targetClasses: List<DanceClass>,
        totalAmount: BigDecimal,
        paymentMonth: YearMonth,
        paymentDate: LocalDate,
        paymentMethod: PaymentMethod,
        notes: String?
    ): List<Payment> {
        val discountFactor = calculateDiscountFactor(targetClasses, totalAmount)
        val isLate = isPaymentLate(paymentDate, paymentMonth)

        return targetClasses.map { danceClass ->
            val proportionalAmount = danceClass.price.multiply(discountFactor)
            
            Payment(
                student = student,
                danceClass = danceClass,
                amount = proportionalAmount,
                paymentDate = paymentDate,
                paymentMonth = paymentMonth,
                paymentMethod = paymentMethod,
                isLatePayment = isLate,
                notes = notes
            )
        }
    }

    /**
     * Calculates the discount factor based on total expected amount vs actual payment amount.
     */
    private fun calculateDiscountFactor(targetClasses: List<DanceClass>, totalAmount: BigDecimal): BigDecimal {
        val totalExpectedAmount = targetClasses.sumOf { it.price }
        return if (totalExpectedAmount > BigDecimal.ZERO) {
            totalAmount.divide(totalExpectedAmount, 4, java.math.RoundingMode.HALF_UP)
        } else {
            BigDecimal.ONE
        }
    }

    /**
     * Determines if a payment is considered late based on payment date and month.
     */
    private fun isPaymentLate(paymentDate: LocalDate, paymentMonth: YearMonth): Boolean {
        return paymentDate.dayOfMonth > 10 && YearMonth.from(paymentDate) == paymentMonth
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

    /**
     * Validates that a student can make a payment for a specific class and month.
     * Checks enrollment date and duplicate payments.
     */
    private fun validatePaymentEligibility(
        studentId: Long,
        classId: Long,
        paymentMonth: YearMonth,
        studentName: String,
        className: String
    ) {
        // Get enrollment date and validate payment month
        val enrollment = studentEnrollmentRepository.findByStudentIdAndDanceClassIdAndIsActive(studentId, classId, true)
            ?: throw StudentNotEnrolledException(studentName, className)
        
        val enrollmentMonth = YearMonth.from(enrollment.enrollmentDate)
        if (paymentMonth.isBefore(enrollmentMonth)) {
            throw IllegalArgumentException(
                "No se puede registrar un pago para ${paymentMonth} porque el estudiante ${studentName} " +
                "se inscribió en ${className} el ${enrollment.enrollmentDate} (${enrollmentMonth})"
            )
        }

        // Check if payment already exists for this student, class, and month
        val existingPayment = paymentRepository.findByStudentAndClassAndMonth(studentId, classId, paymentMonth)
        if (existingPayment != null) {
            throw DuplicatePaymentException(
                studentName, 
                className, 
                paymentMonth.toString(), 
                existingPayment.id
            )
        }
    }
}

data class OutstandingPayment(
    val danceClass: DanceClass,
    val expectedAmount: BigDecimal,
    val isLate: Boolean
)
