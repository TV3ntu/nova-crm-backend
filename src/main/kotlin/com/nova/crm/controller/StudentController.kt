package com.nova.crm.controller

import com.nova.crm.dto.*
import com.nova.crm.entity.Student
import com.nova.crm.service.PaymentService
import com.nova.crm.service.StudentEnrollmentService
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
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.YearMonth

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = ["*"])
@Tag(name = "Students", description = "Student management endpoints")
@SecurityRequirement(name = "bearerAuth")
class StudentController(
    private val studentService: StudentService,
    private val studentEnrollmentService: StudentEnrollmentService,
    private val paymentService: PaymentService
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
            ApiResponse(responseCode = "404", description = "Student not found"),
            ApiResponse(responseCode = "409", description = "Cannot delete student with related data")
        ]
    )
    fun deleteStudent(
        @Parameter(description = "Student ID", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<Any> {
        return try {
            studentService.deleteById(id)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            // Handle deletion conflicts (student has related data)
            val errorResponse = ErrorResponse.conflict(
                message = "No se puede eliminar el estudiante",
                details = mapOf(
                    "errorType" to "STUDENT_HAS_RELATED_DATA",
                    "studentId" to id,
                    "reason" to (e.message ?: "El estudiante tiene datos relacionados que impiden su eliminación")
                )
            )
            ResponseEntity.status(409).body(errorResponse)
        } catch (e: Exception) {
            // Handle any other unexpected errors
            val errorResponse = ErrorResponse.badRequest(
                message = "Error interno del servidor al eliminar estudiante",
                details = mapOf(
                    "errorType" to "INTERNAL_SERVER_ERROR",
                    "studentId" to id,
                    "error" to (e.message ?: "Error desconocido")
                )
            )
            ResponseEntity.status(500).body(errorResponse)
        }
    }

    @PostMapping("/enroll")
    @Operation(
        summary = "Enroll student in class",
        description = "Enroll a student in a specific dance class with enrollment date tracking"
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
            studentEnrollmentService.enrollStudentInClass(
                studentId = request.studentId,
                classId = request.classId,
                enrollmentDate = request.enrollmentDate, // Can be null, service will use LocalDate.now()
                notes = request.notes
            )
            val student = studentService.findById(request.studentId)!!
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
        description = "Remove a student from a specific dance class using enrollment system"
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
            // Use StudentEnrollmentService to unenroll student
            studentEnrollmentService.unenrollStudentFromClass(request.studentId, request.classId)
            val student = studentService.findById(request.studentId)!!
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
            val enrollments = studentEnrollmentService.getStudentEnrollments(id)
            val classes = enrollments.map { it.danceClass }
            val response = classes.map { DanceClassResponse.from(it) }
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{id}/enrollments")
    @Operation(
        summary = "Get student enrollments",
        description = "Retrieve all active enrollments for a specific student with enrollment dates and notes"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Student enrollments retrieved successfully",
                content = [Content(schema = Schema(implementation = StudentEnrollmentResponse::class))]
            ),
            ApiResponse(responseCode = "404", description = "Student not found")
        ]
    )
    fun getStudentEnrollments(
        @Parameter(description = "Student ID", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<List<StudentEnrollmentResponse>> {
        val student = studentService.findById(id)
            ?: return ResponseEntity.notFound().build()
        
        val enrollments = studentEnrollmentService.getStudentEnrollments(id)
        val response = enrollments.map { StudentEnrollmentResponse.from(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/outstanding-payments/{month}")
    @Operation(
        summary = "Get student outstanding payments for a specific month",
        description = "Retrieve all classes that a student hasn't paid for in a specific month, respecting enrollment dates"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Outstanding payments retrieved successfully",
                content = [Content(schema = Schema(implementation = StudentOutstandingPaymentsResponse::class))]
            ),
            ApiResponse(responseCode = "404", description = "Student not found"),
            ApiResponse(responseCode = "400", description = "Invalid month format")
        ]
    )
    fun getStudentOutstandingPayments(
        @Parameter(description = "Student ID", example = "1")
        @PathVariable id: Long,
        @Parameter(description = "Month in YYYY-MM format", example = "2025-01")
        @PathVariable @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth
    ): ResponseEntity<Any> {
        return try {
            val student = studentService.findById(id)
                ?: return ResponseEntity.notFound().build()
            
            val outstandingPayments = paymentService.calculateStudentOutstandingPayments(id, month)
            val response = StudentOutstandingPaymentsResponse.from(
                studentId = id,
                studentName = student.fullName,
                month = month,
                outstandingPayments = outstandingPayments
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.badRequest(
                    message = "Error al obtener pagos pendientes del estudiante",
                    details = mapOf(
                        "errorType" to "OUTSTANDING_PAYMENTS_ERROR",
                        "studentId" to id,
                        "month" to month.toString(),
                        "error" to (e.message ?: "Error desconocido")
                    )
                ))
        }
    }

    @GetMapping("/{studentId}/enrollment/{classId}")
    @Operation(
        summary = "Get specific enrollment details",
        description = "Get enrollment details for a specific student-class combination"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Enrollment details retrieved successfully",
                content = [Content(schema = Schema(implementation = StudentEnrollmentResponse::class))]
            ),
            ApiResponse(responseCode = "404", description = "Enrollment not found")
        ]
    )
    fun getEnrollmentDetails(
        @Parameter(description = "Student ID", example = "1")
        @PathVariable studentId: Long,
        @Parameter(description = "Class ID", example = "1")
        @PathVariable classId: Long
    ): ResponseEntity<StudentEnrollmentResponse> {
        val enrollment = studentService.getEnrollmentDetails(studentId, classId)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(StudentEnrollmentResponse.from(enrollment))
    }

    @GetMapping("/enrollments/date-range")
    @Operation(
        summary = "Get enrollments by date range",
        description = "Retrieve all enrollments within a specific date range"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Enrollments retrieved successfully",
                content = [Content(schema = Schema(implementation = StudentEnrollmentResponse::class))]
            )
        ]
    )
    fun getEnrollmentsByDateRange(
        @Parameter(description = "Start date", example = "2024-01-01")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @Parameter(description = "End date", example = "2024-12-31")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<StudentEnrollmentResponse>> {
        val enrollments = studentService.getEnrollmentsByDateRange(startDate, endDate)
        val response = enrollments.map { StudentEnrollmentResponse.from(it) }
        return ResponseEntity.ok(response)
    }
}
