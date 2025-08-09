package com.nova.crm.service

import com.nova.crm.entity.DanceClass
import com.nova.crm.entity.Teacher
import com.nova.crm.repository.DanceClassRepository
import com.nova.crm.repository.TeacherRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
@Transactional
class TeacherService(
    private val teacherRepository: TeacherRepository,
    private val danceClassRepository: DanceClassRepository
) {

    fun findAll(): List<Teacher> = teacherRepository.findAll()

    fun findById(id: Long): Teacher? = teacherRepository.findByIdOrNull(id)

    fun findByName(firstName: String, lastName: String): List<Teacher> {
        return teacherRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
            firstName, lastName
        )
    }

    fun findStudioOwners(): List<Teacher> = teacherRepository.findByIsStudioOwner(true)

    fun findRegularTeachers(): List<Teacher> = teacherRepository.findByIsStudioOwner(false)

    fun save(teacher: Teacher): Teacher = teacherRepository.save(teacher)

    fun deleteById(id: Long) {
        val teacher = findById(id) ?: throw IllegalArgumentException("Teacher not found with id: $id")
        
        // Remove teacher from all classes before deletion
        teacher.classes.forEach { danceClass ->
            danceClass.removeTeacher(teacher)
        }
        
        teacherRepository.deleteById(id)
    }

    fun assignToClass(teacherId: Long, classId: Long): Teacher {
        val teacher = findById(teacherId) ?: throw IllegalArgumentException("Teacher not found with id: $teacherId")
        val danceClass = danceClassRepository.findByIdOrNull(classId) 
            ?: throw IllegalArgumentException("Class not found with id: $classId")

        // Check if teacher is already assigned
        if (teacher.classes.contains(danceClass)) {
            throw IllegalStateException("Teacher is already assigned to this class")
        }

        // Check for schedule conflicts
        validateNoScheduleConflict(teacher, danceClass)

        danceClass.addTeacher(teacher)
        return teacherRepository.save(teacher)
    }

    fun unassignFromClass(teacherId: Long, classId: Long): Teacher {
        val teacher = findById(teacherId) ?: throw IllegalArgumentException("Teacher not found with id: $teacherId")
        val danceClass = danceClassRepository.findByIdOrNull(classId) 
            ?: throw IllegalArgumentException("Class not found with id: $classId")

        if (!teacher.classes.contains(danceClass)) {
            throw IllegalStateException("Teacher is not assigned to this class")
        }

        danceClass.removeTeacher(teacher)
        return teacherRepository.save(teacher)
    }

    fun getTeacherClasses(teacherId: Long): List<DanceClass> {
        val teacher = findById(teacherId) ?: throw IllegalArgumentException("Teacher not found with id: $teacherId")
        return teacher.classes.toList()
    }

    fun findTeachersWithPaymentsForMonth(month: YearMonth): List<Teacher> {
        return teacherRepository.findTeachersWithPaymentsForMonth(month)
    }

    private fun validateNoScheduleConflict(teacher: Teacher, newClass: DanceClass) {
        val existingClasses = teacher.classes
        
        for (existingClass in existingClasses) {
            for (newSchedule in newClass.schedules) {
                for (existingSchedule in existingClass.schedules) {
                    if (newSchedule.dayOfWeek == existingSchedule.dayOfWeek &&
                        newSchedule.startHour == existingSchedule.startHour &&
                        newSchedule.startMinute == existingSchedule.startMinute) {
                        throw IllegalStateException(
                            "Teacher ${teacher.fullName} has a schedule conflict: " +
                            "${existingClass.name} and ${newClass.name} both on ${newSchedule.dayOfWeek} at ${newSchedule.timeString}"
                        )
                    }
                }
            }
        }
    }
}
