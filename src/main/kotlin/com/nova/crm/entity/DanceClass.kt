package com.nova.crm.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.DayOfWeek

@Entity
@Table(name = "dance_classes")
data class DanceClass(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    @NotBlank(message = "Class name is required")
    val name: String,

    val description: String? = null,

    @Column(nullable = false, precision = 10, scale = 2)
    @Positive(message = "Price must be positive")
    val price: BigDecimal,

    @Column(nullable = false)
    @Positive(message = "Duration must be positive")
    val durationHours: Double,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "class_schedules", joinColumns = [JoinColumn(name = "class_id")])
    val schedules: MutableSet<ClassSchedule> = mutableSetOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "class_teachers",
        joinColumns = [JoinColumn(name = "class_id")],
        inverseJoinColumns = [JoinColumn(name = "teacher_id")]
    )
    val teachers: MutableSet<Teacher> = mutableSetOf(),

    @OneToMany(mappedBy = "danceClass", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val payments: MutableList<Payment> = mutableListOf()
) {
    fun addTeacher(teacher: Teacher) {
        teachers.add(teacher)
        teacher.classes.add(this)
    }

    fun removeTeacher(teacher: Teacher) {
        teachers.remove(teacher)
        teacher.classes.remove(this)
    }

    override fun toString(): String {
        return "DanceClass(id=$id, name='$name', price=$price, durationHours=$durationHours)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DanceClass
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@Embeddable
data class ClassSchedule(
    @Enumerated(EnumType.STRING)
    val dayOfWeek: DayOfWeek,

    @Column(nullable = false)
    val startHour: Int, // 0-23

    @Column(nullable = false)
    val startMinute: Int = 0 // 0-59
) {
    init {
        require(startHour in 0..23) { "Start hour must be between 0 and 23" }
        require(startMinute in 0..59) { "Start minute must be between 0 and 59" }
    }

    val timeString: String
        get() = String.format("%02d:%02d", startHour, startMinute)

    override fun toString(): String {
        return "$dayOfWeek $timeString"
    }
}
