package com.nova.crm.dto

import com.nova.crm.entity.ClassSchedule
import com.nova.crm.entity.DanceClass
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.DayOfWeek

data class CreateDanceClassRequest(
    @field:NotBlank(message = "Class name is required")
    val name: String,
    
    val description: String? = null,
    
    @field:Positive(message = "Price must be positive")
    val price: BigDecimal,
    
    @field:Positive(message = "Duration must be positive")
    val durationHours: Double,
    
    val schedules: List<ClassScheduleDto> = emptyList()
)

data class UpdateDanceClassRequest(
    val name: String? = null,
    val description: String? = null,
    val price: BigDecimal? = null,
    val durationHours: Double? = null
)

data class ClassScheduleDto(
    val dayOfWeek: DayOfWeek,
    val startHour: Int,
    val startMinute: Int = 0
) {
    fun toEntity(): ClassSchedule {
        return ClassSchedule(dayOfWeek, startHour, startMinute)
    }
    
    companion object {
        fun from(schedule: ClassSchedule): ClassScheduleDto {
            return ClassScheduleDto(
                dayOfWeek = schedule.dayOfWeek,
                startHour = schedule.startHour,
                startMinute = schedule.startMinute
            )
        }
    }
}

data class DanceClassResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val durationHours: Double,
    val schedules: List<ClassScheduleDto>,
    val teacherIds: List<Long>,
    val studentIds: List<Long>
) {
    companion object {
        fun from(danceClass: DanceClass): DanceClassResponse {
            return DanceClassResponse(
                id = danceClass.id,
                name = danceClass.name,
                description = danceClass.description,
                price = danceClass.price,
                durationHours = danceClass.durationHours,
                schedules = danceClass.schedules.map { ClassScheduleDto.from(it) },
                teacherIds = danceClass.teachers.map { it.id },
                studentIds = danceClass.students.map { it.id }
            )
        }
    }
}
