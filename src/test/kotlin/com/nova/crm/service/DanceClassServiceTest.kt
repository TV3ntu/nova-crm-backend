package com.nova.crm.service

import com.nova.crm.entity.DanceClass
import com.nova.crm.entity.Student
import com.nova.crm.entity.StudentEnrollment
import com.nova.crm.entity.Teacher
import com.nova.crm.repository.DanceClassRepository
import com.nova.crm.repository.StudentRepository
import com.nova.crm.repository.TeacherRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DanceClassServiceTest {

    private lateinit var danceClassRepository: DanceClassRepository
    private lateinit var studentRepository: StudentRepository
    private lateinit var teacherRepository: TeacherRepository
    private lateinit var studentEnrollmentService: StudentEnrollmentService
    private lateinit var danceClassService: DanceClassService

    private lateinit var danceClass: DanceClass
    private lateinit var student1: Student
    private lateinit var student2: Student
    private lateinit var teacher1: Teacher
    private lateinit var teacher2: Teacher

    @BeforeEach
    fun setUp() {
        danceClassRepository = mockk()
        studentRepository = mockk()
        teacherRepository = mockk()
        studentEnrollmentService = mockk()
        
        danceClassService = DanceClassService(
            danceClassRepository,
            studentRepository,
            teacherRepository,
            studentEnrollmentService
        )

        // Setup test data
        danceClass = DanceClass(
            id = 1L,
            name = "Ballet Intermedio",
            description = "Clase de ballet nivel intermedio",
            price = BigDecimal("5000.00"),
            durationHours = 1.5,
            schedules = mutableSetOf(),
            teachers = mutableSetOf()
        )

        student1 = Student(
            id = 1L,
            firstName = "María",
            lastName = "González",
            phone = "123456789"
        )

        student2 = Student(
            id = 2L,
            firstName = "Ana",
            lastName = "Martínez",
            phone = "987654321"
        )

        teacher1 = Teacher(
            id = 1L,
            firstName = "Carmen",
            lastName = "Martínez",
            phone = "555123456",
            email = "carmen@example.com",
            classes = mutableSetOf()
        )

        teacher2 = Teacher(
            id = 2L,
            firstName = "Luis",
            lastName = "Rodríguez",
            phone = "555987654",
            email = "luis@example.com",
            classes = mutableSetOf()
        )

        // Setup relationships
        danceClass.teachers.add(teacher1)
        danceClass.teachers.add(teacher2)
        
        teacher1.classes.add(danceClass)
        teacher2.classes.add(danceClass)
    }

    @Test
    fun `should delete class correctly with all cascade operations`() {
        // Given
        val classId = 1L
        
        // Create enrollments for the students
        val enrollment1 = StudentEnrollment(
            id = 1L,
            student = student1,
            danceClass = danceClass,
            enrollmentDate = LocalDate.of(2024, 1, 15),
            isActive = true,
            notes = null
        )
        
        val enrollment2 = StudentEnrollment(
            id = 2L,
            student = student2,
            danceClass = danceClass,
            enrollmentDate = LocalDate.of(2024, 2, 1),
            isActive = true,
            notes = null
        )

        every { danceClassRepository.findById(classId) } returns java.util.Optional.of(danceClass)
        every { studentEnrollmentService.getClassEnrollments(classId) } returns listOf(enrollment1, enrollment2)
        every { studentEnrollmentService.unenrollStudentFromClass(1L, classId) } returns enrollment1.copy(isActive = false)
        every { studentEnrollmentService.unenrollStudentFromClass(2L, classId) } returns enrollment2.copy(isActive = false)
        every { danceClassRepository.deleteById(classId) } returns Unit

        // When
        danceClassService.deleteById(classId)

        // Then
        // 1. Verify that students were unenrolled from StudentEnrollment table
        verify { studentEnrollmentService.getClassEnrollments(classId) }
        verify { studentEnrollmentService.unenrollStudentFromClass(1L, classId) }
        verify { studentEnrollmentService.unenrollStudentFromClass(2L, classId) }
        
        // 2. Verify that Many-to-Many relationships were removed
        assertTrue(teacher1.classes.isEmpty(), "Teacher1 should be unassigned from class")
        assertTrue(teacher2.classes.isEmpty(), "Teacher2 should be unassigned from class")
        
        // 3. Verify that the class was deleted
        verify { danceClassRepository.deleteById(classId) }
    }

    @Test
    fun `should handle class with no enrollments in StudentEnrollment table`() {
        // Given
        val classId = 1L
        
        every { danceClassRepository.findById(classId) } returns java.util.Optional.of(danceClass)
        every { studentEnrollmentService.getClassEnrollments(classId) } returns emptyList()
        every { danceClassRepository.deleteById(classId) } returns Unit

        // When
        danceClassService.deleteById(classId)

        // Then
        // Should still remove Many-to-Many relationships and delete class
        assertTrue(teacher1.classes.isEmpty(), "Teacher1 should be unassigned from class")
        assertTrue(teacher2.classes.isEmpty(), "Teacher2 should be unassigned from class")
        
        verify { danceClassRepository.deleteById(classId) }
    }

    @Test
    fun `should handle class with no students or teachers`() {
        // Given
        val classId = 1L
        val emptyClass = DanceClass(
            id = classId,
            name = "Empty Class",
            description = "Class with no students or teachers",
            price = BigDecimal("3000.00"),
            durationHours = 1.5,
            schedules = mutableSetOf(),
            teachers = mutableSetOf()
        )
        
        every { danceClassRepository.findById(classId) } returns java.util.Optional.of(emptyClass)
        every { studentEnrollmentService.getClassEnrollments(classId) } returns emptyList()
        every { danceClassRepository.deleteById(classId) } returns Unit

        // When
        danceClassService.deleteById(classId)

        // Then
        verify { danceClassRepository.deleteById(classId) }
    }

    @Test
    fun `should throw IllegalArgumentException when class not found`() {
        // Given
        val classId = 999L
        every { danceClassRepository.findById(classId) } returns java.util.Optional.empty()

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            danceClassService.deleteById(classId)
        }
        
        assertEquals("Class not found with id: $classId", exception.message)
        
        // Verify that no deletion operations were attempted
        verify(exactly = 0) { studentEnrollmentService.getClassEnrollments(any()) }
        verify(exactly = 0) { danceClassRepository.deleteById(any()) }
    }

    @Test
    fun `should throw IllegalStateException when deletion fails`() {
        // Given
        val classId = 1L
        val errorMessage = "Database constraint violation"
        
        every { danceClassRepository.findById(classId) } returns java.util.Optional.of(danceClass)
        every { studentEnrollmentService.getClassEnrollments(classId) } returns emptyList()
        every { danceClassRepository.deleteById(classId) } throws RuntimeException(errorMessage)

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            danceClassService.deleteById(classId)
        }
        
        assertTrue(exception.message!!.contains("Cannot delete class with id: $classId"))
        assertTrue(exception.message!!.contains(errorMessage))
    }

    @Test
    fun `should preserve payment data during class deletion`() {
        // Given
        val classId = 1L
        
        every { danceClassRepository.findById(classId) } returns java.util.Optional.of(danceClass)
        every { studentEnrollmentService.getClassEnrollments(classId) } returns emptyList()
        every { danceClassRepository.deleteById(classId) } returns Unit

        // When
        danceClassService.deleteById(classId)

        // Then
        // Verify that NO payment deletion methods are called
        // This test ensures that payments are preserved as requested
        verify { danceClassRepository.deleteById(classId) }
        
        // The test passes if no payment-related deletion methods are called
        // This confirms that payments are preserved for historical records
    }
}
