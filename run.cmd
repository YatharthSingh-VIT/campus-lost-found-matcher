@echo off
setlocal
pushd "%~dp0"
javac -encoding UTF-8 --release 21 -d out @sources.txt
if errorlevel 1 (popd & exit /b 1)
java -Dfile.encoding=UTF-8 -cp out com.lostfound.app.Main %*
set "result=%errorlevel%"
popd
exit /b %result%
