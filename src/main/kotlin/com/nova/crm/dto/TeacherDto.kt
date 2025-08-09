package com.nova.crm.dto

import com.nova.crm.entity.Teacher
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CreateTeacherRequest(
    @field:NotBlank(message = "First name is required")
    val firstName: String,
    
    @field:NotBlank(message = "Last name is required")
    val lastName: String,
    
    @field:NotBlank(message = "Phone is required")
    val phone: String,
    
    @field:Email(message = "Invalid email format")
    val email: String? = null,
    
    val address: String? = null,
    
    val isStudioOwner: Boolean = false
)

data class UpdateTeacherRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    @field:Email(message = "Invalid email format")
    val email: String? = null,
    val address: String? = null,
    val isStudioOwner: Boolean? = null
)

data class TeacherResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val phone: String,
    val email: String?,
    val address: String?,
    val isStudioOwner: Boolean,
    val sharePercentage: Double,
    val classIds: List<Long>
) {
    companion object {
        fun from(teacher: Teacher): TeacherResponse {
            return TeacherResponse(
                id = teacher.id,
                firstName = teacher.firstName,
                lastName = teacher.lastName,
                fullName = teacher.fullName,
                phone = teacher.phone,
                email = teacher.email,
                address = teacher.address,
                isStudioOwner = teacher.isStudioOwner,
                sharePercentage = teacher.sharePercentage,
                classIds = teacher.classes.map { it.id }
            )
        }
    }
}

data class TeacherAssignmentRequest(
    val teacherId: Long,
    val classId: Long
)
