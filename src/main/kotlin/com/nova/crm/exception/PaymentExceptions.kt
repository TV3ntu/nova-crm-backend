package com.nova.crm.exception

/**
 * Excepciones específicas para el manejo de pagos
 */

class StudentNotFoundException(studentId: Long) : 
    IllegalArgumentException("No se encontró el estudiante con ID: $studentId")

class DanceClassNotFoundException(classId: Long) : 
    IllegalArgumentException("No se encontró la clase con ID: $classId")

class StudentNotEnrolledException(studentName: String, className: String) : 
    IllegalStateException("El estudiante $studentName no está inscrito en la clase $className")

class DuplicatePaymentException(
    studentName: String, 
    className: String, 
    month: String,
    existingPaymentId: Long
) : IllegalStateException(
    "Ya existe un pago para el estudiante $studentName en la clase $className para el mes $month (ID del pago existente: $existingPaymentId)"
)

class StudentNotEnrolledInAnyClassException(studentName: String) : 
    IllegalStateException("El estudiante $studentName no está inscrito en ninguna clase")

class InsufficientAmountException(required: String, provided: String) : 
    IllegalArgumentException("El monto proporcionado ($provided) es insuficiente. Se requiere al menos $required para cubrir todas las clases")

class PaymentNotFoundException(paymentId: Long) : 
    IllegalArgumentException("No se encontró el pago con ID: $paymentId")
