# NOVA Dance Studio CRM

A comprehensive Customer Relationship Management system built specifically for NOVA dance studio using Kotlin and Spring Boot.

## Features

### Core Functionality
- **Student Management**: Complete CRUD operations for students with enrollment tracking
- **Teacher Management**: Manage teachers with studio owner designation and class assignments
- **Class Management**: Handle dance classes with schedules, pricing, and capacity
- **Payment Processing**: Monthly payment system with late fee calculation (15% after 10th of month)
- **Multi-class Payments**: Support for students paying for multiple classes at once

### Business Rules
- **Revenue Split**: 50/50 between studio and teachers (100% to studio for studio owner classes)
- **Late Payments**: 15% penalty for payments made after the 10th of each month
- **Flexible Pricing**: Administrators can apply custom discounts and promotions
- **Schedule Validation**: Prevents teacher scheduling conflicts

### Reporting & Analytics
- **Outstanding Payments**: Track which students haven't paid and amounts owed
- **Teacher Compensation**: Calculate teacher earnings with detailed breakdowns
- **Financial Reports**: Monthly revenue, expenses, and profit analysis
- **Class Reports**: Individual class performance and student participation

## Technology Stack

- **Backend**: Kotlin + Spring Boot 3.2.1
- **Database**: H2 (development) / PostgreSQL (production ready)
- **ORM**: Spring Data JPA with Hibernate
- **Testing**: JUnit 5 + MockK
- **API**: RESTful endpoints with JSON responses

## Getting Started

### Prerequisites
- Java 17 or higher
- Gradle 7.0 or higher

### Running the Application

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd nova-crm
   ```

2. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

3. **Access the application**
   - API Base URL: `http://localhost:8080/api`
   - H2 Console: `http://localhost:8080/h2-console`
     - JDBC URL: `jdbc:h2:mem:testdb`
     - Username: `sa`
     - Password: `password`

### Running Tests
```bash
./gradlew test
```

## API Endpoints

### Students
- `GET /api/students` - List all students
- `GET /api/students/{id}` - Get student by ID
- `POST /api/students` - Create new student
- `PUT /api/students/{id}` - Update student
- `DELETE /api/students/{id}` - Delete student
- `POST /api/students/enroll` - Enroll student in class
- `DELETE /api/students/unenroll` - Unenroll student from class

### Teachers
- `GET /api/teachers` - List all teachers
- `GET /api/teachers/{id}` - Get teacher by ID
- `POST /api/teachers` - Create new teacher
- `PUT /api/teachers/{id}` - Update teacher
- `DELETE /api/teachers/{id}` - Delete teacher
- `POST /api/teachers/assign` - Assign teacher to class
- `DELETE /api/teachers/unassign` - Unassign teacher from class

### Classes
- `GET /api/classes` - List all classes
- `GET /api/classes/{id}` - Get class by ID
- `POST /api/classes` - Create new class
- `PUT /api/classes/{id}` - Update class
- `DELETE /api/classes/{id}` - Delete class
- `POST /api/classes/{id}/schedules` - Add schedule to class
- `DELETE /api/classes/{id}/schedules` - Remove schedule from class

### Payments
- `GET /api/payments` - List all payments
- `POST /api/payments` - Register single class payment
- `POST /api/payments/multi-class` - Register multi-class payment
- `GET /api/payments/student/{studentId}` - Get payments by student
- `GET /api/payments/class/{classId}` - Get payments by class
- `GET /api/payments/month/{yyyy-MM}` - Get payments by month

### Reports
- `GET /api/reports/teacher-compensation/{yyyy-MM}` - Teacher compensation report
- `GET /api/reports/outstanding-payments/{yyyy-MM}` - Outstanding payments report
- `GET /api/reports/financial/{yyyy-MM}` - Monthly financial report
- `GET /api/reports/class/{classId}/{yyyy-MM}` - Individual class report

## Data Models

### Student
```json
{
  "id": 1,
  "firstName": "Ana",
  "lastName": "García",
  "phone": "123456789",
  "email": "ana@example.com",
  "address": "Calle Falsa 123",
  "birthDate": "2000-05-15"
}
```

### Teacher
```json
{
  "id": 1,
  "firstName": "María",
  "lastName": "López",
  "phone": "987654321",
  "email": "maria@example.com",
  "isStudioOwner": false,
  "sharePercentage": 0.5
}
```

### Dance Class
```json
{
  "id": 1,
  "name": "Danza Clásica",
  "description": "Clase de ballet clásico para principiantes",
  "price": 5000.00,
  "durationHours": 1.5,
  "schedules": [
    {
      "dayOfWeek": "MONDAY",
      "startHour": 17,
      "startMinute": 0
    }
  ]
}
```

### Payment
```json
{
  "id": 1,
  "studentId": 1,
  "classId": 1,
  "amount": 5000.00,
  "paymentMonth": "2024-02",
  "paymentDate": "2024-02-05",
  "isLatePayment": false,
  "notes": "Pago completo del mes"
}
```

## Business Logic Examples

### Late Payment Calculation
```kotlin
// Payment made after 10th of month gets 15% penalty
val basePrice = BigDecimal("5000.00")
val latePaymentPrice = basePrice.multiply(BigDecimal("1.15")) // 5750.00
```

### Teacher Compensation
```kotlin
// Regular teacher gets 50%, studio owner gets 100%
val revenue = BigDecimal("10000.00")
val teacherShare = if (teacher.isStudioOwner) revenue else revenue.multiply(BigDecimal("0.5"))
```

### Multi-Class Payment
```kotlin
// Student enrolled in 3 classes: 5000, 3000, 2000
// Makes single payment of 10000 - all classes marked as paid
```

## Configuration

### Database Configuration
The application uses H2 in-memory database by default. To use PostgreSQL:

1. Update `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nova_crm
    username: your_username
    password: your_password
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

2. Ensure PostgreSQL dependency is available (already included in build.gradle.kts)

## Testing

The application includes comprehensive unit tests covering:
- Payment processing logic
- Student enrollment/unenrollment
- Teacher assignment validation
- Late fee calculations
- Multi-class payment distribution

Run tests with: `./gradlew test`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## License

This project is proprietary software developed for NOVA Dance Studio.
