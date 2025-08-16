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
     * Enroll a student in a class with enrollment date tracking
     */
    fun enrollStudentInClass(
        studentId: Long, 
        classId: Long, 
        enrollmentDate: LocalDate? = null,
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

        // Use current date if enrollmentDate is not provided
        val effectiveEnrollmentDate = enrollmentDate ?: LocalDate.now()

        // Create new enrollment
        val enrollment = StudentEnrollment(
            student = student,
            danceClass = danceClass,
            enrollmentDate = effectiveEnrollmentDate,
            notes = notes,
            isActive = true
        )

        val savedEnrollment = studentEnrollmentRepository.save(enrollment)

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
        val enrollment = studentEnrollmentRepository.findActiveEnrollment(studentId, classId)
        return enrollment != null
    }

    /**
     * Unenroll a student from all classes by deactivating all their enrollments
     */
    fun unenrollStudentFromAllClasses(studentId: Long) {
        val activeEnrollments = studentEnrollmentRepository.findByStudentIdAndIsActive(studentId, true)
        activeEnrollments.forEach { enrollment ->
            val deactivatedEnrollment = enrollment.copy(isActive = false)
            studentEnrollmentRepository.save(deactivatedEnrollment)
        }
    }
}
