#!/bin/sh
# Maven Wrapper Startup Script
# This is a minimal wrapper for bootstrapping Maven

MAVEN_PROJECT_BASEDIR=$(cd "$(dirname "$0")/.." && pwd)
MAVEN_WRAPPER_JAR="$MAVEN_PROJECT_BASEDIR/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECT_BASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
    echo "Downloading Maven wrapper..."
    mkdir -p "$MAVEN_PROJECT_BASEDIR/.mvn/wrapper"
    curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar" -o "$MAVEN_WRAPPER_JAR"
fi

exec java -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECT_BASEDIR" -cp "$MAVEN_WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"