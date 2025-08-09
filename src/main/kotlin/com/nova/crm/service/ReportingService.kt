package com.nova.crm.service

import com.nova.crm.entity.DanceClass
import com.nova.crm.entity.Payment
import com.nova.crm.entity.Student
import com.nova.crm.entity.Teacher
import com.nova.crm.repository.PaymentRepository
import com.nova.crm.repository.TeacherRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

@Service
@Transactional(readOnly = true)
class ReportingService(
    private val paymentRepository: PaymentRepository,
    private val teacherRepository: TeacherRepository,
    private val paymentService: PaymentService
) {

    fun generateTeacherCompensationReport(month: YearMonth): List<TeacherCompensationReport> {
        val teachersWithPayments = teacherRepository.findTeachersWithPaymentsForMonth(month)
        
        return teachersWithPayments.map { teacher ->
            val classReports = generateTeacherClassReports(teacher, month)
            val totalCompensation = classReports.sumOf { it.teacherCompensation }
            
            TeacherCompensationReport(
                teacher = teacher,
                month = month,
                classReports = classReports,
                totalCompensation = totalCompensation
            )
        }
    }

    fun generateOutstandingPaymentsReport(month: YearMonth): OutstandingPaymentsReport {
        val outstandingPayments = paymentService.calculateOutstandingPayments(month)
        val totalOutstanding = outstandingPayments.values.flatten().sumOf { it.expectedAmount }
        val studentsCount = outstandingPayments.size
        
        return OutstandingPaymentsReport(
            month = month,
            outstandingPayments = outstandingPayments,
            totalOutstandingAmount = totalOutstanding,
            studentsWithOutstandingPayments = studentsCount
        )
    }

    fun generateMonthlyFinancialReport(month: YearMonth): MonthlyFinancialReport {
        val totalRevenue = paymentService.getTotalRevenueForMonth(month)
        val payments = paymentRepository.findByPaymentMonth(month)
        val latePayments = paymentRepository.findLatePaymentsForMonth(month)
        
        // Calculate teacher compensations
        val teacherCompensations = generateTeacherCompensationReport(month)
        val totalTeacherCompensation = teacherCompensations.sumOf { it.totalCompensation }
        
        // Calculate studio revenue (total revenue minus teacher compensations)
        val studioRevenue = totalRevenue.subtract(totalTeacherCompensation)
        
        return MonthlyFinancialReport(
            month = month,
            totalRevenue = totalRevenue,
            studioRevenue = studioRevenue,
            totalTeacherCompensation = totalTeacherCompensation,
            totalPayments = payments.size,
            latePayments = latePayments.size,
            latePaymentAmount = latePayments.sumOf { it.amount }
        )
    }

    fun generateClassReport(classId: Long, month: YearMonth): ClassReport {
        val payments = paymentRepository.findByDanceClassIdAndPaymentMonth(classId, month)
        val totalRevenue = payments.sumOf { it.amount }
        val studentsWhoPayment = payments.map { it.student }.distinct()
        
        return ClassReport(
            classId = classId,
            month = month,
            payments = payments,
            totalRevenue = totalRevenue,
            studentsCount = studentsWhoPayment.size
        )
    }

    private fun generateTeacherClassReports(teacher: Teacher, month: YearMonth): List<TeacherClassReport> {
        return teacher.classes.mapNotNull { danceClass ->
            val payments = paymentRepository.findByDanceClassIdAndPaymentMonth(danceClass.id, month)
            
            if (payments.isNotEmpty()) {
                val totalRevenue = payments.sumOf { it.amount }
                val teacherCompensation = totalRevenue.multiply(BigDecimal.valueOf(teacher.sharePercentage))
                    .setScale(2, RoundingMode.HALF_UP)
                
                TeacherClassReport(
                    danceClass = danceClass,
                    payments = payments,
                    totalRevenue = totalRevenue,
                    teacherCompensation = teacherCompensation,
                    studentsWhoPayment = payments.map { it.student }.distinct()
                )
            } else {
                null
            }
        }
    }
}

data class TeacherCompensationReport(
    val teacher: Teacher,
    val month: YearMonth,
    val classReports: List<TeacherClassReport>,
    val totalCompensation: BigDecimal
)

data class TeacherClassReport(
    val danceClass: DanceClass,
    val payments: List<Payment>,
    val totalRevenue: BigDecimal,
    val teacherCompensation: BigDecimal,
    val studentsWhoPayment: List<Student>
)

data class OutstandingPaymentsReport(
    val month: YearMonth,
    val outstandingPayments: Map<Student, List<OutstandingPayment>>,
    val totalOutstandingAmount: BigDecimal,
    val studentsWithOutstandingPayments: Int
)

data class MonthlyFinancialReport(
    val month: YearMonth,
    val totalRevenue: BigDecimal,
    val studioRevenue: BigDecimal,
    val totalTeacherCompensation: BigDecimal,
    val totalPayments: Int,
    val latePayments: Int,
    val latePaymentAmount: BigDecimal
)

data class ClassReport(
    val classId: Long,
    val month: YearMonth,
    val payments: List<Payment>,
    val totalRevenue: BigDecimal,
    val studentsCount: Int
)
