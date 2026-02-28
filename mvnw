#!/bin/sh
# Maven Wrapper script for Unix/macOS
# Adapted from https://github.com/apache/maven-wrapper

set -e

# Determine the project base directory
if [ -z "$MAVEN_PROJECTBASEDIR" ]; then
  MAVEN_PROJECTBASEDIR=$(cd "$(dirname "$0")" && pwd)
  export MAVEN_PROJECTBASEDIR
fi

WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ -r "$WRAPPER_PROPERTIES" ]; then
  MAVEN_DIST_URL=$(grep 'distributionUrl' "$WRAPPER_PROPERTIES" | cut -d'=' -f2-)
fi

# Default Maven version
: "${MAVEN_DIST_URL:=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip}"

MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-3.9.9"

if [ ! -d "$MAVEN_HOME" ]; then
  echo "Downloading Maven to $MAVEN_HOME..."
  mkdir -p "$MAVEN_HOME"
  TMPFILE=$(mktemp)
  curl -fsSL "$MAVEN_DIST_URL" -o "$TMPFILE"
  unzip -qo "$TMPFILE" -d "$HOME/.m2/wrapper/dists/"
  rm -f "$TMPFILE"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
