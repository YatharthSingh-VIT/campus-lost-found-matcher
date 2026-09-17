@echo off
setlocal
pushd "%~dp0"
javac -encoding UTF-8 --release 21 -d out @sources.txt
if errorlevel 1 (popd & exit /b 1)
javac -encoding UTF-8 --release 21 -cp out -d test-out test/ProjectTest.java
if errorlevel 1 (popd & exit /b 1)
java -cp "out;test-out" com.lostfound.ProjectTest
set "result=%errorlevel%"
popd
exit /b %result%
