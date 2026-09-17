#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
javac -encoding UTF-8 --release 21 -d out @sources.txt
java -Dfile.encoding=UTF-8 -cp out com.lostfound.app.Main "$@"
