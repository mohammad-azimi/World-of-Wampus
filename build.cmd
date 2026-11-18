@echo off
setlocal
pushd "%~dp0" || exit /b 1
if not exist build\app mkdir build\app
javac --release 17 -encoding UTF-8 -d build\app src\wampus\*.java
if errorlevel 1 (
    popd
    exit /b 1
)
jar --create --file WampusWorld.jar --main-class wampus.WampusApp -C build\app .
set "WAMPUS_BUILD_STATUS=%errorlevel%"
popd
exit /b %WAMPUS_BUILD_STATUS%
