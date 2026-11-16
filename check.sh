#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
mkdir -p out/main out/test
find src/main/java -name '*.java' -print > out/main-sources.txt
find src/test/java -name '*.java' -print > out/test-sources.txt
javac --release 21 -encoding UTF-8 -Xlint:all -d out/main @out/main-sources.txt
javac --release 21 -encoding UTF-8 -Xlint:all -cp out/main -d out/test @out/test-sources.txt
java -cp out/main:out/test org.qatracker.QaTrackerTests
