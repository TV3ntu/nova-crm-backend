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
    fun registerPayment(@Valid @RequestBody request: CreatePaymentRequest): ResponseEntity<PaymentResponse> {
        return try {
            val payment = paymentService.registerPayment(
                studentId = request.studentId,
                classId = request.classId,
                amount = request.amount,
                paymentMonth = request.paymentMonth,
                paymentDate = request.paymentDate,
                notes = request.notes
            )
            ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentResponse.from(payment))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/multi-class")
    fun registerMultiClassPayment(@Valid @RequestBody request: CreateMultiClassPaymentRequest): ResponseEntity<List<PaymentResponse>> {
        return try {
            val payments = paymentService.registerMultiClassPayment(
                studentId = request.studentId,
                totalAmount = request.totalAmount,
                paymentMonth = request.paymentMonth,
                paymentDate = request.paymentDate,
                notes = request.notes
            )
            val response = payments.map { PaymentResponse.from(it) }
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
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
