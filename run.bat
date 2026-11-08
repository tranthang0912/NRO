@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
for /d %%J in ("%ProgramFiles%\Java\jdk-21*") do set "JAVA_HOME=%%~fJ"
set "JAVA_BIN=java"
if defined JAVA_HOME set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
if exist "nro-web.jar" (
  "%JAVA_BIN%" -Xms64m -Xmx384m -Djava.awt.headless=true -Dfile.encoding=UTF-8 -jar nro-web.jar
  goto :end
)
if not exist "target\nro-web.jar" (
  call mvn -B package
  if errorlevel 1 goto :end
)
"%JAVA_BIN%" -Xms64m -Xmx384m -Djava.awt.headless=true -Dfile.encoding=UTF-8 -jar target\nro-web.jar
:end
pause
endlocal
