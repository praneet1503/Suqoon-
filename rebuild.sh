#!/bin/bash
cd /Users/Praneet/AndroidStudioProjects/Suqoon-

# Clear gradle cache
rm -rf .gradle
rm -rf app/build

# Run a simple gradle command
echo "=== Running Gradle tasks list ==="
./gradlew tasks --all 2>&1 | grep -i test

echo ""
echo "=== Attempting to build ==="
./gradlew assembleDebug 2>&1 | tail -50

