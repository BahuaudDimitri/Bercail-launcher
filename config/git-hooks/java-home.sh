# Sourced by the hooks: Gradle needs a JDK. Falls back to the one shipped with Android Studio on Windows.
if [ -z "$JAVA_HOME" ] && [ -d "/c/Program Files/Android/Android Studio/jbr" ]; then
    export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
fi
