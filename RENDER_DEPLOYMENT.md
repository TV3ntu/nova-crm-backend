# 🚀 NOVA Dance Studio CRM - Render Deployment Guide

## 📋 Prerequisites
- GitHub repository with your code
- Render account (free tier available)
- PostgreSQL database (Render provides free PostgreSQL)

## 🔧 Render Deployment Steps

### 1. **Prepare Your Repository**
Ensure these files are in your repository:
- ✅ `render.yaml` - Render service configuration
- ✅ `build.sh` - Custom build script
- ✅ `gradlew` - Gradle wrapper (executable)
- ✅ All source code and configurations

### 2. **Deploy to Render**

#### Option A: Using render.yaml (Recommended)
1. **Push to GitHub:**
   ```bash
   git add .
   git commit -m "Add Render deployment configuration"
   git push origin main
   ```

2. **Connect to Render:**
   - Go to [render.com](https://render.com)
   - Click "New +" → "Blueprint"
   - Connect your GitHub repository
   - Render will automatically detect `render.yaml`

#### Option B: Manual Setup
1. **Create Web Service:**
   - Go to Render Dashboard
   - Click "New +" → "Web Service"
   - Connect GitHub repository
   - Configure:
     - **Build Command:** `./build.sh`
     - **Start Command:** `java -Dserver.port=$PORT -jar build/libs/nova-crm-*.jar --spring.profiles.active=prod`
     - **Environment:** Java

2. **Create PostgreSQL Database:**
   - Click "New +" → "PostgreSQL"
   - Name: `nova-crm-db`
   - Plan: Free
   - Region: Oregon (same as web service)

### 3. **Environment Variables**
Set these in Render Dashboard → Web Service → Environment:

#### Required Variables:
```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=<auto-populated by Render PostgreSQL>
JWT_SECRET=<generate secure 32+ character string>
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<generate secure password>
```

#### Optional Variables:
```bash
SWAGGER_ENABLED=false
API_DOCS_ENABLED=false
JAVA_OPTS=-Xmx512m -Xms256m
```

### 4. **Generate Secure Secrets**
```bash
# Generate JWT Secret (32+ characters)
openssl rand -base64 32

# Generate Admin Password
openssl rand -base64 16
```

## 🔗 **Render Configuration Details**

### render.yaml Configuration:
- **Build Command:** `./gradlew build -x test`
- **Start Command:** Auto-configured for Spring Boot
- **Health Check:** `/api/auth/login`
- **Plan:** Free tier (sufficient for development)
- **Region:** Oregon (fast and reliable)

### Database Configuration:
- **PostgreSQL 14+** (Render managed)
- **Connection:** Auto-configured via `DATABASE_URL`
- **Backups:** Included in free tier
- **SSL:** Enabled by default

## 🌐 **Access Your Application**

After successful deployment:
- **Application URL:** `https://nova-crm-xxxx.onrender.com`
- **API Base:** `https://nova-crm-xxxx.onrender.com/api`
- **Health Check:** `https://nova-crm-xxxx.onrender.com/api/auth/login`

## 🔐 **First Time Setup**

1. **Test Authentication:**
   ```bash
   curl -X POST https://your-app.onrender.com/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"your-admin-password"}'
   ```

2. **Get JWT Token** from response and use for API calls:
   ```bash
   curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     https://your-app.onrender.com/api/students
   ```

## 📊 **Monitoring & Logs**

### Render Dashboard:
- **Logs:** Real-time application logs
- **Metrics:** CPU, Memory, Response times
- **Health:** Service status and uptime
- **Database:** Connection stats and queries

### Application Logs:
- Authentication attempts
- Database connections
- API requests (INFO level)
- Error tracking

## 🚨 **Troubleshooting**

### Common Issues:

1. **Build Fails:**
   ```bash
   # Check gradlew permissions
   chmod +x gradlew
   git add gradlew
   git commit -m "Fix gradlew permissions"
   git push
   ```

2. **Database Connection Error:**
   - Verify `DATABASE_URL` is set correctly
   - Check PostgreSQL service is running
   - Ensure both services are in same region

3. **Application Won't Start:**
   - Check `JAVA_OPTS` memory settings
   - Verify all required environment variables
   - Review logs in Render dashboard

4. **JWT Token Issues:**
   - Ensure `JWT_SECRET` is 32+ characters
   - Check `ADMIN_PASSWORD` is set correctly

### Performance Tips:
- Use **Starter** plan for production (more memory)
- Enable **Auto-Deploy** for continuous deployment
- Set up **Custom Domain** for professional URLs

## 🔄 **Continuous Deployment**

Render automatically deploys when you push to your main branch:
```bash
git add .
git commit -m "Update application"
git push origin main
# Render will automatically build and deploy
```

## 💰 **Cost Estimation**

### Free Tier (Development):
- **Web Service:** Free (with limitations)
- **PostgreSQL:** Free (1GB storage)
- **Total:** $0/month

### Production Ready:
- **Web Service:** $7/month (Starter plan)
- **PostgreSQL:** $7/month (Starter plan)
- **Total:** $14/month

## 📞 **Support**

- **Render Docs:** [render.com/docs](https://render.com/docs)
- **Application Logs:** Available in Render dashboard
- **Database Access:** Via Render dashboard or external tools

---

## 🎉 **You're Ready!**

Your NOVA Dance Studio CRM is now configured for Render deployment with:
- ✅ Automatic builds and deployments
- ✅ Managed PostgreSQL database
- ✅ SSL certificates
- ✅ Environment variable management
- ✅ Health monitoring
- ✅ Scalable infrastructure

Just push your code to GitHub and deploy! 🚀
