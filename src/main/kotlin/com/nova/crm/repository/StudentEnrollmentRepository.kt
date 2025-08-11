package com.nova.crm.repository

import com.nova.crm.entity.StudentEnrollment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface StudentEnrollmentRepository : JpaRepository<StudentEnrollment, Long> {
    
    fun findByStudentIdAndDanceClassIdAndIsActive(
        studentId: Long, 
        danceClassId: Long, 
        isActive: Boolean = true
    ): StudentEnrollment?
    
    fun findByStudentIdAndIsActive(studentId: Long, isActive: Boolean = true): List<StudentEnrollment>
    
    fun findByDanceClassIdAndIsActive(danceClassId: Long, isActive: Boolean = true): List<StudentEnrollment>
    
    @Query("""
        SELECT se FROM StudentEnrollment se 
        WHERE se.student.id = :studentId 
        AND se.danceClass.id = :classId 
        AND se.isActive = true
    """)
    fun findActiveEnrollment(
        @Param("studentId") studentId: Long, 
        @Param("classId") classId: Long
    ): StudentEnrollment?
    
    @Query("""
        SELECT se FROM StudentEnrollment se 
        WHERE se.enrollmentDate BETWEEN :startDate AND :endDate 
        AND se.isActive = true
        ORDER BY se.enrollmentDate DESC
    """)
    fun findEnrollmentsByDateRange(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<StudentEnrollment>
    
    @Query("""
        SELECT COUNT(se) FROM StudentEnrollment se 
        WHERE se.danceClass.id = :classId 
        AND se.isActive = true
    """)
    fun countActiveEnrollmentsByClass(@Param("classId") classId: Long): Long
    
    @Query("""
        SELECT COUNT(se) FROM StudentEnrollment se 
        WHERE se.student.id = :studentId 
        AND se.isActive = true
    """)
    fun countActiveEnrollmentsByStudent(@Param("studentId") studentId: Long): Long
}
