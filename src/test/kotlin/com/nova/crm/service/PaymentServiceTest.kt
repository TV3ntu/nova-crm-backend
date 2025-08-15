package com.nova.crm.service

import com.nova.crm.entity.DanceClass
import com.nova.crm.entity.Payment
import com.nova.crm.entity.PaymentMethod
import com.nova.crm.entity.Student
import com.nova.crm.repository.DanceClassRepository
import com.nova.crm.repository.PaymentRepository
import com.nova.crm.repository.StudentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private lateinit var paymentService: PaymentService

    private lateinit var student: Student
    private lateinit var danceClass: DanceClass

    @BeforeEach
    fun setUp() {
        paymentRepository = mockk()
        studentRepository = mockk()
        danceClassRepository = mockk()
        paymentService = PaymentService(paymentRepository, studentRepository, danceClassRepository)

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

        every { studentRepository.findAll() } returns students
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
}
