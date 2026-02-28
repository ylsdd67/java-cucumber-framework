@REM Maven Wrapper script for Windows
@REM Adapted from https://github.com/apache/maven-wrapper

@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"

@REM Read distribution URL from properties
for /f "tokens=1,* delims==" %%a in ('findstr "distributionUrl" "%WRAPPER_PROPERTIES%"') do set "MAVEN_DIST_URL=%%b"
if "%MAVEN_DIST_URL%"=="" set "MAVEN_DIST_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"

set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9"

if exist "%MAVEN_HOME%\bin\mvn.cmd" goto runMaven

echo Downloading Maven to %MAVEN_HOME%...
mkdir "%MAVEN_HOME%" 2>nul

@REM Use PowerShell to download and extract
powershell -Command ^
  "$url='%MAVEN_DIST_URL%'; " ^
  "$zip='%TEMP%\maven-dist.zip'; " ^
  "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; " ^
  "Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing; " ^
  "Expand-Archive -Path $zip -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force; " ^
  "Remove-Item $zip"

:runMaven
"%MAVEN_HOME%\bin\mvn.cmd" %*
