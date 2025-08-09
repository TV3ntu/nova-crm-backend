package com.nova.crm.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "teachers")
data class Teacher(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    @NotBlank(message = "First name is required")
    val firstName: String,

    @Column(nullable = false)
    @NotBlank(message = "Last name is required")
    val lastName: String,

    @Column(nullable = false)
    @NotBlank(message = "Phone is required")
    val phone: String,

    @Email(message = "Invalid email format")
    val email: String? = null,

    val address: String? = null,

    @Column(nullable = false)
    val isStudioOwner: Boolean = false,

    @ManyToMany(mappedBy = "teachers", fetch = FetchType.LAZY)
    val classes: MutableSet<DanceClass> = mutableSetOf()
) {
    val fullName: String
        get() = "$firstName $lastName"

    /**
     * Calculate teacher's share percentage based on whether they are the studio owner
     * Studio owner gets 100%, other teachers get 50%
     */
    val sharePercentage: Double
        get() = if (isStudioOwner) 1.0 else 0.5

    override fun toString(): String {
        return "Teacher(id=$id, firstName='$firstName', lastName='$lastName', isStudioOwner=$isStudioOwner)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Teacher
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
