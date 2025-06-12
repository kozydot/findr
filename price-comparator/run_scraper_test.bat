@echo off
setlocal EnableDelayedExpansion

echo Compiling the project...
call mvnw.cmd compile
if %ERRORLEVEL% neq 0 (
    echo Compilation failed. Please check the errors above.
    exit /b %ERRORLEVEL%
)

echo Generating classpath...
call mvnw.cmd dependency:build-classpath -Dmdep.outputFile=classpath.txt
if %ERRORLEVEL% neq 0 (
    echo Failed to generate classpath. Please check the errors above.
    exit /b %ERRORLEVEL%
)

echo Cleaning old dependencies...
if exist price-comparator\target\dependency rmdir /S /Q price-comparator\target\dependency
if %ERRORLEVEL% neq 0 (
    echo Failed to clean old dependencies. Please check the errors above.
    exit /b %ERRORLEVEL%
)

echo Copying dependencies...
call mvnw.cmd dependency:copy-dependencies -DoutputDirectory=target\dependency -Dmdep.overWriteReleases=true -Dmdep.overWriteSnapshots=true
if %ERRORLEVEL% neq 0 (
    echo Failed to copy dependencies. Please check the errors above.
    exit /b %ERRORLEVEL%
)

echo Reading classpath...
set /p CLASSPATH=<classpath.txt
if not defined CLASSPATH (
    echo Failed to read classpath from file.
    exit /b 1
)

echo Setting full classpath...
set FULL_CLASSPATH=%CD%\price-comparator\target\classes;%CD%\price-comparator\target\test-classes;%CD%\price-comparator\target\dependency\jsoup-1.17.2.jar;%CD%\price-comparator\target\dependency\*;%CLASSPATH%

echo Running ScraperTestRunner...
java -cp "%FULL_CLASSPATH%" com.example.price_comparator.ScraperTestRunner

if %ERRORLEVEL% neq 0 (
    echo Test execution failed. Please check the errors above.
    exit /b %ERRORLEVEL%
)

echo Test execution completed.
endlocal
