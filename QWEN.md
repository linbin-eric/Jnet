# JNet Project Context

This document provides an overview of the JNet project, an asynchronous network framework implemented in Java.

## Project Overview

JNet is a Java-based asynchronous network communication framework built on top of Java NIO (Non-blocking I/O) and AIO (Asynchronous I/O). It provides interfaces and implementations for creating high-performance TCP clients and servers. The core concepts include:

- **AioServer / AioClient**: Interfaces for starting and managing server/client connections.
- **Pipeline**: A core component representing a connection channel, handling data flow and processing.
- **ReadProcessor / WriteProcessor**: Components that can be added to a Pipeline to handle incoming and outgoing data transformations or actions.
- **ChannelConfig**: Configuration object for network channel parameters.

The project also includes an extension for building a reverse proxy server (`ReverseProxyServer`) with features like resource serving and HTTP request proxying based on URL patterns, configurable via a YAML file (`reverse.config`).

## Key Technologies

- **Language**: Java 21
- **Build Tool**: Apache Maven
- **Dependencies**:
  - `com.jfirer:baseutil`: A base utility library.
  - `org.projectlombok:lombok`: For reducing boilerplate code (used at compile time).
  - `org.apache.logging.log4j:log4j-slf4j2-impl`: For logging.
  - Testing: `junit` and `JMH` (Java Microbenchmark Harness).

## Architecture

The project is structured into main packages under `src/main/java/com/jfirer/jnet`:
- `client`: Contains interfaces and implementations for client-side connections (`ClientChannel`).
- `server`: Contains interfaces and implementations for server-side connections (`AioServer`, `DefaultAioServer`).
- `common`: Houses core APIs (`Pipeline`, `ReadProcessor`, etc.) and utilities.
- `extend`: Contains extensions, notably the `reverse` package for the `ReverseProxyServer`.

## Building and Running

The project uses Maven for building.

- **Build**: `mvn clean package`. This will compile the code and create artifacts, including a fat JAR for the `ReverseApp` (`ReverseApp.jar`) as configured in `pom.xml`.
- **Run Reverse Proxy App**: After building, execute the generated JAR: `java -jar target/ReverseApp.jar`. This will start the reverse proxy server as defined in `reverse.config`.

## Development Conventions

- Java 21 features are used.
- Lombok annotations are leveraged to minimize boilerplate (e.g., getters, setters, loggers).
- Code structure follows standard Maven conventions (`src/main/java`, `src/test/java`).
- Asynchronous I/O operations are central to the design for scalability.