package com.nova.crm.service

import com.nova.crm.entity.DanceClass
import com.nova.crm.entity.Payment
import com.nova.crm.entity.Student
import com.nova.crm.entity.StudentEnrollment
import com.nova.crm.repository.DanceClassRepository
import com.nova.crm.repository.PaymentRepository
import com.nova.crm.repository.StudentRepository
import com.nova.crm.service.StudentEnrollmentService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.LocalDate

class StudentServiceTest {

    private lateinit var studentRepository: StudentRepository
    private lateinit var danceClassRepository: DanceClassRepository
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var studentEnrollmentService: StudentEnrollmentService
    private lateinit var studentService: StudentService

    private lateinit var student: Student
    private lateinit var danceClass: DanceClass

    @BeforeEach
    fun setUp() {
        studentRepository = mockk()
        danceClassRepository = mockk()
        paymentRepository = mockk()
        studentEnrollmentService = mockk()
        studentService = StudentService(studentRepository, danceClassRepository, paymentRepository, studentEnrollmentService)

        student = Student(
            id = 1L,
            firstName = "Ana",
            lastName = "García",
            phone = "123456789",
            email = "ana@example.com",
            birthDate = LocalDate.of(2000, 5, 15)
        )

        danceClass = DanceClass(
            id = 1L,
            name = "Danza Clásica",
            price = BigDecimal("5000.00"),
            durationHours = 1.5
        )
    }

    @Test
    fun `should find student by id`() {
        // Given
        every { studentRepository.findByIdOrNull(1L) } returns student

        // When
        val result = studentService.findById(1L)

        // Then
        assertNotNull(result)
        assertEquals(student.id, result?.id)
        assertEquals(student.firstName, result?.firstName)
    }

    @Test
    fun `should return null when student not found`() {
        // Given
        every { studentRepository.findByIdOrNull(999L) } returns null

        // When
        val result = studentService.findById(999L)

        // Then
        assertNull(result)
    }

    @Test
    fun `should delete student successfully`() {
        // Given
        val enrollment1 = StudentEnrollment(
            id = 1L,
            student = student,
            danceClass = danceClass,
            enrollmentDate = LocalDate.of(2024, 1, 15),
            isActive = true
        )
        
        every { studentRepository.findByIdOrNull(1L) } returns student
        every { studentEnrollmentService.getStudentEnrollments(1L) } returns listOf(enrollment1)
        every { studentEnrollmentService.unenrollStudentFromClass(1L, 1L) } returns enrollment1.copy(isActive = false)
        every { paymentRepository.deleteByStudentId(1L) } returns Unit
        every { studentRepository.deleteById(1L) } returns Unit

        // When & Then
        assertDoesNotThrow {
            studentService.deleteById(1L)
        }

        verify { studentEnrollmentService.getStudentEnrollments(1L) }
        verify { studentEnrollmentService.unenrollStudentFromClass(1L, 1L) }
        verify { paymentRepository.deleteByStudentId(1L) }
        verify { studentRepository.deleteById(1L) }
    }

    @Test
    fun `should find students by name`() {
        // Given
        val students = listOf(student)
        every { studentRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase("Ana", "García") } returns students

        // When
        val result = studentService.findByName("Ana", "García")

        // Then
        assertEquals(1, result.size)
        assertEquals(student, result[0])
    }

    @Test
    fun `should save student successfully`() {
        // Given
        every { studentRepository.save(student) } returns student

        // When
        val result = studentService.save(student)

        // Then
        assertEquals(student, result)
        verify { studentRepository.save(student) }
    }
}
