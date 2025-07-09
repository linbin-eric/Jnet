# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Jnet is a Java NIO networking library that provides high-performance asynchronous I/O operations. It's built around a pipeline-based architecture for processing network data with pooled buffer management.

## Development Commands

### Building and Testing
- **Build**: `mvn compile` - Compiles the project
- **Test**: `mvn test` - Runs all unit tests
- **Run specific test**: `mvn test -Dtest=ClassName` (e.g., `mvn test -Dtest=BaseTest`)
- **Package**: `mvn package` - Creates JAR file
- **Clean**: `mvn clean` - Removes target directory

### Running Performance Tests
The project includes JMH (Java Microbenchmark Harness) for performance testing:
- Performance tests are located in `src/test/java/com/jfirer/jnet/common/buffer/`
- Key benchmark classes: `BenchTest`, `BenchRwTest`, `BenchCache`

## Architecture

### Core Components

**Pipeline System** (`com.jfirer.jnet.common.api.Pipeline`):
- Central abstraction for processing network data
- Supports chained read/write processors
- Thread-safe with worker-based execution model
- Implementation: `DefaultPipeline` in `com.jfirer.jnet.common.internal`

**Buffer Management** (`com.jfirer.jnet.common.buffer`):
- **Arena**: Memory arena with chunk-based allocation using buddy algorithm
- **BufferAllocator**: Interface for buffer allocation (pooled vs unpooled)
- **IoBuffer**: Primary buffer abstraction with read/write operations
- **ChunkList**: Manages memory chunks with different usage thresholds (c000, c025, c050, c075, c100, cInt)

**Server/Client** (`com.jfirer.jnet.server`, `com.jfirer.jnet.client`):
- **AioServer**: Asynchronous server implementation using Java NIO.2
- **ClientChannel**: Client-side connection management
- Both use `ChannelConfig` for configuration

**Decoders** (`com.jfirer.jnet.common.decoder`):
- Frame-based decoders for protocol handling
- `TotalLengthFieldBasedFrameDecoder`: Length-prefixed frame decoder
- `HeartBeatDecoder`, `FixLengthDecoder`: Specialized decoders

### Key Patterns

**Worker Threading Model**:
- Uses `WorkerGroup` for thread management
- Each pipeline bound to specific worker thread to avoid concurrency
- Read operations are non-concurrent (AIO guarantees)
- Write operations use thread binding for concurrency safety

**Memory Management**:
- Sophisticated pooled buffer system with multiple chunk lists
- Different usage thresholds: cInt (2-25%), c000 (2-50%), c025 (25-75%), etc.
- SubPages for small allocations (<pageSize)
- Direct memory support via Unsafe operations

**Pipeline Processing**:
- Chain of responsibility pattern for read/write processors
- Each processor can transform data or trigger side effects
- Pipeline completion callbacks for initialization

## Project Structure

- `src/main/java/com/jfirer/jnet/common/`: Core networking and buffer management
- `src/main/java/com/jfirer/jnet/server/`: Server implementation
- `src/main/java/com/jfirer/jnet/client/`: Client implementation  
- `src/main/java/com/jfirer/jnet/extend/`: Extensions (HTTP client/server)
- `src/test/java/com/jfirer/jnet/`: Test suites and benchmarks

## Testing Strategy

**BaseTest**: Comprehensive integration test that:
- Creates server with echo processor
- Spawns multiple clients sending numbered messages
- Verifies all messages are received correctly
- Tests under high concurrency (8 clients, 60M+ messages each)

**Buffer Tests**: Extensive testing of memory allocation:
- `ArenaTest`, `ChunkListTest`: Arena and chunk management
- `PooledBufferRWTest`, `UnPooledBufferRWTest`: Buffer read/write operations
- `ReAllocateTest`, `SliceBufferTest`: Buffer manipulation

## Dependencies

- **Runtime**: baseutil (custom utility library), Java 21+
- **Build**: Lombok for code generation  
- **Testing**: JUnit 4, JMH for benchmarking
- **Logging**: SLF4J with Log4j2 implementation