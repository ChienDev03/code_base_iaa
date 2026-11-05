#!/bin/sh
# Use an explicit JDK, or the Android Studio runtime on macOS, without changing gradlew.
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
if [ -z "${JAVA_HOME:-}" ] && [ -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" ]; then
    JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    export JAVA_HOME
fi
exec "$project_dir/gradlew" -p "$project_dir" "$@"
