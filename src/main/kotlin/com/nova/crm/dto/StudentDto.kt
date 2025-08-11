package com.nova.crm.dto

import com.nova.crm.entity.Student
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import java.time.LocalDate

@Schema(description = "Request to create a new student")
data class CreateStudentRequest(
    @field:NotBlank(message = "First name is required")
    @Schema(description = "Student's first name", example = "Sofía", required = true)
    val firstName: String,
    
    @field:NotBlank(message = "Last name is required")
    @Schema(description = "Student's last name", example = "Martínez", required = true)
    val lastName: String,
    
    @field:NotBlank(message = "Phone is required")
    @Schema(description = "Student's phone number", example = "1134567890", required = true)
    val phone: String,
    
    @field:Email(message = "Invalid email format")
    @Schema(description = "Student's email address", example = "sofia@email.com")
    val email: String? = null,
    
    @Schema(description = "Student's home address", example = "Calle Falsa 123, Buenos Aires")
    val address: String? = null,
    
    @field:Past(message = "Birth date must be in the past")
    @Schema(description = "Student's birth date", example = "2005-03-15")
    val birthDate: LocalDate? = null
)

@Schema(description = "Request to update an existing student")
data class UpdateStudentRequest(
    @Schema(description = "Student's first name", example = "Sofía")
    val firstName: String? = null,
    
    @Schema(description = "Student's last name", example = "Martínez")
    val lastName: String? = null,
    
    @Schema(description = "Student's phone number", example = "1134567890")
    val phone: String? = null,
    
    @field:Email(message = "Invalid email format")
    @Schema(description = "Student's email address", example = "sofia@email.com")
    val email: String? = null,
    
    @Schema(description = "Student's home address", example = "Calle Falsa 123, Buenos Aires")
    val address: String? = null,
    
    @field:Past(message = "Birth date must be in the past")
    @Schema(description = "Student's birth date", example = "2005-03-15")
    val birthDate: LocalDate? = null
)

@Schema(description = "Student information response")
data class StudentResponse(
    @Schema(description = "Unique student identifier", example = "1")
    val id: Long,
    
    @Schema(description = "Student's first name", example = "Sofía")
    val firstName: String,
    
    @Schema(description = "Student's last name", example = "Martínez")
    val lastName: String,
    
    @Schema(description = "Student's full name", example = "Sofía Martínez")
    val fullName: String,
    
    @Schema(description = "Student's phone number", example = "1134567890")
    val phone: String,
    
    @Schema(description = "Student's email address", example = "sofia@email.com")
    val email: String?,
    
    @Schema(description = "Student's home address", example = "Calle Falsa 123, Buenos Aires")
    val address: String?,
    
    @Schema(description = "Student's birth date", example = "2005-03-15")
    val birthDate: LocalDate?,
    
    @Schema(description = "List of class IDs the student is enrolled in", example = "[1, 2]")
    val classIds: List<Long>
) {
    companion object {
        fun from(student: Student): StudentResponse {
            return StudentResponse(
                id = student.id,
                firstName = student.firstName,
                lastName = student.lastName,
                fullName = student.fullName,
                phone = student.phone,
                email = student.email,
                address = student.address,
                birthDate = student.birthDate,
                classIds = student.classes.map { it.id }
            )
        }
    }
}

@Schema(description = "Request to enroll or unenroll a student from a class")
data class EnrollmentRequest(
    @Schema(description = "Student ID", example = "1", required = true)
    val studentId: Long,
    
    @Schema(description = "Class ID", example = "1", required = true)
    val classId: Long,
    
    @Schema(description = "Enrollment date (optional, defaults to current date)", example = "2024-01-15")
    val enrollmentDate: LocalDate? = null,
    
    @Schema(description = "Additional notes about the enrollment")
    val notes: String? = null
)

@Schema(description = "Student enrollment information with date details")
data class StudentEnrollmentResponse(
    @Schema(description = "Enrollment ID", example = "1")
    val id: Long,
    
    @Schema(description = "Student ID", example = "1")
    val studentId: Long,
    
    @Schema(description = "Student's full name", example = "Sofía Martínez")
    val studentName: String,
    
    @Schema(description = "Class ID", example = "1")
    val classId: Long,
    
    @Schema(description = "Class name", example = "Ballet Intermedio")
    val className: String,
    
    @Schema(description = "Date when student enrolled", example = "2024-01-15")
    val enrollmentDate: LocalDate,
    
    @Schema(description = "Additional notes about the enrollment")
    val notes: String?,
    
    @Schema(description = "Whether the enrollment is currently active")
    val isActive: Boolean
) {
    companion object {
        fun from(enrollment: com.nova.crm.entity.StudentEnrollment): StudentEnrollmentResponse {
            return StudentEnrollmentResponse(
                id = enrollment.id,
                studentId = enrollment.student.id,
                studentName = enrollment.student.fullName,
                classId = enrollment.danceClass.id,
                className = enrollment.danceClass.name,
                enrollmentDate = enrollment.enrollmentDate,
                notes = enrollment.notes,
                isActive = enrollment.isActive
            )
        }
    }
}

@Schema(description = "Enhanced student response with enrollment details")
data class StudentWithEnrollmentsResponse(
    @Schema(description = "Unique student identifier", example = "1")
    val id: Long,
    
    @Schema(description = "Student's first name", example = "Sofía")
    val firstName: String,
    
    @Schema(description = "Student's last name", example = "Martínez")
    val lastName: String,
    
    @Schema(description = "Student's full name", example = "Sofía Martínez")
    val fullName: String,
    
    @Schema(description = "Student's phone number", example = "1134567890")
    val phone: String,
    
    @Schema(description = "Student's email address", example = "sofia@email.com")
    val email: String?,
    
    @Schema(description = "Student's home address", example = "Calle Falsa 123, Buenos Aires")
    val address: String?,
    
    @Schema(description = "Student's birth date", example = "2005-03-15")
    val birthDate: LocalDate?,
    
    @Schema(description = "List of active enrollments with dates")
    val enrollments: List<StudentEnrollmentResponse>
) {
    companion object {
        fun from(student: Student, enrollments: List<com.nova.crm.entity.StudentEnrollment>): StudentWithEnrollmentsResponse {
            return StudentWithEnrollmentsResponse(
                id = student.id,
                firstName = student.firstName,
                lastName = student.lastName,
                fullName = student.fullName,
                phone = student.phone,
                email = student.email,
                address = student.address,
                birthDate = student.birthDate,
                enrollments = enrollments.map { StudentEnrollmentResponse.from(it) }
            )
        }
    }
}
