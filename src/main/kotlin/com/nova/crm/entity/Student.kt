package com.nova.crm.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import java.time.LocalDate

@Entity
@Table(name = "students")
data class Student(
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

    @Past(message = "Birth date must be in the past")
    val birthDate: LocalDate? = null,

    @OneToMany(mappedBy = "student", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val payments: MutableList<Payment> = mutableListOf()
) {
    val fullName: String
        get() = "$firstName $lastName"

    override fun toString(): String {
        return "Student(id=$id, firstName='$firstName', lastName='$lastName', phone='$phone')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Student
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
