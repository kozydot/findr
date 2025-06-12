@echo off
setlocal EnableDelayedExpansion

echo Compiling the project...
call mvnw.cmd compile
if %ERRORLEVEL% neq 0 (
    echo Compilation failed. Please check the errors above.
    exit /b %ERRORLEVEL%
)

echo Compiling the test classes...
call mvnw.cmd test-compile
if %ERRORLEVEL% neq 0 (
    echo Test compilation failed. Please check the errors above.
    exit /b %ERRORLEVEL%
)

echo Generating classpath...
call mvnw.cmd dependency:build-classpath -Dmdep.outputFile=classpath.txt
if %ERRORLEVEL% neq 0 (
    echo Failed to generate classpath. Please check the errors above.
    exit /b %ERRORLEVEL%
)

echo Copying test dependencies...
call mvnw.cmd dependency:copy-dependencies -DoutputDirectory=target\dependency
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
set FULL_CLASSPATH=target\classes;target\test-classes;target\dependency\*;%CLASSPATH%

echo Running ScrapingServiceTest...
java -cp "%FULL_CLASSPATH%" org.junit.platform.console.ConsoleLauncher --select-class com.example.price_comparator.service.ScrapingServiceTest

if %ERRORLEVEL% neq 0 (
    echo Test execution failed. Please check the errors above.
    exit /b %ERRORLEVEL%
)

echo Test execution completed.
endlocal
