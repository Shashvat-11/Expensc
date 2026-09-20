@echo off
setlocal
set "MAVEN_HOME=C:\Users\shashvat gupta\maven\apache-maven-3.9.9"
set "PATH=%MAVEN_HOME%\bin;%PATH%"
call mvn %*
