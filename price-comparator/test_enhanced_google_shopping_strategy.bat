@echo off
echo ========================================
echo TESTING ENHANCED GOOGLE SHOPPING STRATEGY
echo ========================================
echo.
echo This test will verify the new strategy:
echo 1. Find retailer products with initial search
echo 2. Find associated Google Shopping URLs for each retailer
echo 3. Fetch detailed specs from Google Shopping pages
echo 4. Enhance product matching with detailed specifications
echo.
echo Running enhanced scraper test...
echo.

cd /d "C:\Users\Kozy\Documents\princecomparison_dom\price-comparator"

mvn clean compile exec:java -Dexec.mainClass="com.example.price_comparator.service.GoogleShoppingStrategyTest" -Dexec.args="" -q
