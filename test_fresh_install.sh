#!/bin/bash

# Test Fresh Install Script for WealthTracker
# This script completely clears app data and reinstalls the app for testing

echo "🧹 Clearing app data..."
adb shell pm clear com.example.wealthtracker

echo "🗑️  Uninstalling app..."
adb uninstall com.example.wealthtracker

echo "✨ Cleaning and building..."
./gradlew clean assembleDebug

echo "📱 Installing fresh build..."
./gradlew installDebug

echo "🚀 Launching app..."
adb shell am start -n com.example.wealthtracker/.MainActivity

echo "✅ Fresh install complete! App should now show onboarding → country selection → dashboard"
