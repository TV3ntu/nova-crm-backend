package com.nova.crm.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "student_enrollments",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["student_id", "class_id"])
    ]
)
data class StudentEnrollment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    val student: Student,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    val danceClass: DanceClass,

    @Column(nullable = false)
    val enrollmentDate: LocalDate = LocalDate.now(),

    val notes: String? = null,

    @Column(nullable = false)
    val isActive: Boolean = true
) {
    override fun toString(): String {
        return "StudentEnrollment(id=$id, studentId=${student.id}, classId=${danceClass.id}, enrollmentDate=$enrollmentDate, isActive=$isActive)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as StudentEnrollment
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
