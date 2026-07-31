#!/bin/bash
# 1. Build and install the app to the phone
./gradlew installDebug

# 2. If the build was successful, launch the app automatically
if [ $? -eq 0 ]; then
    echo "✅ Build successful! Launching app..."
    adb shell am start -n com.example.first_app/.MainActivity
else
    echo "❌ Build failed. Check the errors above."
fi
