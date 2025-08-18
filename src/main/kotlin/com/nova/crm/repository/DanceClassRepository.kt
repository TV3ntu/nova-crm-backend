package com.nova.crm.repository

import com.nova.crm.entity.DanceClass
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.DayOfWeek

@Repository
interface DanceClassRepository : JpaRepository<DanceClass, Long> {
    
    fun findByNameContainingIgnoreCase(name: String): List<DanceClass>
    
    @Query("""
        SELECT DISTINCT c FROM DanceClass c 
        JOIN StudentEnrollment se ON se.danceClass.id = c.id
        WHERE se.student.id = :studentId AND se.isActive = true
    """)
    fun findByStudentId(@Param("studentId") studentId: Long): List<DanceClass>
    
    @Query("""
        SELECT DISTINCT c FROM DanceClass c 
        JOIN c.teachers t 
        WHERE t.id = :teacherId
    """)
    fun findByTeacherId(@Param("teacherId") teacherId: Long): List<DanceClass>
    
    @Query("""
        SELECT c FROM DanceClass c 
        JOIN c.schedules s 
        WHERE s.dayOfWeek = :dayOfWeek 
        AND s.startHour = :hour 
        AND s.startMinute = :minute
    """)
    fun findBySchedule(
        @Param("dayOfWeek") dayOfWeek: DayOfWeek,
        @Param("hour") hour: Int,
        @Param("minute") minute: Int
    ): List<DanceClass>
    
    @Query("""
        SELECT c FROM DanceClass c 
        JOIN c.schedules s 
        WHERE s.dayOfWeek = :dayOfWeek
    """)
    fun findByDayOfWeek(@Param("dayOfWeek") dayOfWeek: DayOfWeek): List<DanceClass>
}
