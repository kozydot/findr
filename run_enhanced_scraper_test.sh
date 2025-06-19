#!/bin/bash

# Enhanced Scraper Test Runner
# This script runs the enhanced scraper tests and validates the functionality

echo "🔍 Enhanced Shopping Scraper Test Suite"
echo "========================================"
echo ""

# Set test environment
export SPRING_PROFILES_ACTIVE=test

echo "📋 Running Enhanced Scraper Tests..."
echo ""

# Change to the price-comparator directory
cd price-comparator

# Run the specific test class
echo "🧪 Executing EnhancedScraperTest..."
./mvnw test -Dtest=EnhancedScraperTest -q

TEST_RESULT=$?

echo ""
echo "========================================"

if [ $TEST_RESULT -eq 0 ]; then
    echo "✅ All Enhanced Scraper Tests PASSED!"
    echo ""
    echo "📊 Test Summary:"
    echo "   ✓ Image URL extraction with multiple fallbacks"
    echo "   ✓ Enhanced specification extraction from multiple sources"
    echo "   ✓ Text parsing from 'About this item' sections"
    echo "   ✓ Specification deduplication and cleaning"
    echo "   ✓ Real product data structure handling"
    echo "   ✓ Performance testing with large datasets"
    echo ""
    echo "🚀 Enhanced scraper is ready for production use!"
else
    echo "❌ Some tests FAILED!"
    echo ""
    echo "🔧 Please check the test output above for details."
    echo "   - Verify all required dependencies are available"
    echo "   - Check that test data structures match expected format"
    echo "   - Ensure private method signatures are correct"
    echo ""
    echo "💡 Run with verbose output: ./mvnw test -Dtest=EnhancedScraperTest"
fi

echo ""
echo "========================================"
echo "🏁 Test execution completed."

exit $TEST_RESULT
