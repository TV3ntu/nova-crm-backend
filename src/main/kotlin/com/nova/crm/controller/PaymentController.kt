package com.nova.crm.controller

import com.nova.crm.dto.*
import com.nova.crm.exception.*
import com.nova.crm.service.PaymentService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.YearMonth

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = ["*"])
class PaymentController(
    private val paymentService: PaymentService
) {

    @GetMapping
    fun getAllPayments(): ResponseEntity<List<PaymentResponse>> {
        val payments = paymentService.findAll()
        val response = payments.map { PaymentResponse.from(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getPaymentById(@PathVariable id: Long): ResponseEntity<PaymentResponse> {
        val payment = paymentService.findById(id)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(PaymentResponse.from(payment))
    }

    @GetMapping("/student/{studentId}")
    fun getPaymentsByStudent(@PathVariable studentId: Long): ResponseEntity<List<PaymentResponse>> {
        val payments = paymentService.findByStudentId(studentId)
        val response = payments.map { PaymentResponse.from(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/class/{classId}")
    fun getPaymentsByClass(@PathVariable classId: Long): ResponseEntity<List<PaymentResponse>> {
        val payments = paymentService.findByClassId(classId)
        val response = payments.map { PaymentResponse.from(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/month/{month}")
    fun getPaymentsByMonth(
        @PathVariable @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth
    ): ResponseEntity<List<PaymentResponse>> {
        val payments = paymentService.findByMonth(month)
        val response = payments.map { PaymentResponse.from(it) }
        return ResponseEntity.ok(response)
    }

    @PostMapping
    fun registerPayment(@Valid @RequestBody request: CreatePaymentRequest): ResponseEntity<*> {
        return try {
            val payment = paymentService.registerPayment(
                studentId = request.studentId,
                classId = request.classId,
                amount = request.amount,
                paymentMonth = request.paymentMonth,
                paymentDate = request.paymentDate,
                notes = request.notes,
                paymentMethod = request.paymentMethod
            )
            ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentResponse.from(payment))
        } catch (e: StudentNotFoundException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse.badRequest(
                    message = e.message ?: "Estudiante no encontrado",
                    details = mapOf(
                        "errorType" to "STUDENT_NOT_FOUND",
                        "studentId" to request.studentId
                    )
                ))
        } catch (e: DanceClassNotFoundException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse.badRequest(
                    message = e.message ?: "Clase no encontrada",
                    details = mapOf(
                        "errorType" to "CLASS_NOT_FOUND",
                        "classId" to request.classId
                    )
                ))
        } catch (e: StudentNotEnrolledException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse.badRequest(
                    message = e.message ?: "Estudiante no inscrito en la clase",
                    details = mapOf(
                        "errorType" to "STUDENT_NOT_ENROLLED",
                        "studentId" to request.studentId,
                        "classId" to request.classId
                    )
                ))
        } catch (e: DuplicatePaymentException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.conflict(
                    message = e.message ?: "Ya existe un pago para este período",
                    details = mapOf(
                        "errorType" to "DUPLICATE_PAYMENT",
                        "studentId" to request.studentId,
                        "classId" to request.classId,
                        "paymentMonth" to request.paymentMonth.toString()
                    )
                ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse.badRequest(
                    message = e.message ?: "Parámetros de solicitud inválidos",
                    details = mapOf(
                        "errorType" to "INVALID_PARAMETERS",
                        "studentId" to request.studentId,
                        "classId" to request.classId
                    )
                ))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.conflict(
                    message = e.message ?: "Conflicto en la operación de pago",
                    details = mapOf(
                        "errorType" to "PAYMENT_CONFLICT",
                        "studentId" to request.studentId,
                        "classId" to request.classId,
                        "paymentMonth" to request.paymentMonth.toString()
                    )
                ))
        }
    }

    @PostMapping("/multi-class")
    fun registerMultiClassPayment(@Valid @RequestBody request: CreateMultiClassPaymentRequest): ResponseEntity<*> {
        return try {
            val payments = paymentService.registerMultiClassPayment(
                studentId = request.studentId,
                totalAmount = request.totalAmount,
                paymentMonth = request.paymentMonth,
                paymentDate = request.paymentDate,
                paymentMethod = request.paymentMethod,
                notes = request.notes,
                classIds = request.classIds
            )
            ResponseEntity.status(HttpStatus.CREATED)
                .body(payments.map { PaymentResponse.from(it) })
        } catch (e: StudentNotFoundException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse.badRequest(
                    message = e.message ?: "Estudiante no encontrado",
                    details = mapOf(
                        "errorType" to "STUDENT_NOT_FOUND",
                        "studentId" to request.studentId
                    )
                ))
        } catch (e: StudentNotEnrolledInAnyClassException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse.badRequest(
                    message = e.message ?: "Estudiante no inscrito en ninguna clase",
                    details = mapOf(
                        "errorType" to "STUDENT_NOT_ENROLLED_ANY_CLASS",
                        "studentId" to request.studentId
                    )
                ))
        } catch (e: InsufficientAmountException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse.badRequest(
                    message = e.message ?: "Monto insuficiente para cubrir todas las clases",
                    details = mapOf(
                        "errorType" to "INSUFFICIENT_AMOUNT",
                        "studentId" to request.studentId,
                        "providedAmount" to request.totalAmount,
                        "paymentMonth" to request.paymentMonth.toString()
                    )
                ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse.badRequest(
                    message = e.message ?: "Parámetros de solicitud inválidos",
                    details = mapOf(
                        "errorType" to "INVALID_PARAMETERS",
                        "studentId" to request.studentId
                    )
                ))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.conflict(
                    message = e.message ?: "Conflicto en la operación de pago múltiple",
                    details = mapOf(
                        "errorType" to "MULTI_PAYMENT_CONFLICT",
                        "studentId" to request.studentId,
                        "paymentMonth" to request.paymentMonth.toString()
                    )
                ))
        }
    }

    @PutMapping("/{id}")
    fun updatePayment(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdatePaymentRequest
    ): ResponseEntity<*> {
        return try {
            val payment = paymentService.updatePayment(
                paymentId = id,
                amount = request.amount,
                paymentDate = request.paymentDate,
                paymentMethod = request.paymentMethod,
                notes = request.notes
            )
            ResponseEntity.ok(PaymentResponse.from(payment))
        } catch (e: PaymentNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(
                    message = e.message ?: "Pago no encontrado",
                    status = 404,
                    details = mapOf(
                        "errorType" to "PAYMENT_NOT_FOUND",
                        "paymentId" to id
                    )
                ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse.badRequest(
                    message = e.message ?: "Parámetros de actualización inválidos",
                    details = mapOf(
                        "errorType" to "INVALID_UPDATE_PARAMETERS",
                        "paymentId" to id
                    )
                ))
        }
    }

    @DeleteMapping("/{id}")
    fun deletePayment(@PathVariable id: Long): ResponseEntity<*> {
        return try {
            paymentService.deletePayment(id)
            ResponseEntity.noContent().build<Any>()
        } catch (e: PaymentNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(
                    message = e.message ?: "Pago no encontrado",
                    status = 404,
                    details = mapOf(
                        "errorType" to "PAYMENT_NOT_FOUND",
                        "paymentId" to id
                    )
                ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(
                    message = "Error interno al eliminar el pago: ${e.message}",
                    status = 500,
                    details = mapOf(
                        "errorType" to "DELETE_ERROR",
                        "paymentId" to id
                    )
                ))
        }
    }

    @GetMapping("/late/{month}")
    fun getLatePayments(
        @PathVariable @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth
    ): ResponseEntity<List<PaymentResponse>> {
        val latePayments = paymentService.getLatePaymentsForMonth(month)
        val response = latePayments.map { PaymentResponse.from(it) }
        return ResponseEntity.ok(response)
    }
}
