#!/usr/bin/env bash
# Compiles the game and packages a runnable jar.
# Usage: ./build.sh            (then, from this folder: java -jar dist/bird-dash.jar)
set -euo pipefail
cd "$(dirname "$0")"

rm -rf build dist
mkdir -p build dist
javac -d build $(find src -name "*.java")
jar --create --file dist/bird-dash.jar --main-class Game -C build .
echo "Done. Run it from this folder (it loads images/, maps/ and sounds/): java -jar dist/bird-dash.jar"
