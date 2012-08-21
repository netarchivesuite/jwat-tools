@echo off
setlocal enableextensions

call %~dp0\env.cmd

if "%JAVA_OPTS%" == "" (
   set JAVA_OPTS=-Xms256m -Xmx1024m -XX:PermSize=64M -XX:MaxPermSize=256M
)

%JAVA% %JAVA_OPTS% -cp "%CP%" org.jwat.tools.JWATTools %*
