package com.nova.crm.service

import com.nova.crm.entity.DanceClass
import com.nova.crm.entity.Student
import com.nova.crm.entity.StudentEnrollment
import com.nova.crm.repository.DanceClassRepository
import com.nova.crm.repository.PaymentRepository
import com.nova.crm.repository.StudentRepository
import com.nova.crm.service.StudentEnrollmentService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

@Service
@Transactional
class StudentService(
    private val studentRepository: StudentRepository,
    private val danceClassRepository: DanceClassRepository,
    private val paymentRepository: PaymentRepository,
    private val studentEnrollmentService: StudentEnrollmentService
) {

    fun findAll(): List<Student> = studentRepository.findAll()

    fun findById(id: Long): Student? = studentRepository.findByIdOrNull(id)

    fun findByName(firstName: String, lastName: String): List<Student> {
        return studentRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
            firstName, lastName
        )
    }

    fun findByPhone(phone: String): List<Student> {
        return studentRepository.findByPhoneContaining(phone)
    }

    fun save(student: Student): Student = studentRepository.save(student)

    fun deleteById(id: Long) {
        val student = findById(id) ?: throw IllegalArgumentException("Student not found with id: $id")
        
        try {
            // 1. Deactivate all enrollments in StudentEnrollment table
            val enrollments = studentEnrollmentService.getStudentEnrollments(id)
            enrollments.forEach { enrollment ->
                if (enrollment.id > 0) { // Only deactivate real enrollments, not temporary ones
                    studentEnrollmentService.unenrollStudentFromClass(id, enrollment.danceClass.id)
                }
            }
            
            // 2. Delete all payments for this student
            paymentRepository.deleteByStudentId(id)
            
            // 3. Finally delete the student
            studentRepository.deleteById(id)
            
        } catch (e: Exception) {
            throw IllegalStateException("Cannot delete student with id: $id. Error: ${e.message}", e)
        }
    }

    fun findStudentsWithoutPaymentForMonth(month: YearMonth): List<Student> {
        return studentRepository.findStudentsWithoutPaymentForMonth(month)
    }

    fun findStudentsWithoutPaymentForClassAndMonth(classId: Long, month: YearMonth): List<Student> {
        return studentRepository.findStudentsWithoutPaymentForClassAndMonth(classId, month)
    }

    // NEW METHODS: Enhanced enrollment functionality with dates
    // These methods provide additional functionality while maintaining backward compatibility

    /**
     * Enroll student with enrollment date tracking (enhanced version)
     */
    fun enrollInClassWithDate(
        studentId: Long, 
        classId: Long, 
        enrollmentDate: LocalDate = LocalDate.now(),
        notes: String? = null
    ): StudentEnrollment {
        return studentEnrollmentService.enrollStudentInClass(studentId, classId, enrollmentDate, notes)
    }

    /**
     * Get student enrollments with date information
     */
    fun getStudentEnrollments(studentId: Long): List<StudentEnrollment> {
        return studentEnrollmentService.getStudentEnrollments(studentId)
    }

    /**
     * Get enrollment details for a specific student-class combination
     */
    fun getEnrollmentDetails(studentId: Long, classId: Long): StudentEnrollment? {
        return studentEnrollmentService.getEnrollmentDetails(studentId, classId)
    }

    /**
     * Check if student is enrolled in a class (enhanced version)
     */
    fun isStudentEnrolledInClass(studentId: Long, classId: Long): Boolean {
        return studentEnrollmentService.isStudentEnrolled(studentId, classId)
    }

    /**
     * Get count of active classes for a student
     */
    fun getActiveClassCount(studentId: Long): Long {
        return studentEnrollmentService.getActiveClassCount(studentId)
    }

    /**
     * Get enrollments within a date range
     */
    fun getEnrollmentsByDateRange(startDate: LocalDate, endDate: LocalDate): List<StudentEnrollment> {
        return studentEnrollmentService.getEnrollmentsByDateRange(startDate, endDate)
    }
}
