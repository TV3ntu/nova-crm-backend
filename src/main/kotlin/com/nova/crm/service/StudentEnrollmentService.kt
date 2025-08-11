package com.nova.crm.service

import com.nova.crm.entity.DanceClass
import com.nova.crm.entity.Student
import com.nova.crm.entity.StudentEnrollment
import com.nova.crm.repository.DanceClassRepository
import com.nova.crm.repository.StudentEnrollmentRepository
import com.nova.crm.repository.StudentRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class StudentEnrollmentService(
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val studentRepository: StudentRepository,
    private val danceClassRepository: DanceClassRepository
) {

    /**
     * Enroll a student in a class with enrollment date tracking.
     * This method maintains compatibility with existing enrollment logic.
     */
    fun enrollStudentInClass(
        studentId: Long, 
        classId: Long, 
        enrollmentDate: LocalDate = LocalDate.now(),
        notes: String? = null
    ): StudentEnrollment {
        val student = studentRepository.findByIdOrNull(studentId)
            ?: throw IllegalArgumentException("Student not found with id: $studentId")
        
        val danceClass = danceClassRepository.findByIdOrNull(classId)
            ?: throw IllegalArgumentException("Class not found with id: $classId")

        // Check if student is already enrolled (active enrollment)
        val existingEnrollment = studentEnrollmentRepository.findActiveEnrollment(studentId, classId)
        if (existingEnrollment != null) {
            throw IllegalStateException("Student is already enrolled in this class")
        }

        // Create new enrollment
        val enrollment = StudentEnrollment(
            student = student,
            danceClass = danceClass,
            enrollmentDate = enrollmentDate,
            notes = notes,
            isActive = true
        )

        val savedEnrollment = studentEnrollmentRepository.save(enrollment)

        // Maintain existing Many-to-Many relationship for backward compatibility
        danceClass.addStudent(student)
        studentRepository.save(student)

        return savedEnrollment
    }

    /**
     * Unenroll a student from a class by deactivating the enrollment.
     * This maintains the historical record while removing the active relationship.
     */
    fun unenrollStudentFromClass(studentId: Long, classId: Long): StudentEnrollment {
        val student = studentRepository.findByIdOrNull(studentId)
            ?: throw IllegalArgumentException("Student not found with id: $studentId")
        
        val danceClass = danceClassRepository.findByIdOrNull(classId)
            ?: throw IllegalArgumentException("Class not found with id: $classId")

        val enrollment = studentEnrollmentRepository.findActiveEnrollment(studentId, classId)
            ?: throw IllegalStateException("Student is not enrolled in this class")

        // Deactivate enrollment instead of deleting (preserve history)
        val deactivatedEnrollment = enrollment.copy(isActive = false)
        val savedEnrollment = studentEnrollmentRepository.save(deactivatedEnrollment)

        // Remove from existing Many-to-Many relationship for backward compatibility
        danceClass.removeStudent(student)
        studentRepository.save(student)

        return savedEnrollment
    }

    /**
     * Get all active enrollments for a student
     */
    fun getStudentEnrollments(studentId: Long): List<StudentEnrollment> {
        return studentEnrollmentRepository.findByStudentIdAndIsActive(studentId, true)
    }

    /**
     * Get all active enrollments for a class
     */
    fun getClassEnrollments(classId: Long): List<StudentEnrollment> {
        return studentEnrollmentRepository.findByDanceClassIdAndIsActive(classId, true)
    }

    /**
     * Get enrollment details for a specific student-class combination
     */
    fun getEnrollmentDetails(studentId: Long, classId: Long): StudentEnrollment? {
        return studentEnrollmentRepository.findActiveEnrollment(studentId, classId)
    }

    /**
     * Get enrollments within a date range
     */
    fun getEnrollmentsByDateRange(startDate: LocalDate, endDate: LocalDate): List<StudentEnrollment> {
        return studentEnrollmentRepository.findEnrollmentsByDateRange(startDate, endDate)
    }

    /**
     * Get count of active students in a class
     */
    fun getActiveStudentCount(classId: Long): Long {
        return studentEnrollmentRepository.countActiveEnrollmentsByClass(classId)
    }

    /**
     * Get count of active classes for a student
     */
    fun getActiveClassCount(studentId: Long): Long {
        return studentEnrollmentRepository.countActiveEnrollmentsByStudent(studentId)
    }

    /**
     * Check if a student is enrolled in a class
     */
    fun isStudentEnrolled(studentId: Long, classId: Long): Boolean {
        return studentEnrollmentRepository.findActiveEnrollment(studentId, classId) != null
    }

    /**
     * Migrate existing enrollments from the Many-to-Many relationship to StudentEnrollment table.
     * This should be called once during deployment to preserve existing data.
     */
    fun migrateExistingEnrollments() {
        val students = studentRepository.findAll()
        
        students.forEach { student ->
            student.classes.forEach { danceClass ->
                // Check if enrollment already exists
                val existingEnrollment = studentEnrollmentRepository.findActiveEnrollment(student.id, danceClass.id)
                
                if (existingEnrollment == null) {
                    // Create enrollment with current date (we don't have historical data)
                    val enrollment = StudentEnrollment(
                        student = student,
                        danceClass = danceClass,
                        enrollmentDate = LocalDate.now(),
                        notes = "Migrated from existing enrollment",
                        isActive = true
                    )
                    studentEnrollmentRepository.save(enrollment)
                }
            }
        }
    }
}
