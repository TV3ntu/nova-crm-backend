# NOVA Dance Studio CRM - Deployment Guide

## 🚀 Deployment Configuration

This application is configured to use **H2 database for local development** and **PostgreSQL for production deployment**.

## 📋 Prerequisites

### For Production Deployment:
- PostgreSQL 12+ server
- Java 17+
- At least 512MB RAM
- Environment variables configured

## 🔧 Environment Setup

### Local Development (H2)
```bash
# Run with development profile (default)
./gradlew bootRun

# Or explicitly set profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Production Deployment (PostgreSQL)
```bash
# Set environment variables
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://your-db-host:5432/nova_crm
export DATABASE_USERNAME=nova_user
export DATABASE_PASSWORD=your-secure-password
export JWT_SECRET=your-very-secure-jwt-secret-key-minimum-256-bits
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=your-secure-admin-password

# Optional: Disable Swagger in production
export SWAGGER_ENABLED=false
export API_DOCS_ENABLED=false

# Run application
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## 🗄️ Database Setup

### PostgreSQL Setup
1. **Create Database:**
   ```sql
   CREATE DATABASE nova_crm;
   CREATE USER nova_user WITH PASSWORD 'your-secure-password';
   GRANT ALL PRIVILEGES ON DATABASE nova_crm TO nova_user;
   ```

2. **Run Initialization Script:**
   ```bash
   psql -U postgres -d nova_crm -f database/init.sql
   ```

3. **Application will auto-create tables** on first run using Hibernate DDL.

## ☁️ Cloud Deployment Options

### Heroku
```bash
# Add PostgreSQL addon
heroku addons:create heroku-postgresql:hobby-dev

# Set environment variables
heroku config:set SPRING_PROFILES_ACTIVE=prod
heroku config:set JWT_SECRET=your-jwt-secret
heroku config:set ADMIN_USERNAME=admin
heroku config:set ADMIN_PASSWORD=your-password
heroku config:set SWAGGER_ENABLED=false

# Deploy
git push heroku master
```

### Railway
```bash
# Set environment variables in Railway dashboard:
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=postgresql://user:pass@host:port/db
JWT_SECRET=your-jwt-secret
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your-password
```

### Docker Deployment
```dockerfile
FROM openjdk:17-jdk-slim

COPY build/libs/nova-crm-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=prod"]
```

## 🔒 Security Considerations

### Production Environment Variables:
- **JWT_SECRET**: Must be at least 256 bits (32+ characters)
- **ADMIN_PASSWORD**: Use a strong password
- **DATABASE_PASSWORD**: Use a secure database password
- **SWAGGER_ENABLED**: Set to `false` in production

### Recommended Production Settings:
```bash
export JWT_SECRET=$(openssl rand -base64 32)
export ADMIN_PASSWORD=$(openssl rand -base64 16)
export SWAGGER_ENABLED=false
export API_DOCS_ENABLED=false
```

## 📊 Health Checks

### Application Health
- **Health Check URL**: `GET /actuator/health` (if Spring Actuator is enabled)
- **API Status**: `POST /api/auth/login` with valid credentials

### Database Connection
The application will fail to start if database connection fails, providing clear error messages.

## 🔍 Monitoring & Logs

### Log Levels by Profile:
- **Development**: DEBUG level for detailed logging
- **Production**: INFO level for performance

### Key Log Locations:
- Authentication attempts
- Database connection status
- Payment processing
- API request/response (dev only)

## 🚨 Troubleshooting

### Common Issues:

1. **Database Connection Failed**
   - Verify DATABASE_URL format
   - Check database server is running
   - Confirm user permissions

2. **JWT Token Issues**
   - Ensure JWT_SECRET is properly set
   - Check token expiration settings

3. **Port Already in Use**
   - Change server.port in application.yml
   - Or set SERVER_PORT environment variable

### Support
For deployment issues, check:
1. Application logs
2. Database connectivity
3. Environment variables
4. Network/firewall settings

## 📈 Performance Tuning

### Database Connection Pool (Production):
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### JVM Options (Production):
```bash
export JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"
```
