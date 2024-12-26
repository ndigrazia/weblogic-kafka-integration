FROM ndigrazia/java:1.8.0_431

# Define default environment variables
ENV APP_HOME=/opt/telefonica/containerize

# Name of the executable artifact
ENV ARTIFACT_NAME=weblogic-kafka-integration-*.jar

# Java debug port
ENV DEBUG_PORT=8000

# Copy all artifacts to the home directory
# In maven/ the files as specified in the <assembly> section is stored
# and need to be added manually
COPY /maven/docker-entrypoint.sh /
COPY /maven/$ARTIFACT_NAME ${APP_HOME}/

# Give permissions
USER root
RUN chmod 755 /docker-entrypoint.sh

# setting permission to 777 for write access to directory
RUN chmod -R 777 /opt/telefonica

# set work directory
WORKDIR /opt/telefonica

# Main command
USER 185
ENTRYPOINT ["/docker-entrypoint.sh"]