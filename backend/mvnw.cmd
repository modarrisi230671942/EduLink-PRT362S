@REM ============================================================
@REM EduLink Maven Wrapper — Windows
@REM Double-click this file OR run it in a terminal to launch
@REM the EduLink desktop application.
@REM ============================================================
@echo off
setlocal

SET "SCRIPT_DIR=%~dp0"
SET "MVN_HOME=%SCRIPT_DIR%maven\apache-maven-3.9.6"
SET "MVN_EXE=%MVN_HOME%\bin\mvn.cmd"

IF NOT EXIST "%MVN_EXE%" (
    echo [EduLink] Bundled Maven not found. Trying system Maven...
    SET "MVN_EXE=mvn"
)

echo [EduLink] Starting EduLink desktop application...
"%MVN_EXE%" spring-boot:run -f "%SCRIPT_DIR%pom.xml"

endlocal
