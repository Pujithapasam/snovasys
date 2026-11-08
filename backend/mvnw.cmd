@REM ----------------------------------------------------------------------------
@REM Maven Wrapper script for Windows
@REM ----------------------------------------------------------------------------
@IF "%DEBUG%" == "1" (@ECHO ON) ELSE (@ECHO OFF)
@setlocal

set ERROR_CODE=0

@REM Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%" == "" goto OkJHome

for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"
goto checkJCmd

:OkJHome
set "JAVACMD=%JAVA_HOME%\bin\java.exe"

:checkJCmd
if exist "%JAVACMD%" goto chkMHome

echo The JAVA_HOME environment variable is not defined correctly >&2
echo This environment variable is needed to run this program >&2
goto error

:chkMHome
set "DIRNAME=%~dp0%"
if "%DIRNAME%" == "" set "DIRNAME=."
set "MAVEN_PROJECTBASEDIR=%DIRNAME%"

@REM Fallback if mvn is installed locally
where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 (
    mvn %*
    goto end
)

echo [INFO] Running Maven build...
if exist "%DIRNAME%\.mvn\wrapper\maven-wrapper.jar" (
    "%JAVACMD%" -jar "%DIRNAME%\.mvn\wrapper\maven-wrapper.jar" %*
) else (
    echo [INFO] Direct Maven fallback or install Apache Maven to run.
)

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%
exit /B %ERROR_CODE%

