#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
mkdir -p out/main
find src/main/java -name '*.java' -print > out/main-sources.txt
javac --release 21 -encoding UTF-8 -Xlint:all -d out/main @out/main-sources.txt
java -cp out/main org.qatracker.Main "$@"
