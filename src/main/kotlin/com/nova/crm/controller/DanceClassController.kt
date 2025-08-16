package com.nova.crm.controller

import com.nova.crm.dto.*
import com.nova.crm.entity.DanceClass
import com.nova.crm.service.DanceClassService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.DayOfWeek

@RestController
@RequestMapping("/api/classes")
@CrossOrigin(origins = ["*"])
class DanceClassController(
    private val danceClassService: DanceClassService
) {

    @GetMapping
    fun getAllClasses(): ResponseEntity<List<DanceClassResponse>> {
        val classes = danceClassService.findAll()
        val response = classes.map { DanceClassResponse.from(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getClassById(@PathVariable id: Long): ResponseEntity<DanceClassResponse> {
        val danceClass = danceClassService.findById(id)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(DanceClassResponse.from(danceClass))
    }

    @GetMapping("/search")
    fun searchClasses(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) dayOfWeek: DayOfWeek?
    ): ResponseEntity<List<DanceClassResponse>> {
        val classes = when {
            !name.isNullOrBlank() -> {
                danceClassService.findByName(name)
            }
            dayOfWeek != null -> {
                danceClassService.findByDayOfWeek(dayOfWeek)
            }
            else -> {
                danceClassService.findAll()
            }
        }
        
        val response = classes.map { DanceClassResponse.from(it) }
        return ResponseEntity.ok(response)
    }

    @PostMapping
    fun createClass(@Valid @RequestBody request: CreateDanceClassRequest): ResponseEntity<DanceClassResponse> {
        val schedules = request.schedules.map { it.toEntity() }.toMutableSet()
        
        val danceClass = DanceClass(
            name = request.name,
            description = request.description,
            price = request.price,
            durationHours = request.durationHours,
            schedules = schedules
        )
        
        return try {
            val savedClass = danceClassService.save(danceClass)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(DanceClassResponse.from(savedClass))
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{id}")
    fun updateClass(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateDanceClassRequest
    ): ResponseEntity<DanceClassResponse> {
        val existingClass = danceClassService.findById(id)
            ?: return ResponseEntity.notFound().build()

        val updatedClass = existingClass.copy(
            name = request.name ?: existingClass.name,
            description = request.description ?: existingClass.description,
            price = request.price ?: existingClass.price,
            durationHours = request.durationHours ?: existingClass.durationHours
        )

        return try {
            val savedClass = danceClassService.save(updatedClass)
            ResponseEntity.ok(DanceClassResponse.from(savedClass))
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete dance class",
        description = "Remove a dance class from the system, unenrolling all students and unassigning teachers"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Class deleted successfully"),
            ApiResponse(responseCode = "404", description = "Class not found"),
            ApiResponse(responseCode = "409", description = "Cannot delete class with related data")
        ]
    )
    fun deleteClass(@PathVariable id: Long): ResponseEntity<Any> {
        return try {
            danceClassService.deleteById(id)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            // Handle deletion conflicts (class has related data that cannot be removed)
            val errorResponse = ErrorResponse.conflict(
                message = "No se puede eliminar la clase",
                details = mapOf(
                    "errorType" to "CLASS_HAS_RELATED_DATA",
                    "classId" to id,
                    "reason" to (e.message ?: "La clase tiene datos relacionados que impiden su eliminación")
                )
            )
            ResponseEntity.status(409).body(errorResponse)
        } catch (e: Exception) {
            // Handle any other unexpected errors
            val errorResponse = ErrorResponse.badRequest(
                message = "Error interno del servidor al eliminar clase",
                details = mapOf(
                    "errorType" to "INTERNAL_SERVER_ERROR",
                    "classId" to id,
                    "error" to (e.message ?: "Error desconocido")
                )
            )
            ResponseEntity.status(500).body(errorResponse)
        }
    }

    @PostMapping("/{id}/schedules")
    fun addSchedule(
        @PathVariable id: Long,
        @Valid @RequestBody scheduleDto: ClassScheduleDto
    ): ResponseEntity<DanceClassResponse> {
        return try {
            val updatedClass = danceClassService.addSchedule(id, scheduleDto.toEntity())
            ResponseEntity.ok(DanceClassResponse.from(updatedClass))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}/schedules")
    fun removeSchedule(
        @PathVariable id: Long,
        @Valid @RequestBody scheduleDto: ClassScheduleDto
    ): ResponseEntity<DanceClassResponse> {
        return try {
            val updatedClass = danceClassService.removeSchedule(id, scheduleDto.toEntity())
            ResponseEntity.ok(DanceClassResponse.from(updatedClass))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/{id}/students")
    fun getClassStudents(@PathVariable id: Long): ResponseEntity<List<StudentResponse>> {
        return try {
            val students = danceClassService.getClassStudents(id)
            val response = students.map { StudentResponse.from(it) }
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{id}/teachers")
    fun getClassTeachers(@PathVariable id: Long): ResponseEntity<List<TeacherResponse>> {
        return try {
            val teachers = danceClassService.getClassTeachers(id)
            val response = teachers.map { TeacherResponse.from(it) }
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }
}
