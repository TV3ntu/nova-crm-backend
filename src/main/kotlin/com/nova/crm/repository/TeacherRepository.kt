package com.nova.crm.repository

import com.nova.crm.entity.Teacher
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.YearMonth

@Repository
interface TeacherRepository : JpaRepository<Teacher, Long> {
    
    fun findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
        firstName: String, 
        lastName: String
    ): List<Teacher>
    
    fun findByIsStudioOwner(isStudioOwner: Boolean): List<Teacher>
    
    @Query("""
        SELECT DISTINCT t FROM Teacher t 
        JOIN t.classes c 
        WHERE c.id = :classId
    """)
    fun findByClassId(@Param("classId") classId: Long): List<Teacher>
    
    @Query("""
        SELECT t FROM Teacher t
        JOIN t.classes c
        JOIN c.payments p
        WHERE p.paymentMonth = :month
        GROUP BY t.id
    """)
    fun findTeachersWithPaymentsForMonth(@Param("month") month: YearMonth): List<Teacher>
}
