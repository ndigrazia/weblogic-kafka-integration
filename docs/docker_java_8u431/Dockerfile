# Stage 1: Builder
FROM debian:bullseye-slim AS builder

# Copy JDK from local disk to image
COPY jdk-8u431-linux-x64.tar.gz /tmp/jdk.tar.gz

# Install JDK
RUN mkdir -p /tmp/java \
    && tar -xzf /tmp/jdk.tar.gz -C /tmp/java 

# Stage 2: Final Image
# Use a minimal base image
FROM debian:bullseye-slim

# Set environment variables
ENV JAVA_HOME=/usr/local/java
ENV PATH="$JAVA_HOME/bin:$PATH"

# Install JDK
RUN mkdir -p /usr/local/java 

# Copy built application from the builder stage
COPY --from=builder /tmp/java/jdk1.8.0_431/jre /usr/local/java

# Verify Java installation
RUN java -version

# Set the working directory
WORKDIR /app

# Command to keep the container running
CMD ["bash"]
