#!/bin/bash

# Render Build Script for NOVA Dance Studio CRM
echo "🚀 Building NOVA Dance Studio CRM for Render deployment..."

# Make gradlew executable
chmod +x gradlew

# Clean and build the application
echo "📦 Building application..."
./gradlew clean build -x test

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✅ Build completed successfully!"
    echo "📁 JAR file created: $(ls build/libs/*.jar)"
else
    echo "❌ Build failed!"
    exit 1
fi

echo "🎉 Ready for deployment on Render!"
