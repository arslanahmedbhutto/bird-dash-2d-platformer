@echo off
rem Compiles the game and packages a runnable jar (Windows).
rem Usage: build.bat   (then, from this folder: java -jar dist\bird-dash.jar)
setlocal
cd /d "%~dp0"
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build dist
javac -d build src\*.java src\game2D\*.java src\GameClasses\*.java || exit /b 1
jar --create --file dist\bird-dash.jar --main-class Game -C build .
echo Done. Run it from this folder: java -jar dist\bird-dash.jar
