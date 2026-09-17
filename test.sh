#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
javac -encoding UTF-8 --release 21 -d out @sources.txt
javac -encoding UTF-8 --release 21 -cp out -d test-out test/ProjectTest.java
java -cp "out:test-out" com.lostfound.ProjectTest
