package com.nova.crm.service

import com.nova.crm.entity.DanceClass
import com.nova.crm.entity.Student
import com.nova.crm.repository.DanceClassRepository
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

class StudentServiceTest {

    private lateinit var studentRepository: StudentRepository
    private lateinit var danceClassRepository: DanceClassRepository
    private lateinit var studentService: StudentService

    private lateinit var student: Student
    private lateinit var danceClass: DanceClass

    @BeforeEach
    fun setUp() {
        studentRepository = mockk()
        danceClassRepository = mockk()
        studentService = StudentService(studentRepository, danceClassRepository)

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
    fun `should enroll student in class successfully`() {
        // Given
        every { studentRepository.findByIdOrNull(1L) } returns student
        every { danceClassRepository.findByIdOrNull(1L) } returns danceClass
        every { studentRepository.save(any<Student>()) } answers { firstArg() }

        // When
        val result = studentService.enrollInClass(1L, 1L)

        // Then
        assertNotNull(result)
        assertTrue(result.classes.contains(danceClass))
        assertTrue(danceClass.students.contains(student))
        
        verify { studentRepository.save(any<Student>()) }
    }

    @Test
    fun `should throw exception when enrolling student already in class`() {
        // Given
        danceClass.addStudent(student) // Student already enrolled
        
        every { studentRepository.findByIdOrNull(1L) } returns student
        every { danceClassRepository.findByIdOrNull(1L) } returns danceClass

        // When & Then
        assertThrows<IllegalStateException> {
            studentService.enrollInClass(1L, 1L)
        }
    }

    @Test
    fun `should unenroll student from class successfully`() {
        // Given
        danceClass.addStudent(student) // Student is enrolled
        
        every { studentRepository.findByIdOrNull(1L) } returns student
        every { danceClassRepository.findByIdOrNull(1L) } returns danceClass
        every { studentRepository.save(any<Student>()) } answers { firstArg() }

        // When
        val result = studentService.unenrollFromClass(1L, 1L)

        // Then
        assertNotNull(result)
        assertFalse(result.classes.contains(danceClass))
        assertFalse(danceClass.students.contains(student))
        
        verify { studentRepository.save(any<Student>()) }
    }

    @Test
    fun `should throw exception when unenrolling student not in class`() {
        // Given
        every { studentRepository.findByIdOrNull(1L) } returns student
        every { danceClassRepository.findByIdOrNull(1L) } returns danceClass

        // When & Then
        assertThrows<IllegalStateException> {
            studentService.unenrollFromClass(1L, 1L)
        }
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

    @Test
    fun `should delete student and remove from classes`() {
        // Given
        danceClass.addStudent(student)
        
        every { studentRepository.findByIdOrNull(1L) } returns student
        every { studentRepository.deleteById(1L) } returns Unit

        // When
        studentService.deleteById(1L)

        // Then
        assertFalse(danceClass.students.contains(student))
        verify { studentRepository.deleteById(1L) }
    }
}
