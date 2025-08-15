package com.nova.crm.service

import com.nova.crm.entity.DanceClass
import com.nova.crm.entity.Payment
import com.nova.crm.entity.PaymentMethod
import com.nova.crm.entity.Student
import com.nova.crm.entity.StudentEnrollment
import com.nova.crm.repository.DanceClassRepository
import com.nova.crm.repository.PaymentRepository
import com.nova.crm.repository.StudentEnrollmentRepository
import com.nova.crm.repository.StudentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class PaymentServiceTest {

    private lateinit var paymentRepository: PaymentRepository
    private lateinit var studentRepository: StudentRepository
    private lateinit var danceClassRepository: DanceClassRepository
    private lateinit var studentEnrollmentRepository: StudentEnrollmentRepository
    private lateinit var paymentService: PaymentService

    private lateinit var student: Student
    private lateinit var danceClass: DanceClass

    @BeforeEach
    fun setUp() {
        paymentRepository = mockk()
        studentRepository = mockk()
        danceClassRepository = mockk()
        studentEnrollmentRepository = mockk()
        paymentService = PaymentService(paymentRepository, studentRepository, danceClassRepository, studentEnrollmentRepository)

        student = Student(
            id = 1L,
            firstName = "Ana",
            lastName = "García",
            phone = "123456789"
        )

        danceClass = DanceClass(
            id = 1L,
            name = "Danza Clásica",
            price = BigDecimal("5000.00"),
            durationHours = 1.5
        )

        // Add student to class
        danceClass.addStudent(student)
    }

    @Test
    fun `should register payment successfully`() {
        // Given
        val paymentMonth = YearMonth.of(2024, 2)
        val amount = BigDecimal("5000.00")
        val paymentDate = LocalDate.of(2024, 2, 5)

        every { studentRepository.findByIdOrNull(1L) } returns student
        every { danceClassRepository.findByIdOrNull(1L) } returns danceClass
        every { paymentRepository.findByStudentAndClassAndMonth(1L, 1L, paymentMonth) } returns null
        every { paymentRepository.save(any<Payment>()) } answers { firstArg() }

        // When
        val result = paymentService.registerPayment(
            studentId = 1L,
            classId = 1L,
            amount = amount,
            paymentMonth = paymentMonth,
            paymentDate = paymentDate,
            paymentMethod = PaymentMethod.EFECTIVO
        )

        // Then
        assertNotNull(result)
        assertEquals(student, result.student)
        assertEquals(danceClass, result.danceClass)
        assertEquals(amount, result.amount)
        assertEquals(paymentMonth, result.paymentMonth)
        assertEquals(paymentDate, result.paymentDate)
        assertFalse(result.isLatePayment) // Payment on 5th is not late

        verify { paymentRepository.save(any<Payment>()) }
    }

    @Test
    fun `should mark payment as late when paid after 10th`() {
        // Given
        val paymentMonth = YearMonth.of(2024, 2)
        val amount = BigDecimal("5000.00")
        val paymentDate = LocalDate.of(2024, 2, 15) // After 10th

        every { studentRepository.findByIdOrNull(1L) } returns student
        every { danceClassRepository.findByIdOrNull(1L) } returns danceClass
        every { paymentRepository.findByStudentAndClassAndMonth(1L, 1L, paymentMonth) } returns null
        every { paymentRepository.save(any<Payment>()) } answers { firstArg() }

        // When
        val result = paymentService.registerPayment(
            studentId = 1L,
            classId = 1L,
            amount = amount,
            paymentMonth = paymentMonth,
            paymentDate = paymentDate,
            paymentMethod = PaymentMethod.EFECTIVO
        )

        // Then
        assertTrue(result.isLatePayment)
    }

    @Test
    fun `should throw exception when student not found`() {
        // Given
        every { studentRepository.findByIdOrNull(1L) } returns null

        // When & Then
        assertThrows<IllegalArgumentException> {
            paymentService.registerPayment(
                studentId = 1L,
                classId = 1L,
                amount = BigDecimal("5000.00"),
                paymentMonth = YearMonth.of(2024, 2),
                paymentMethod = PaymentMethod.EFECTIVO
            )
        }
    }

    @Test
    fun `should throw exception when student not enrolled in class`() {
        // Given
        val unenrolledStudent = Student(
            id = 2L,
            firstName = "Carlos",
            lastName = "López",
            phone = "987654321"
        )

        every { studentRepository.findByIdOrNull(2L) } returns unenrolledStudent
        every { danceClassRepository.findByIdOrNull(1L) } returns danceClass

        // When & Then
        assertThrows<IllegalStateException> {
            paymentService.registerPayment(
                studentId = 2L,
                classId = 1L,
                amount = BigDecimal("5000.00"),
                paymentMonth = YearMonth.of(2024, 2),
                paymentMethod = PaymentMethod.EFECTIVO
            )
        }
    }

    @Test
    fun `should register multi-class payment successfully`() {
        // Given
        val danceClass2 = DanceClass(
            id = 2L,
            name = "Danza Jazz",
            price = BigDecimal("3000.00"),
            durationHours = 1.0
        )
        danceClass2.addStudent(student)

        val paymentMonth = YearMonth.of(2024, 2)
        val totalAmount = BigDecimal("8000.00") // 5000 + 3000

        every { studentRepository.findByIdOrNull(1L) } returns student
        every { paymentRepository.findByStudentAndClassAndMonth(1L, 1L, paymentMonth) } returns null
        every { paymentRepository.findByStudentAndClassAndMonth(1L, 2L, paymentMonth) } returns null
        every { paymentRepository.saveAll(any<List<Payment>>()) } answers { firstArg() }

        // When
        val result = paymentService.registerMultiClassPayment(
            studentId = 1L,
            totalAmount = totalAmount,
            paymentMonth = paymentMonth,
            paymentMethod = PaymentMethod.EFECTIVO
        )

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.danceClass.id == 1L && it.amount == BigDecimal("5000.00") })
        assertTrue(result.any { it.danceClass.id == 2L && it.amount == BigDecimal("3000.00") })

        verify { paymentRepository.saveAll(any<List<Payment>>()) }
    }

    @Test
    fun `should throw exception when multi-class payment amount is insufficient`() {
        // Given
        val paymentMonth = YearMonth.of(2024, 2)
        val insufficientAmount = BigDecimal("3000.00") // Less than class price of 5000

        every { studentRepository.findByIdOrNull(1L) } returns student
        every { paymentRepository.findByStudentAndClassAndMonth(1L, 1L, paymentMonth) } returns null

        // When & Then
        assertThrows<IllegalArgumentException> {
            paymentService.registerMultiClassPayment(
                studentId = 1L,
                totalAmount = insufficientAmount,
                paymentMonth = paymentMonth,
                paymentMethod = PaymentMethod.EFECTIVO
            )
        }
    }

    @Test
    fun `should calculate outstanding payments correctly`() {
        // Given
        val month = YearMonth.of(2024, 2)
        val students = listOf(student)
        
        // Create enrollment for the student (enrolled before the requested month)
        val enrollment = StudentEnrollment(
            id = 1L,
            student = student,
            danceClass = danceClass,
            enrollmentDate = LocalDate.of(2024, 1, 15), // Enrolled in January
            isActive = true,
            notes = null
        )

        every { studentRepository.findAll() } returns students
        every { studentEnrollmentRepository.findByStudentIdAndIsActive(1L, true) } returns listOf(enrollment)
        every { paymentRepository.findByStudentAndClassAndMonth(1L, 1L, month) } returns null

        // When
        val result = paymentService.calculateOutstandingPayments(month)

        // Then
        assertEquals(1, result.size)
        assertTrue(result.containsKey(student))
        val outstandingPayments = result[student]!!
        assertEquals(1, outstandingPayments.size)
        assertEquals(danceClass, outstandingPayments[0].danceClass)
        assertEquals(BigDecimal("5000.00"), outstandingPayments[0].expectedAmount)
    }

    @Test
    fun `should not show outstanding payments for months before enrollment date`() {
        // Given
        val student = Student(
            id = 1L,
            firstName = "María",
            lastName = "González",
            phone = "123456789"
        )
        
        val danceClass = DanceClass(
            id = 1L,
            name = "Ballet Intermedio",
            price = BigDecimal("5000.00"),
            durationHours = 1.5
        )
        
        // Student enrolled on March 15, 2024
        val enrollmentDate = LocalDate.of(2024, 3, 15)
        val enrollment = StudentEnrollment(
            id = 1L,
            student = student,
            danceClass = danceClass,
            enrollmentDate = enrollmentDate,
            isActive = true
        )
        
        // Test months: February (before enrollment) and April (after enrollment)
        val februaryMonth = YearMonth.of(2024, 2) // Before enrollment
        val aprilMonth = YearMonth.of(2024, 4)    // After enrollment
        
        // Mock repository calls
        every { studentRepository.findAll() } returns listOf(student)
        every { studentEnrollmentRepository.findByStudentIdAndIsActive(1L, true) } returns listOf(enrollment)
        
        // No payments exist for either month
        every { paymentRepository.findByStudentAndClassAndMonth(1L, 1L, februaryMonth) } returns null
        every { paymentRepository.findByStudentAndClassAndMonth(1L, 1L, aprilMonth) } returns null
        
        // When - Check February (before enrollment)
        val februaryOutstanding = paymentService.calculateOutstandingPayments(februaryMonth)
        
        // Then - Should NOT have outstanding payments for February (before enrollment)
        assertTrue(februaryOutstanding.isEmpty(), "Should not have outstanding payments for months before enrollment")
        
        // When - Check April (after enrollment)
        val aprilOutstanding = paymentService.calculateOutstandingPayments(aprilMonth)
        
        // Then - Should HAVE outstanding payments for April (after enrollment)
        assertFalse(aprilOutstanding.isEmpty(), "Should have outstanding payments for months after enrollment")
        assertEquals(1, aprilOutstanding.size, "Should have one student with outstanding payments")
        
        val studentOutstanding = aprilOutstanding[student]
        assertNotNull(studentOutstanding, "Student should have outstanding payments for April")
        assertEquals(1, studentOutstanding!!.size, "Student should have one outstanding payment")
        assertEquals(danceClass.name, studentOutstanding[0].danceClass.name, "Outstanding payment should be for the correct class")
        assertEquals(danceClass.price, studentOutstanding[0].expectedAmount, "Expected amount should match class price")
    }
    
    @Test
    fun `should show outstanding payments for enrollment month and after`() {
        // Given
        val student = Student(
            id = 2L,
            firstName = "Ana",
            lastName = "Martínez", 
            phone = "987654321"
        )
        
        val danceClass = DanceClass(
            id = 2L,
            name = "Jazz Avanzado",
            price = BigDecimal("6000.00"),
            durationHours = 2.0
        )
        
        // Student enrolled on March 1, 2024 (beginning of month)
        val enrollmentDate = LocalDate.of(2024, 3, 1)
        val enrollment = StudentEnrollment(
            id = 2L,
            student = student,
            danceClass = danceClass,
            enrollmentDate = enrollmentDate,
            isActive = true
        )
        
        val marchMonth = YearMonth.of(2024, 3) // Same month as enrollment
        
        // Mock repository calls
        every { studentRepository.findAll() } returns listOf(student)
        every { studentEnrollmentRepository.findByStudentIdAndIsActive(2L, true) } returns listOf(enrollment)
        every { paymentRepository.findByStudentAndClassAndMonth(2L, 2L, marchMonth) } returns null
        
        // When
        val outstandingPayments = paymentService.calculateOutstandingPayments(marchMonth)
        
        // Then
        assertFalse(outstandingPayments.isEmpty(), "Should have outstanding payments for enrollment month")
        assertEquals(1, outstandingPayments.size, "Should have one student with outstanding payments")
        
        val studentOutstanding = outstandingPayments[student]
        assertNotNull(studentOutstanding, "Student should have outstanding payments")
        assertEquals(1, studentOutstanding!!.size, "Student should have one outstanding payment")
        assertEquals(danceClass.name, studentOutstanding[0].danceClass.name)
        assertEquals(danceClass.price, studentOutstanding[0].expectedAmount)
    }
    
    @Test
    fun `should handle multiple enrollments with different dates correctly`() {
        // Given
        val student = Student(
            id = 3L,
            firstName = "Carlos",
            lastName = "López",
            phone = "555666777"
        )
        
        val balletClass = DanceClass(
            id = 3L,
            name = "Ballet Básico",
            price = BigDecimal("4000.00"),
            durationHours = 1.0
        )
        
        val jazzClass = DanceClass(
            id = 4L,
            name = "Jazz Intermedio", 
            price = BigDecimal("5500.00"),
            durationHours = 1.5
        )
        
        // Student enrolled in Ballet on February 1, Jazz on April 1
        val balletEnrollment = StudentEnrollment(
            id = 3L,
            student = student,
            danceClass = balletClass,
            enrollmentDate = LocalDate.of(2024, 2, 1),
            isActive = true
        )
        
        val jazzEnrollment = StudentEnrollment(
            id = 4L,
            student = student,
            danceClass = jazzClass,
            enrollmentDate = LocalDate.of(2024, 4, 1),
            isActive = true
        )
        
        val marchMonth = YearMonth.of(2024, 3) // After Ballet, before Jazz
        
        // Mock repository calls
        every { studentRepository.findAll() } returns listOf(student)
        every { studentEnrollmentRepository.findByStudentIdAndIsActive(3L, true) } returns listOf(balletEnrollment, jazzEnrollment)
        every { paymentRepository.findByStudentAndClassAndMonth(3L, 3L, marchMonth) } returns null // No Ballet payment
        every { paymentRepository.findByStudentAndClassAndMonth(3L, 4L, marchMonth) } returns null // No Jazz payment
        
        // When
        val outstandingPayments = paymentService.calculateOutstandingPayments(marchMonth)
        
        // Then
        assertFalse(outstandingPayments.isEmpty(), "Should have outstanding payments")
        assertEquals(1, outstandingPayments.size, "Should have one student with outstanding payments")
        
        val studentOutstanding = outstandingPayments[student]
        assertNotNull(studentOutstanding, "Student should have outstanding payments")
        assertEquals(1, studentOutstanding!!.size, "Should only have Ballet outstanding (Jazz not enrolled yet)")
        assertEquals(balletClass.name, studentOutstanding[0].danceClass.name, "Should be Ballet class only")
        assertEquals(balletClass.price, studentOutstanding[0].expectedAmount)
    }
}
