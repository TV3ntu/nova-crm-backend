package com.nova.crm.controller

import com.nova.crm.dto.*
import com.nova.crm.entity.Student
import com.nova.crm.service.StudentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = ["*"])
@Tag(name = "Students", description = "Student management endpoints")
@SecurityRequirement(name = "bearerAuth")
class StudentController(
    private val studentService: StudentService
) {

    @GetMapping
    @Operation(
        summary = "Get all students",
        description = "Retrieve a list of all students enrolled in the dance studio"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of students retrieved successfully",
        content = [Content(schema = Schema(implementation = StudentResponse::class))]
    )
    fun getAllStudents(): ResponseEntity<List<StudentResponse>> {
        val students = studentService.findAll()
        val response = students.map { StudentResponse.from(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get student by ID",
        description = "Retrieve a specific student by their unique identifier"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Student found",
                content = [Content(schema = Schema(implementation = StudentResponse::class))]
            ),
            ApiResponse(responseCode = "404", description = "Student not found")
        ]
    )
    fun getStudentById(
        @Parameter(description = "Student ID", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<StudentResponse> {
        val student = studentService.findById(id)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(StudentResponse.from(student))
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search students",
        description = "Search students by first name, last name, or phone number"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Search results retrieved successfully"
    )
    fun searchStudents(
        @Parameter(description = "First name to search for", example = "Ana")
        @RequestParam(required = false) firstName: String?,
        @Parameter(description = "Last name to search for", example = "García")
        @RequestParam(required = false) lastName: String?,
        @Parameter(description = "Phone number to search for", example = "123456789")
        @RequestParam(required = false) phone: String?
    ): ResponseEntity<List<StudentResponse>> {
        val students = when {
            !firstName.isNullOrBlank() && !lastName.isNullOrBlank() -> {
                studentService.findByName(firstName, lastName)
            }
            !phone.isNullOrBlank() -> {
                studentService.findByPhone(phone)
            }
            else -> {
                studentService.findAll()
            }
        }
        
        val response = students.map { StudentResponse.from(it) }
        return ResponseEntity.ok(response)
    }

    @PostMapping
    @Operation(
        summary = "Create new student",
        description = "Register a new student in the dance studio"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Student created successfully",
                content = [Content(schema = Schema(implementation = StudentResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid student data")
        ]
    )
    fun createStudent(@Valid @RequestBody request: CreateStudentRequest): ResponseEntity<StudentResponse> {
        val student = Student(
            firstName = request.firstName,
            lastName = request.lastName,
            phone = request.phone,
            email = request.email,
            address = request.address,
            birthDate = request.birthDate
        )
        
        val savedStudent = studentService.save(student)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(StudentResponse.from(savedStudent))
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update student",
        description = "Update an existing student's information"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Student updated successfully",
                content = [Content(schema = Schema(implementation = StudentResponse::class))]
            ),
            ApiResponse(responseCode = "404", description = "Student not found"),
            ApiResponse(responseCode = "400", description = "Invalid student data")
        ]
    )
    fun updateStudent(
        @Parameter(description = "Student ID", example = "1")
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateStudentRequest
    ): ResponseEntity<StudentResponse> {
        val existingStudent = studentService.findById(id)
            ?: return ResponseEntity.notFound().build()

        val updatedStudent = existingStudent.copy(
            firstName = request.firstName ?: existingStudent.firstName,
            lastName = request.lastName ?: existingStudent.lastName,
            phone = request.phone ?: existingStudent.phone,
            email = request.email ?: existingStudent.email,
            address = request.address ?: existingStudent.address,
            birthDate = request.birthDate ?: existingStudent.birthDate
        )

        val savedStudent = studentService.save(updatedStudent)
        return ResponseEntity.ok(StudentResponse.from(savedStudent))
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete student",
        description = "Remove a student from the dance studio system"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Student deleted successfully"),
            ApiResponse(responseCode = "404", description = "Student not found")
        ]
    )
    fun deleteStudent(
        @Parameter(description = "Student ID", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        return try {
            studentService.deleteById(id)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/enroll")
    @Operation(
        summary = "Enroll student in class",
        description = "Enroll a student in a specific dance class"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Student enrolled successfully",
                content = [Content(schema = Schema(implementation = StudentResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid enrollment data or student already enrolled")
        ]
    )
    fun enrollStudentInClass(@Valid @RequestBody request: EnrollmentRequest): ResponseEntity<StudentResponse> {
        return try {
            val student = studentService.enrollInClass(request.studentId, request.classId)
            ResponseEntity.ok(StudentResponse.from(student))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/unenroll")
    @Operation(
        summary = "Unenroll student from class",
        description = "Remove a student from a specific dance class"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Student unenrolled successfully",
                content = [Content(schema = Schema(implementation = StudentResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid unenrollment data or student not enrolled")
        ]
    )
    fun unenrollStudentFromClass(@Valid @RequestBody request: EnrollmentRequest): ResponseEntity<StudentResponse> {
        return try {
            val student = studentService.unenrollFromClass(request.studentId, request.classId)
            ResponseEntity.ok(StudentResponse.from(student))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/{id}/classes")
    @Operation(
        summary = "Get student's classes",
        description = "Retrieve all classes that a specific student is enrolled in"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Student's classes retrieved successfully",
                content = [Content(schema = Schema(implementation = DanceClassResponse::class))]
            ),
            ApiResponse(responseCode = "404", description = "Student not found")
        ]
    )
    fun getStudentClasses(
        @Parameter(description = "Student ID", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<List<DanceClassResponse>> {
        return try {
            val classes = studentService.getStudentClasses(id)
            val response = classes.map { DanceClassResponse.from(it) }
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }
}
