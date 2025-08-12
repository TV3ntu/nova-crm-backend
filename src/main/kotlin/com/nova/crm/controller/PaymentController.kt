package com.nova.crm.controller

import com.nova.crm.dto.*
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
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse.badRequest(
                    message = e.message ?: "Invalid request parameters",
                    details = mapOf(
                        "studentId" to request.studentId,
                        "classId" to request.classId
                    )
                ))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.conflict(
                    message = e.message ?: "Payment operation conflict",
                    details = mapOf(
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
                notes = request.notes
            )
            val response = payments.map { PaymentResponse.from(it) }
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse.badRequest(
                    message = e.message ?: "Invalid request parameters",
                    details = mapOf(
                        "studentId" to request.studentId,
                        "totalAmount" to request.totalAmount
                    )
                ))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.conflict(
                    message = e.message ?: "Payment operation conflict",
                    details = mapOf(
                        "studentId" to request.studentId,
                        "totalAmount" to request.totalAmount,
                        "paymentMonth" to request.paymentMonth.toString()
                    )
                ))
        }
    }

    @DeleteMapping("/{id}")
    fun deletePayment(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            paymentService.deletePayment(id)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
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
