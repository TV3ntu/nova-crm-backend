package com.nova.crm.service

import com.nova.crm.entity.ClassSchedule
import com.nova.crm.entity.DanceClass
import com.nova.crm.entity.Student
import com.nova.crm.entity.Teacher
import com.nova.crm.repository.DanceClassRepository
import com.nova.crm.repository.StudentRepository
import com.nova.crm.repository.TeacherRepository
import com.nova.crm.service.StudentEnrollmentService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek

@Service
@Transactional
class DanceClassService(
    private val danceClassRepository: DanceClassRepository,
    private val studentRepository: StudentRepository,
    private val teacherRepository: TeacherRepository,
    private val studentEnrollmentService: StudentEnrollmentService
) {

    fun findAll(): List<DanceClass> = danceClassRepository.findAll()

    fun findById(id: Long): DanceClass? = danceClassRepository.findByIdOrNull(id)

    fun findByName(name: String): List<DanceClass> {
        return danceClassRepository.findByNameContainingIgnoreCase(name)
    }

    fun findByStudentId(studentId: Long): List<DanceClass> {
        return danceClassRepository.findByStudentId(studentId)
    }

    fun findByTeacherId(teacherId: Long): List<DanceClass> {
        return danceClassRepository.findByTeacherId(teacherId)
    }

    fun findByDayOfWeek(dayOfWeek: DayOfWeek): List<DanceClass> {
        return danceClassRepository.findByDayOfWeek(dayOfWeek)
    }

    fun save(danceClass: DanceClass): DanceClass {
        validateSchedules(danceClass)
        return danceClassRepository.save(danceClass)
    }

    fun deleteById(id: Long) {
        val danceClass = findById(id) ?: throw IllegalArgumentException("Class not found with id: $id")
        
        try {
            // 1. Desinscribir todos los estudiantes de la clase
            val enrollments = studentEnrollmentService.getClassEnrollments(id)
            enrollments.forEach { enrollment ->
                studentEnrollmentService.unenrollStudentFromClass(enrollment.student.id, id)
            }
            
            // 2. Desasignar todos los profesores de la clase
            danceClass.teachers.forEach { teacher ->
                teacher.classes.remove(danceClass)
            }
            
            // 3. Los pagos se mantienen intactos para registro histórico
            // (No eliminamos pagos - como solicitado por el usuario)
            
            // 4. Finalmente eliminar la clase
            danceClassRepository.deleteById(id)
            
        } catch (e: Exception) {
            throw IllegalStateException("Cannot delete class with id: $id. Error: ${e.message}", e)
        }
    }

    fun addSchedule(classId: Long, schedule: ClassSchedule): DanceClass {
        val danceClass = findById(classId) ?: throw IllegalArgumentException("Class not found with id: $classId")
        
        // Check if schedule already exists
        if (danceClass.schedules.contains(schedule)) {
            throw IllegalStateException("Schedule already exists for this class")
        }
        
        danceClass.schedules.add(schedule)
        validateSchedules(danceClass)
        
        return danceClassRepository.save(danceClass)
    }

    fun removeSchedule(classId: Long, schedule: ClassSchedule): DanceClass {
        val danceClass = findById(classId) ?: throw IllegalArgumentException("Class not found with id: $classId")
        
        if (!danceClass.schedules.remove(schedule)) {
            throw IllegalStateException("Schedule not found for this class")
        }
        
        return danceClassRepository.save(danceClass)
    }

    fun getClassStudents(classId: Long): List<Student> {
        val enrollments = studentEnrollmentService.getClassEnrollments(classId)
        return enrollments.map { it.student }
    }

    fun getClassTeachers(classId: Long): List<Teacher> {
        val danceClass = findById(classId) ?: throw IllegalArgumentException("Class not found with id: $classId")
        return danceClass.teachers.toList()
    }

    fun findClassesWithScheduleConflict(schedule: ClassSchedule): List<DanceClass> {
        return danceClassRepository.findBySchedule(
            schedule.dayOfWeek,
            schedule.startHour,
            schedule.startMinute
        )
    }

    private fun validateSchedules(danceClass: DanceClass) {
        // Check for duplicate schedules within the same class
        val scheduleSet = mutableSetOf<ClassSchedule>()
        for (schedule in danceClass.schedules) {
            if (!scheduleSet.add(schedule)) {
                throw IllegalStateException("Duplicate schedule found: $schedule")
            }
        }

        // Check for conflicts with teachers' other classes
        for (teacher in danceClass.teachers) {
            for (otherClass in teacher.classes) {
                if (otherClass.id != danceClass.id) {
                    for (schedule in danceClass.schedules) {
                        for (otherSchedule in otherClass.schedules) {
                            if (schedule.dayOfWeek == otherSchedule.dayOfWeek &&
                                schedule.startHour == otherSchedule.startHour &&
                                schedule.startMinute == otherSchedule.startMinute) {
                                throw IllegalStateException(
                                    "Schedule conflict: Teacher ${teacher.fullName} " +
                                    "has conflicting classes ${danceClass.name} and ${otherClass.name} " +
                                    "on ${schedule.dayOfWeek} at ${schedule.timeString}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
