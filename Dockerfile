LABEL authors="mahes"

ENTRYPOINT ["top", "-b"]

# Start with a base image that has both Java and Rust
FROM eclipse-temurin:17-jdk AS builder

# Install Rust and Cargo
RUN apt-get update && apt-get install -y \
    curl \
    build-essential \
    && rm -rf /var/lib/apt/lists/* \
    && curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y

# Add Cargo to PATH
ENV PATH="/root/.cargo/bin:${PATH}"

# Set the working directory
WORKDIR /majorapplication

# Copy the entire project
COPY . .

# First build the Rust library
WORKDIR /app/rustbulletproof
RUN cargo build --release

# Return to app directory
WORKDIR /majorapplication

# Build the Spring Boot application
RUN ./gradlew build -x test

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built Spring Boot JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy the Rust executable
COPY --from=builder /app/rustbulletproof/target/release/* /usr/local/bin/

# Set the environment variable to point to the Rust executable
ENV RUST_EXECUTABLE_PATH="/usr/local/bin/your_rust_executable_name"

# Expose the Spring Boot port
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]

