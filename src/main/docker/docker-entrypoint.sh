#!/bin/sh
set -e

echo 'Starting containerize Spring Boot App'

if [ "$DEBUG" = true ]; then
  printf "Running the application in debug mode\n"
  JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT"
fi

# Enables application to take PID 1 and receive SIGTERM sent by Docker stop command.
# See here https://docs.docker.com/engine/reference/builder/#/entrypoint
exec java $JAVA_OPTS \
    --add-opens java.base/java.io=ALL-UNNAMED -jar \
       ${APP_HOME}/$ARTIFACT_NAME

# keep the container running until interrupted
sleep infinity