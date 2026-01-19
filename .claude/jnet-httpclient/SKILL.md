---
name: jnet-httpclient
description: Guide for using JNet HttpClient API to create HTTP clients, send requests (GET/POST/PUT/DELETE), configure connection pools and proxies, handle responses (sync/streaming), and manage resources. Use when working with JNet framework's HTTP client functionality, implementing HTTP communication, configuring advanced options like SSL/timeouts/proxies, or handling streaming responses.
---

# JNet HttpClient

## Overview

Use this skill when working with JNet's HttpClient API to send HTTP/HTTPS requests, configure connection pools, handle responses, or implement streaming communication.

## Quick Start

### Basic Usage Pattern

```java
HttpClient client = new HttpClient();

try (HttpRequest request = new HttpRequest()
        .setUrl("https://api.example.com/data")
        .get()) {
    try (HttpResponse response = client.call(request)) {
        // Process response
    }
}
```

**Key points:**
- Always use try-with-resources for `HttpRequest` and `HttpResponse`
- JNet auto-detects HTTPS from URL and enables SSL
- Connection pool is managed automatically

## Core Workflows

### 1. Simple HTTP Requests

**GET request:**
```java
HttpClient client = new HttpClient();

try (HttpRequest request = new HttpRequest()
        .setUrl("https://api.example.com/users/1")
        .get()
        .addHeader("Authorization", "Bearer token")) {

    try (HttpResponse response = client.call(request)) {
        IoBuffer bodyBuffer = response.getBodyBuffer();
        String text = StandardCharsets.UTF_8
            .decode(bodyBuffer.readableByteBuffer())
            .toString();
    }
}
```

**POST request:**
```java
try (HttpRequest request = new HttpRequest()
        .setUrl("https://api.example.com/users")
        .post()
        .setContentType("application/json")
        .setBody("{\"name\":\"John\"}")) {

    try (HttpResponse response = client.call(request)) {
        int statusCode = response.getHead().getStatusCode();
    }
}
```

### 2. Configured Client with Connection Pool

```java
HttpClientConfig config = new HttpClientConfig()
    .setConnectTimeoutSeconds(15)
    .setReadTimeoutSeconds(120)
    .setMaxConnectionsPerHost(100)
    .setKeepAliveSeconds(3600);

HttpClient client = new HttpClient(config);
```

### 3. Proxy Configuration

```java
HttpClientConfig config = new HttpClientConfig()
    .setProxyHost("127.0.0.1")
    .setProxyPort(7890);

HttpClient client = new HttpClient(config);
// All requests go through proxy
```

### 4. Streaming Responses

```java
client.streamCall(
    request,
    // Handle each chunk
    part -> {
        if (part instanceof HttpResponsePartHead) {
            HttpResponsePartHead head = (HttpResponsePartHead) part;
            System.out.println("Status: " + head.getStatusCode());
        } else if (part instanceof HttpResponseChunkedBodyPart) {
            HttpResponseChunkedBodyPart bodyPart = (HttpResponseChunkedBodyPart) part;
            // Process chunk
        }
        part.free();
    },
    // Handle errors
    error -> error.printStackTrace()
);
```

## Key Classes

- **HttpClient** - Main client class, manages connection pool
- **HttpClientConfig** - Configuration (timeouts, proxy, pool size)
- **HttpRequest** - Request builder (URL, method, headers, body)
- **HttpResponse** - Response container (status, headers, body)
- **HttpResponsePart** - Streaming response fragment

## Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| connectTimeoutSeconds | 10 | Connection timeout |
| readTimeoutSeconds | 60 | Read timeout |
| keepAliveSeconds | 1800 | Keep-Alive duration |
| maxConnectionsPerHost | 50 | Max connections per host |
| acquireTimeoutSeconds | 1 | Connection acquisition timeout |
| sslHandshakeTimeoutSeconds | 30 | SSL handshake timeout |
| proxyHost | null | Proxy server host |
| proxyPort | 0 | Proxy server port |

## Resource Management

**Critical:** Always close resources to prevent memory leaks:

```java
// Recommended: try-with-resources
try (HttpRequest request = new HttpRequest().setUrl(url).get()) {
    try (HttpResponse response = client.call(request)) {
        // Process response
    }
}

// Manual: call close() in finally
HttpRequest request = null;
try {
    request = new HttpRequest().setUrl(url).get();
    HttpResponse response = client.call(request);
    try {
        // Process
    } finally {
        response.close();
    }
} finally {
    if (request != null) request.close();
}
```

## Common Patterns

### RESTful API Client

See `assets/configured-client.java` for a complete RESTful client template with GET/POST/PUT/DELETE methods.

### Streaming (SSE, ChatGPT-style)

See `assets/streaming-client.java` for handling Server-Sent Events and chunked responses.

### Batch Requests

Use virtual threads for concurrent requests:

```java
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
List<CompletableFuture<HttpResponse>> futures = urls.stream()
    .map(url -> CompletableFuture.supplyAsync(() -> {
        try (HttpRequest req = new HttpRequest().setUrl(url).get()) {
            return client.call(req);
        }
    }, executor))
    .toList();
```

## Resources

### references/
- **api-reference.md** - Detailed API documentation for all classes and methods
- **examples.md** - Comprehensive examples covering all use cases

### assets/
- **basic-http-client.java** - Simple GET/POST template
- **configured-client.java** - RESTful client with connection pool and error handling
- **streaming-client.java** - Streaming response handler for SSE and chunked transfers

## Best Practices

1. **Reuse HttpClient instances** - Each instance maintains a connection pool
2. **Always close resources** - Use try-with-resources for HttpRequest/HttpResponse
3. **Configure timeouts** - Set appropriate timeouts for your use case
4. **Use connection pooling** - Configure `maxConnectionsPerHost` for high-throughput scenarios
5. **Handle errors properly** - Check status codes and handle exceptions
6. **Free IoBuffer** - Response body buffers must be freed (automatic with `response.close()`)

## When to Read References

- **api-reference.md** - When you need detailed method signatures, parameters, or configuration options
- **examples.md** - When implementing specific scenarios (file download, batch requests, etc.)

## Quick Reference

```java
// Create client
HttpClient client = new HttpClient();
HttpClient client = new HttpClient(config);

// Build request
new HttpRequest()
    .setUrl(url)
    .get() / .post() / .put() / .delete()
    .addHeader(name, value)
    .setContentType(type)
    .setBody(body)

// Send request
HttpResponse response = client.call(request);
StreamableResponseFuture future = client.streamCall(request, partConsumer, errorConsumer);

// Read response
int status = response.getHead().getStatusCode();
IoBuffer body = response.getBodyBuffer();
String text = StandardCharsets.UTF_8.decode(body.readableByteBuffer()).toString();
```
