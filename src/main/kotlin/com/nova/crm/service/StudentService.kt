package com.nova.crm.service

import com.nova.crm.entity.DanceClass
import com.nova.crm.entity.Student
import com.nova.crm.repository.DanceClassRepository
import com.nova.crm.repository.StudentRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
@Transactional
class StudentService(
    private val studentRepository: StudentRepository,
    private val danceClassRepository: DanceClassRepository
) {

    fun findAll(): List<Student> = studentRepository.findAll()

    fun findById(id: Long): Student? = studentRepository.findByIdOrNull(id)

    fun findByName(firstName: String, lastName: String): List<Student> {
        return studentRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
            firstName, lastName
        )
    }

    fun findByPhone(phone: String): List<Student> {
        return studentRepository.findByPhoneContaining(phone)
    }

    fun save(student: Student): Student = studentRepository.save(student)

    fun deleteById(id: Long) {
        val student = findById(id) ?: throw IllegalArgumentException("Student not found with id: $id")
        
        // Remove student from all classes before deletion
        student.classes.forEach { danceClass ->
            danceClass.removeStudent(student)
        }
        
        studentRepository.deleteById(id)
    }

    fun enrollInClass(studentId: Long, classId: Long): Student {
        val student = findById(studentId) ?: throw IllegalArgumentException("Student not found with id: $studentId")
        val danceClass = danceClassRepository.findByIdOrNull(classId) 
            ?: throw IllegalArgumentException("Class not found with id: $classId")

        // Check if student is already enrolled
        if (student.classes.contains(danceClass)) {
            throw IllegalStateException("Student is already enrolled in this class")
        }

        danceClass.addStudent(student)
        return studentRepository.save(student)
    }

    fun unenrollFromClass(studentId: Long, classId: Long): Student {
        val student = findById(studentId) ?: throw IllegalArgumentException("Student not found with id: $studentId")
        val danceClass = danceClassRepository.findByIdOrNull(classId) 
            ?: throw IllegalArgumentException("Class not found with id: $classId")

        if (!student.classes.contains(danceClass)) {
            throw IllegalStateException("Student is not enrolled in this class")
        }

        danceClass.removeStudent(student)
        return studentRepository.save(student)
    }

    fun findStudentsWithoutPaymentForMonth(month: YearMonth): List<Student> {
        return studentRepository.findStudentsWithoutPaymentForMonth(month)
    }

    fun findStudentsWithoutPaymentForClassAndMonth(classId: Long, month: YearMonth): List<Student> {
        return studentRepository.findStudentsWithoutPaymentForClassAndMonth(classId, month)
    }

    fun getStudentClasses(studentId: Long): List<DanceClass> {
        val student = findById(studentId) ?: throw IllegalArgumentException("Student not found with id: $studentId")
        return student.classes.toList()
    }
}
