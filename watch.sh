#!/bin/bash

echo "👀 Watching for file changes... Press Ctrl+C to stop."
echo "Just hit 'Save' in your editor, and the app will update automatically!"

# Watch the src, res, and build.gradle files for any saves
inotifywait -m -r -e close_write -e moved_to --format '%w%f' app/src app/build.gradle |
while read FILE
do
    echo ""
    echo "⚡ Change detected in: $FILE"
    echo "🔄 Rebuilding and installing..."
    
    # 1. Build and install
    ./gradlew installDebug
    
    # 2. If successful, launch the app
    if [ $? -eq 0 ]; then
        echo "✅ Success! Launching app..."
        adb shell am start -n com.example.first_app/.MainActivity
    else
        echo "❌ Build failed. Fix the error and save again."
    fi
done
