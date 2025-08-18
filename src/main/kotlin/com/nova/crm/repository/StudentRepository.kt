package com.nova.crm.repository

import com.nova.crm.entity.Student
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.YearMonth

@Repository
interface StudentRepository : JpaRepository<Student, Long> {
    
    fun findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
        firstName: String, 
        lastName: String
    ): List<Student>
    
    fun findByPhoneContaining(phone: String): List<Student>
    
    @Query("""
        SELECT DISTINCT s FROM Student s 
        JOIN StudentEnrollment se ON se.student.id = s.id
        WHERE se.danceClass.id = :classId AND se.isActive = true
    """)
    fun findByClassId(@Param("classId") classId: Long): List<Student>
    
    @Query("""
        SELECT DISTINCT s FROM Student s 
        LEFT JOIN s.payments p ON p.paymentMonth = :month
        JOIN StudentEnrollment se ON se.student.id = s.id
        WHERE p.id IS NULL AND se.isActive = true
    """)
    fun findStudentsWithoutPaymentForMonth(@Param("month") month: YearMonth): List<Student>
    
    @Query("""
        SELECT s FROM Student s 
        WHERE s.id NOT IN (
            SELECT DISTINCT p.student.id FROM Payment p 
            WHERE p.paymentMonth = :month AND p.danceClass.id = :classId
        )
        AND s.id IN (
            SELECT DISTINCT se.student.id FROM StudentEnrollment se
            WHERE se.danceClass.id = :classId AND se.isActive = true
        )
    """)
    fun findStudentsWithoutPaymentForClassAndMonth(
        @Param("classId") classId: Long,
        @Param("month") month: YearMonth
    ): List<Student>
}
