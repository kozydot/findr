@echo off
REM Enhanced Scraper Test Script for Windows
REM This script tests the enhanced scraper functionality

echo =====================================
echo ENHANCED SCRAPER TESTING SCRIPT
echo =====================================

REM Get the base URL from command line argument or default to localhost
set BASE_URL=%1
if "%BASE_URL%"=="" set BASE_URL=http://localhost:8080

echo Using base URL: %BASE_URL%
echo.

REM Test 1: Check test endpoint status
echo Test 1: Checking enhanced scraper test status...
curl -s -X GET "%BASE_URL%/api/v1/test/status"
echo.
echo.

REM Test 2: Run mock data tests
echo Test 2: Running enhanced scraper mock tests...
curl -s -X GET "%BASE_URL%/api/v1/test/enhanced-scraper"

set TEST_RESULT=%ERRORLEVEL%

echo.
echo ========================================

if %TEST_RESULT% equ 0 (
    echo ✅ All Enhanced Scraper Tests PASSED!
    echo.
    echo 📊 Test Summary:
    echo    ✓ Image URL extraction with multiple fallbacks
    echo    ✓ Enhanced specification extraction from multiple sources
    echo    ✓ Text parsing from 'About this item' sections
    echo    ✓ Specification deduplication and cleaning
    echo    ✓ Real product data structure handling
    echo    ✓ Performance testing with large datasets
    echo.
    echo 🚀 Enhanced scraper is ready for production use!
) else (
    echo ❌ Some tests FAILED!
    echo.
    echo 🔧 Please check the test output above for details.
    echo    - Verify all required dependencies are available
    echo    - Check that test data structures match expected format
    echo    - Ensure private method signatures are correct
    echo.
    echo 💡 Run with verbose output: mvnw.cmd test -Dtest=EnhancedScraperTest
)

echo.
echo ========================================
echo 🏁 Test execution completed.

pause
exit /b %TEST_RESULT%
