package com.nova.crm.controller

import com.nova.crm.dto.*
import com.nova.crm.entity.Teacher
import com.nova.crm.service.TeacherService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/teachers")
@CrossOrigin(origins = ["*"])
class TeacherController(
    private val teacherService: TeacherService
) {

    @GetMapping
    fun getAllTeachers(): ResponseEntity<List<TeacherResponse>> {
        val teachers = teacherService.findAll()
        val response = teachers.map { TeacherResponse.from(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getTeacherById(@PathVariable id: Long): ResponseEntity<TeacherResponse> {
        val teacher = teacherService.findById(id)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(TeacherResponse.from(teacher))
    }

    @GetMapping("/search")
    fun searchTeachers(
        @RequestParam(required = false) firstName: String?,
        @RequestParam(required = false) lastName: String?,
        @RequestParam(required = false) isStudioOwner: Boolean?
    ): ResponseEntity<List<TeacherResponse>> {
        val teachers = when {
            !firstName.isNullOrBlank() && !lastName.isNullOrBlank() -> {
                teacherService.findByName(firstName, lastName)
            }
            isStudioOwner == true -> {
                teacherService.findStudioOwners()
            }
            isStudioOwner == false -> {
                teacherService.findRegularTeachers()
            }
            else -> {
                teacherService.findAll()
            }
        }
        
        val response = teachers.map { TeacherResponse.from(it) }
        return ResponseEntity.ok(response)
    }

    @PostMapping
    fun createTeacher(@Valid @RequestBody request: CreateTeacherRequest): ResponseEntity<TeacherResponse> {
        val teacher = Teacher(
            firstName = request.firstName,
            lastName = request.lastName,
            phone = request.phone,
            email = request.email,
            address = request.address,
            isStudioOwner = request.isStudioOwner
        )
        
        val savedTeacher = teacherService.save(teacher)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(TeacherResponse.from(savedTeacher))
    }

    @PutMapping("/{id}")
    fun updateTeacher(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTeacherRequest
    ): ResponseEntity<TeacherResponse> {
        val existingTeacher = teacherService.findById(id)
            ?: return ResponseEntity.notFound().build()

        val updatedTeacher = existingTeacher.copy(
            firstName = request.firstName ?: existingTeacher.firstName,
            lastName = request.lastName ?: existingTeacher.lastName,
            phone = request.phone ?: existingTeacher.phone,
            email = request.email ?: existingTeacher.email,
            address = request.address ?: existingTeacher.address,
            isStudioOwner = request.isStudioOwner ?: existingTeacher.isStudioOwner
        )

        val savedTeacher = teacherService.save(updatedTeacher)
        return ResponseEntity.ok(TeacherResponse.from(savedTeacher))
    }

    @DeleteMapping("/{id}")
    fun deleteTeacher(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            teacherService.deleteById(id)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/assign")
    fun assignTeacherToClass(@Valid @RequestBody request: TeacherAssignmentRequest): ResponseEntity<TeacherResponse> {
        return try {
            val teacher = teacherService.assignToClass(request.teacherId, request.classId)
            ResponseEntity.ok(TeacherResponse.from(teacher))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/unassign")
    fun unassignTeacherFromClass(@Valid @RequestBody request: TeacherAssignmentRequest): ResponseEntity<TeacherResponse> {
        return try {
            val teacher = teacherService.unassignFromClass(request.teacherId, request.classId)
            ResponseEntity.ok(TeacherResponse.from(teacher))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/{id}/classes")
    fun getTeacherClasses(@PathVariable id: Long): ResponseEntity<List<DanceClassResponse>> {
        return try {
            val classes = teacherService.getTeacherClasses(id)
            val response = classes.map { DanceClassResponse.from(it) }
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }
}
