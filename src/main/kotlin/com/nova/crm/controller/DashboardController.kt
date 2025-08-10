package com.nova.crm.controller

import com.nova.crm.service.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.YearMonth

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = ["*"])
@Tag(name = "Dashboard", description = "Dashboard metrics and KPIs")
@SecurityRequirement(name = "bearerAuth")
class DashboardController(
    private val studentService: StudentService,
    private val teacherService: TeacherService,
    private val danceClassService: DanceClassService,
    private val paymentService: PaymentService,
    private val reportingService: ReportingService
) {

    @GetMapping
    @Operation(
        summary = "Get dashboard metrics",
        description = "Retrieve main KPIs and metrics for the dashboard"
    )
    @ApiResponse(responseCode = "200", description = "Dashboard metrics retrieved successfully")
    fun getDashboardMetrics(): Map<String, Any> {
        val currentMonth = YearMonth.now()
        
        // Basic counts
        val totalStudents = studentService.findAll().size
        val totalTeachers = teacherService.findAll().size
        val totalClasses = danceClassService.findAll().size
        
        // Monthly payments
        val monthlyPayments = paymentService.findByMonth(currentMonth)
        val monthlyRevenue = monthlyPayments.sumOf { it.amount }
        
        // Outstanding payments using ReportingService
        val outstandingReport = reportingService.generateOutstandingPaymentsReport(currentMonth)
        val outstandingPayments = outstandingReport.outstandingPayments.values.flatten()
        val outstandingAmount = outstandingReport.totalOutstandingAmount
        
        // Classes today (simplified)
        val classesToday = totalClasses // Simplified for now
        
        // Recent activity (last 5 payments)
        val recentPayments = paymentService.findAll()
            .sortedByDescending { it.paymentDate }
            .take(5)
            .map { payment ->
                mapOf(
                    "id" to payment.id,
                    "studentName" to "${payment.student.firstName} ${payment.student.lastName}",
                    "amount" to payment.amount,
                    "date" to payment.paymentDate.toString(),
                    "status" to if (payment.isLatePayment) "LATE" else "ON_TIME"
                )
            }
        
        return mapOf(
            "kpis" to mapOf(
                "totalStudents" to totalStudents,
                "totalTeachers" to totalTeachers,
                "totalClasses" to totalClasses,
                "monthlyRevenue" to monthlyRevenue,
                "outstandingPayments" to outstandingPayments.size,
                "outstandingAmount" to outstandingAmount,
                "classesToday" to classesToday
            ),
            "recentActivity" to recentPayments,
            "alerts" to listOf(
                if (outstandingPayments.isNotEmpty()) 
                    mapOf(
                        "type" to "warning",
                        "message" to "${outstandingPayments.size} pagos pendientes",
                        "count" to outstandingPayments.size
                    )
                else null
            ).filterNotNull(),
            "quickStats" to mapOf(
                "averageClassSize" to if (totalClasses > 0) totalStudents / totalClasses.toDouble() else 0.0,
                "activeStudentsPercentage" to 100, // Simplified
                "monthlyGrowth" to "+5%" // Placeholder
            )
        )
    }

    @GetMapping("/revenue-chart")
    @Operation(
        summary = "Get revenue chart data",
        description = "Get monthly revenue data for charts"
    )
    fun getRevenueChart(): Map<String, Any> {
        // Get last 6 months of data
        val months = mutableListOf<Map<String, Any>>()
        
        for (i in 5 downTo 0) {
            val month = YearMonth.now().minusMonths(i.toLong())
            val payments = paymentService.findByMonth(month)
            val revenue = payments.sumOf { it.amount }
            
            months.add(mapOf(
                "month" to month.toString(),
                "revenue" to revenue,
                "payments" to payments.size
            ))
        }
        
        return mapOf(
            "months" to months,
            "totalRevenue" to months.sumOf { (it["revenue"] as Number).toDouble() },
            "averageMonthly" to if (months.isNotEmpty()) 
                months.sumOf { (it["revenue"] as Number).toDouble() } / months.size 
                else 0.0
        )
    }

    @GetMapping("/class-distribution")
    @Operation(
        summary = "Get class distribution data",
        description = "Get student distribution across classes"
    )
    fun getClassDistribution(): Map<String, Any> {
        val classes = danceClassService.findAll()
        
        val distribution = classes.map { danceClass ->
            val students = danceClassService.getClassStudents(danceClass.id!!)
            val maxStudents = 20 // Default capacity since it's not in the entity
            val teacher = if (danceClass.teachers.isNotEmpty()) danceClass.teachers.first() else null
            
            mapOf(
                "className" to danceClass.name,
                "studentCount" to students.size,
                "maxStudents" to maxStudents,
                "occupancyPercentage" to if (maxStudents > 0) 
                    (students.size * 100.0) / maxStudents 
                    else 0.0,
                "teacher" to if (teacher != null) "${teacher.firstName} ${teacher.lastName}" else "Sin asignar",
                "price" to danceClass.price
            )
        }
        
        return mapOf(
            "classes" to distribution,
            "totalCapacity" to distribution.sumOf { (it["maxStudents"] as Number).toInt() },
            "totalEnrolled" to distribution.sumOf { (it["studentCount"] as Number).toInt() },
            "averageOccupancy" to if (distribution.isNotEmpty()) 
                distribution.sumOf { (it["occupancyPercentage"] as Number).toDouble() } / distribution.size 
                else 0.0
        )
    }
}
