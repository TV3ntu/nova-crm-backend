package com.nova.crm.controller

import com.nova.crm.service.OutstandingPayment
import com.nova.crm.service.ReportingService
import com.nova.crm.service.PaymentService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.YearMonth

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = ["*"])
class ReportController(
    private val reportingService: ReportingService,
    private val paymentService: PaymentService
) {

    @GetMapping("/teacher-compensation/{month}")
    fun getTeacherCompensationReport(
        @PathVariable @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth
    ): ResponseEntity<List<TeacherCompensationReportDto>> {
        val reports = reportingService.generateTeacherCompensationReport(month)
        val response = reports.map { TeacherCompensationReportDto.from(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/outstanding-payments/{month}")
    fun getOutstandingPaymentsReport(
        @PathVariable @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth
    ): ResponseEntity<OutstandingPaymentsReportDto> {
        val report = reportingService.generateOutstandingPaymentsReport(month)
        return ResponseEntity.ok(OutstandingPaymentsReportDto.from(report))
    }

    @GetMapping("/financial/{month}")
    fun getMonthlyFinancialReport(
        @PathVariable @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth
    ): ResponseEntity<MonthlyFinancialReportDto> {
        val report = reportingService.generateMonthlyFinancialReport(month)
        return ResponseEntity.ok(MonthlyFinancialReportDto.from(report))
    }

    @GetMapping("/class/{classId}/{month}")
    fun getClassReport(
        @PathVariable classId: Long,
        @PathVariable @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth
    ): ResponseEntity<ClassReportDto> {
        val report = reportingService.generateClassReport(classId, month)
        return ResponseEntity.ok(ClassReportDto.from(report))
    }

    @GetMapping("/revenue/{month}")
    fun getTotalRevenue(
        @PathVariable @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth
    ): ResponseEntity<RevenueDto> {
        val totalRevenue = paymentService.getTotalRevenueForMonth(month)
        return ResponseEntity.ok(RevenueDto(month, totalRevenue))
    }
}

// DTOs for reports
data class TeacherCompensationReportDto(
    val teacherId: Long,
    val teacherName: String,
    val isStudioOwner: Boolean,
    val month: YearMonth,
    val classReports: List<TeacherClassReportDto>,
    val totalCompensation: BigDecimal
) {
    companion object {
        fun from(report: com.nova.crm.service.TeacherCompensationReport): TeacherCompensationReportDto {
            return TeacherCompensationReportDto(
                teacherId = report.teacher.id,
                teacherName = report.teacher.fullName,
                isStudioOwner = report.teacher.isStudioOwner,
                month = report.month,
                classReports = report.classReports.map { TeacherClassReportDto.from(it) },
                totalCompensation = report.totalCompensation
            )
        }
    }
}

data class TeacherClassReportDto(
    val classId: Long,
    val className: String,
    val totalRevenue: BigDecimal,
    val teacherCompensation: BigDecimal,
    val studentsWhoPayment: List<StudentSummaryDto>
) {
    companion object {
        fun from(report: com.nova.crm.service.TeacherClassReport): TeacherClassReportDto {
            return TeacherClassReportDto(
                classId = report.danceClass.id,
                className = report.danceClass.name,
                totalRevenue = report.totalRevenue,
                teacherCompensation = report.teacherCompensation,
                studentsWhoPayment = report.studentsWhoPayment.map { 
                    StudentSummaryDto(it.id, it.fullName) 
                }
            )
        }
    }
}

data class OutstandingPaymentsReportDto(
    val month: YearMonth,
    val outstandingPayments: List<StudentOutstandingDto>,
    val totalOutstandingAmount: BigDecimal,
    val studentsWithOutstandingPayments: Int
) {
    companion object {
        fun from(report: com.nova.crm.service.OutstandingPaymentsReport): OutstandingPaymentsReportDto {
            return OutstandingPaymentsReportDto(
                month = report.month,
                outstandingPayments = report.outstandingPayments.map { (student, payments) ->
                    StudentOutstandingDto(
                        studentId = student.id,
                        studentName = student.fullName,
                        outstandingPayments = payments.map { OutstandingPaymentDto.from(it) },
                        totalOwed = payments.sumOf { it.expectedAmount }
                    )
                },
                totalOutstandingAmount = report.totalOutstandingAmount,
                studentsWithOutstandingPayments = report.studentsWithOutstandingPayments
            )
        }
    }
}

data class StudentOutstandingDto(
    val studentId: Long,
    val studentName: String,
    val outstandingPayments: List<OutstandingPaymentDto>,
    val totalOwed: BigDecimal
)

data class OutstandingPaymentDto(
    val classId: Long,
    val className: String,
    val expectedAmount: BigDecimal,
    val isLate: Boolean
) {
    companion object {
        fun from(payment: OutstandingPayment): OutstandingPaymentDto {
            return OutstandingPaymentDto(
                classId = payment.danceClass.id,
                className = payment.danceClass.name,
                expectedAmount = payment.expectedAmount,
                isLate = payment.isLate
            )
        }
    }
}

data class MonthlyFinancialReportDto(
    val month: YearMonth,
    val totalRevenue: BigDecimal,
    val studioRevenue: BigDecimal,
    val totalTeacherCompensation: BigDecimal,
    val totalPayments: Int,
    val latePayments: Int,
    val latePaymentAmount: BigDecimal
) {
    companion object {
        fun from(report: com.nova.crm.service.MonthlyFinancialReport): MonthlyFinancialReportDto {
            return MonthlyFinancialReportDto(
                month = report.month,
                totalRevenue = report.totalRevenue,
                studioRevenue = report.studioRevenue,
                totalTeacherCompensation = report.totalTeacherCompensation,
                totalPayments = report.totalPayments,
                latePayments = report.latePayments,
                latePaymentAmount = report.latePaymentAmount
            )
        }
    }
}

data class ClassReportDto(
    val classId: Long,
    val month: YearMonth,
    val totalRevenue: BigDecimal,
    val studentsCount: Int,
    val payments: List<PaymentSummaryDto>
) {
    companion object {
        fun from(report: com.nova.crm.service.ClassReport): ClassReportDto {
            return ClassReportDto(
                classId = report.classId,
                month = report.month,
                totalRevenue = report.totalRevenue,
                studentsCount = report.studentsCount,
                payments = report.payments.map { 
                    PaymentSummaryDto(it.id, it.student.fullName, it.amount, it.paymentDate) 
                }
            )
        }
    }
}

data class StudentSummaryDto(
    val id: Long,
    val name: String
)

data class PaymentSummaryDto(
    val id: Long,
    val studentName: String,
    val amount: BigDecimal,
    val paymentDate: java.time.LocalDate
)

data class RevenueDto(
    val month: YearMonth,
    val totalRevenue: BigDecimal
)
