#!/bin/bash

# SMTShell-API Test Script
# This script validates the implementation and builds the project

set -e

echo "🔧 Starting SMTShell-API validation and build process..."

# Check if we're in the right directory
if [[ ! -f "settings.gradle" ]]; then
    echo "❌ Error: Not in project root directory"
    exit 1
fi

echo "📋 Validating project structure..."

# Check critical files exist
FILES_TO_CHECK=(
    "api/src/main/java/net/blufenix/smtshell/api/SMTShellAPI.java"
    "api/src/main/AndroidManifest.xml"
    "api/build.gradle"
    "app/src/main/java/com/example/smtshellapp/MainActivity.java"
    "app/src/main/AndroidManifest.xml"
    "app/build.gradle"
    ".github/workflows/build.yml"
    "README.md"
)

for file in "${FILES_TO_CHECK[@]}"; do
    if [[ ! -f "$file" ]]; then
        echo "❌ Missing file: $file"
        exit 1
    fi
    echo "✅ Found: $file"
done

echo "🔍 Checking API implementation..."

# Verify key methods exist in SMTShellAPI
API_METHODS=(
    "isRootAvailable"
    "requestRootAccess"
    "executeCommand"
    "loadLibrary"
    "RootAccessCallback"
)

API_FILE="api/src/main/java/net/blufenix/smtshell/api/SMTShellAPI.java"
for method in "${API_METHODS[@]}"; do
    if grep -q "$method" "$API_FILE"; then
        echo "✅ Found method/interface: $method"
    else
        echo "❌ Missing method/interface: $method"
        exit 1
    fi
done

# Check if root permissions are in manifest
if grep -q "android.permission.ACCESS_SUPERUSER" "api/src/main/AndroidManifest.xml"; then
    echo "✅ Root permission found in API manifest"
else
    echo "❌ Missing root permission in API manifest"
    exit 1
fi

if grep -q "android.permission.ACCESS_SUPERUSER" "app/src/main/AndroidManifest.xml"; then
    echo "✅ Root permission found in app manifest"
else
    echo "❌ Missing root permission in app manifest"
    exit 1
fi

echo "🔧 Making gradlew executable..."
chmod +x gradlew

echo "🏗️ Starting build process..."

# Clean build
echo "📁 Cleaning project..."
./gradlew clean

# Build API library
echo "📚 Building API library..."
./gradlew :api:build

# Build sample app
echo "📱 Building sample app..."
./gradlew :app:build

# Create release APKs
echo "📦 Creating release APK..."
./gradlew assembleRelease

# Create debug APKs
echo "🐛 Creating debug APK..."
./gradlew assembleDebug

echo "📊 Build summary:"
echo "===================="

# List output files
if [[ -d "app/build/outputs/apk/debug" ]]; then
    echo "Debug APK(s):"
    ls -la app/build/outputs/apk/debug/*.apk
fi

if [[ -d "app/build/outputs/apk/release" ]]; then
    echo "Release APK(s):"
    ls -la app/build/outputs/apk/release/*.apk
fi

if [[ -d "api/build/outputs/aar" ]]; then
    echo "Library AAR(s):"
    ls -la api/build/outputs/aar/*.aar
fi

echo "✅ Build completed successfully!"
echo ""
echo "🎉 SMTShell-API has been successfully converted to use direct root access!"
echo ""
echo "Key changes made:"
echo "  - Removed Samsung SMTShell service dependency"
echo "  - Implemented direct 'su' command execution"
echo "  - Added root permission checking and requesting"
echo "  - Created sample app demonstrating usage"
echo "  - Added GitHub Actions workflow for CI/CD"
echo "  - Updated documentation with migration guide"
echo ""
echo "To test on device:"
echo "  1. Install the debug APK on a rooted device"
echo "  2. Launch the app"
echo "  3. Tap 'Request Root Access' and grant permission"
echo "  4. Tap 'Execute Test Command' to verify functionality"

# Verification script for key functionality
cat > verify_implementation.java << 'EOF'
// Quick verification of API surface
import net.blufenix.smtshell.api.SMTShellAPI;

public class VerifyImplementation {
    public static void main(String[] args) {
        System.out.println("Verifying SMTShellAPI implementation...");
        
        // These should compile without error
        boolean rootAvailable = SMTShellAPI.isRootAvailable();
        SMTShellAPI.requestRootAccess(null, new SMTShellAPI.RootAccessCallback() {
            @Override
            public void onResult(boolean granted) {
                System.out.println("Root access: " + granted);
            }
        });
        
        SMTShellAPI.executeCommand(null, "echo test", new SMTShellAPI.CommandCallback() {
            @Override
            public void onComplete(String stdout, String stderr, int exitCode) {
                System.out.println("Command executed");
            }
        });
        
        SMTShellAPI.loadLibrary(null, "/path/to/lib.so", new SMTShellAPI.LoadLibraryCallback() {
            @Override
            public void onComplete(boolean success) {
                System.out.println("Library loaded: " + success);
            }
        });
        
        System.out.println("✅ API surface verification complete!");
    }
}
EOF

echo "📝 Created verification file: verify_implementation.java"
echo ""
echo "Run './test_build.sh' to execute this validation script"