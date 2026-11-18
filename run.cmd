@echo off
setlocal
java -jar "%~dp0WampusWorld.jar"
if errorlevel 1 pause
