# Database Connection Pool

## 1. Project Overview

This project is a custom database connection pool implemented in Java.

The purpose of the project is both:

1. To build a reusable database connection pool.
2. To understand the underlying software engineering concepts involved in
   resource management, concurrency, synchronization, database connectivity,
   and lifecycle management.

The project will initially target PostgreSQL through JDBC.

---

# 2. Problem Statement

Creating a database connection can be relatively expensive compared to
executing many individual database operations.

If an application creates a new database connection for every operation,
the application repeatedly pays the cost of establishing and closing
connections.

For example:

    Request
       |
       v
    Create DB connection
       |
       v
    Execute query
       |
       v
    Close connection

Repeated many times, this can result in unnecessary overhead.

A connection pool maintains a collection of reusable database connections.

Instead of creating a connection for every database operation, an
application can borrow an existing connection from the pool and return it
after use.

The desired lifecycle is:

    Application
        |
        v
    Borrow connection
        |
        v
    Execute database operation
        |
        v
    Return connection
        |
        v
    Connection becomes available again

---

# 3. Goals

The project should eventually provide a thread-safe database connection
pool with the following capabilities:

- Create a configurable number of database connections.
- Maintain available database connections.
- Allow callers to borrow connections.
- Allow callers to return connections.
- Block callers when no connection is available.
- Support connection acquisition timeout.
- Validate connections before they are provided.
- Replace invalid connections.
- Track active and available connections.
- Shut down the pool gracefully.
- Prevent resource leaks.
- Support concurrent access from multiple threads.

---

# 4. Non-Goals

The first version will not attempt to implement every feature of a
production-grade connection pool.

The following are initially outside the scope:

- Dynamic pool resizing.
- Advanced connection eviction policies.
- Complex monitoring systems.
- Distributed connection pooling.
- Database-specific optimization.
- Transaction management.
- Query caching.
- SQL execution abstraction.
- ORM functionality.

These may be considered later if they are useful for learning or extending
the project.

---

# 5. Technology Stack

## Programming Language

Java

## Build System

Gradle

## Database

PostgreSQL

## Database Connectivity

JDBC

## Testing

JUnit

## Version Control

Git

---

# 6. High-Level Architecture

The initial architecture consists of the following components:

    Application
        |
        v
    ConnectionPool
        |
        v
    JDBC
        |
        v
    PostgreSQL

The ConnectionPool is responsible for managing the lifecycle of database
connections.

The application should not need to know how connections are created,
stored, reused, or managed internally.

---

# 7. Components

## 7.1 PoolConfig

Responsibility:

Store configuration required to create and operate the connection pool.

Expected configuration:

- JDBC URL
- database username
- database password
- pool size

Future configuration may include:

- connection acquisition timeout
- minimum pool size
- maximum pool size
- validation settings
- shutdown behavior

PoolConfig should not be responsible for creating database connections.

---

## 7.2 ConnectionPool

Responsibility:

Manage the lifecycle and availability of database connections.

Primary responsibilities:

- Create connections.
- Store available connections.
- Provide connections to callers.
- Receive returned connections.
- Validate connections.
- Replace invalid connections.
- Close connections during shutdown.

The ConnectionPool is the main component of the project.

---

## 7.3 Main

Main is currently used as a simple demonstration/integration entry point.

It should:

- create PoolConfig
- create ConnectionPool
- borrow a connection
- execute a simple database operation
- return the connection
- shut down the pool

Main is not part of the core pool implementation.

---

# 8. Connection Lifecycle

A connection can exist in several states.

Initial state:

    CREATED
       |
       v
    AVAILABLE

When borrowed:

    AVAILABLE
       |
       | borrow
       v
    IN_USE

When returned:

    IN_USE
       |
       | return
       v
    AVAILABLE

When invalid:

    IN_USE / AVAILABLE
          |
          | invalid
          v
       CLOSED

The pool may then create a replacement:

    CLOSED
       |
       v
    NEW CONNECTION
       |
       v
    AVAILABLE

During shutdown:

    AVAILABLE / IN_USE
          |
          v
       CLOSED

---

# 9. Connection Pool State

The pool conceptually contains two groups of connections:

    Connection Pool
    |
    +-- Available Connections
    |
    +-- Connections In Use

For example, if the pool size is 5:

    Total connections = 5

    Available:
        C1
        C2

    In Use:
        C3
        C4
        C5

Therefore:

    Available = 2
    Active = 3
    Total = 5

The implementation should maintain these states consistently.

---

# 10. Concurrency Model

Multiple application threads may access the connection pool
simultaneously.

Example:

    Thread 1 ----\
    Thread 2 -----\
    Thread 3 ------> ConnectionPool
    Thread 4 -----/
    Thread 5 ----/

The pool must therefore be thread-safe.

A shared collection of available connections must not allow two threads
to obtain the same connection simultaneously.

The initial implementation will use Java's BlockingQueue abstraction.

The queue provides thread-safe access and blocking behavior.

---

# 11. Borrowing a Connection

The basic operation is:

    borrowConnection()

Expected behavior:

1. If a connection is available, return it immediately.
2. Remove the connection from the available pool.
3. Mark the connection as in use.
4. If no connection is available, wait.
5. Later versions will support a configurable timeout.

Example:

    Pool size = 3

    Available:
        C1
        C2
        C3

    Thread A -> borrow()

    Available:
        C2
        C3

    In Use:
        C1

---

# 12. Returning a Connection

The basic operation is:

    returnConnection(connection)

Expected behavior:

1. Receive the connection from the caller.
2. Validate the connection.
3. If valid, make it available again.
4. If invalid, remove/discard it.
5. Optionally create a replacement.

Initial implementation may simply return the connection to the
available queue.

Validation and replacement will be added in later iterations.

---

# 13. Pool Exhaustion

Pool exhaustion occurs when every connection is currently in use.

Example:

    Pool size = 3

    C1 -> Thread A
    C2 -> Thread B
    C3 -> Thread C

    Available = 0

If Thread D requests a connection:

    Thread D
        |
        v
    borrowConnection()
        |
        v
    No connection available
        |
        v
    Wait

When another thread returns a connection:

    Thread A
        |
        v
    return(C1)
        |
        v
    Thread D can receive C1

The implementation must not create an unlimited number of connections.

---

# 14. Timeout

A later version will support connection acquisition timeout.

Example:

    borrowConnection(5 seconds)

If no connection becomes available within 5 seconds:

    Connection acquisition fails

The caller should receive an appropriate exception or failure result.

The timeout prevents a thread from waiting indefinitely.

---

# 15. Connection Validation

Connections may become invalid because of:

- database restart
- network failure
- server-side connection termination
- connection timeout
- other database/network failures

Before providing a connection to a caller, the pool should eventually
be able to determine whether the connection is still usable.

Conceptually:

    borrow
       |
       v
    validate
       |
       +------ valid ------> caller
       |
       +------ invalid ----> discard
                                |
                                v
                           create replacement

---

# 16. Shutdown

The pool must provide a shutdown operation.

Example:

    pool.shutdown()

Expected behavior:

1. Prevent new connection acquisition.
2. Handle connections currently in use.
3. Close available connections.
4. Close remaining resources.
5. Release pool resources.
6. Transition the pool to a closed state.

After shutdown, new borrow operations should not be allowed.

---

# 17. Error Handling

Potential failures include:

- database unavailable
- invalid database credentials
- connection creation failure
- connection validation failure
- interrupted waiting thread
- pool already shut down
- invalid connection returned to the pool

The implementation should fail explicitly rather than silently ignoring
these conditions.

---

# 18. Initial API

The initial public API is intentionally small.

    ConnectionPool(PoolConfig config)

    Connection borrowConnection()

    void returnConnection(Connection connection)

    void shutdown()

Future API:

    Connection borrowConnection(timeout)

    PoolMetrics getMetrics()

---

# 19. Development Iterations

The project will be developed incrementally.

## Iteration 1 — Basic Pool

Implement:

- PoolConfig
- ConnectionPool
- connection creation
- available connection queue
- borrow
- return
- shutdown

Status:

    IN PROGRESS

---

## Iteration 2 — Unit and Integration Tests

Tests should verify:

- pool creation
- configured pool size
- connection borrowing
- connection returning
- pool exhaustion
- connection reuse
- shutdown behavior

Status:

    NOT STARTED

---

## Iteration 3 — Concurrency

Test multiple threads using the pool simultaneously.

Verify:

- no connection is given to two threads simultaneously
- returned connections become available
- threads wait when the pool is exhausted
- no race conditions occur

Status:

    NOT STARTED

---

## Iteration 4 — Timeout

Add:

- configurable acquisition timeout
- timeout failure behavior

Status:

    NOT STARTED

---

## Iteration 5 — Connection Validation

Add:

- connection validation
- invalid connection removal
- replacement connection creation

Status:

    NOT STARTED

---

## Iteration 6 — Pool State and Metrics

Track:

- total connections
- active connections
- idle connections
- waiting threads
- connection acquisition time

Status:

    NOT STARTED

---

## Iteration 7 — Graceful Shutdown

Improve shutdown behavior.

Handle:

- active connections
- waiting threads
- new borrow requests
- repeated shutdown calls

Status:

    NOT STARTED

---

## Iteration 8 — Stress Testing

Simulate:

- many concurrent threads
- repeated borrow/return operations
- database failures
- pool exhaustion

Measure:

- throughput
- wait time
- failures
- connection reuse

Status:

    NOT STARTED

---

## Iteration 9 — Compare With Production Pool

Study an established Java connection pool implementation.

Compare:

- architecture
- concurrency model
- connection lifecycle
- timeout handling
- validation
- pool sizing
- resource management

The purpose is not to copy the implementation but to understand why
production systems use more sophisticated designs.

Status:

    NOT STARTED

---

# 20. Testing Strategy

Testing will happen at multiple levels.

## Unit Tests

Test individual components and behaviors without requiring a real
database where possible.

## Integration Tests

Test the pool against a real PostgreSQL database.

## Concurrency Tests

Use multiple Java threads to test simultaneous access.

## Stress Tests

Generate a large number of borrow/return operations and observe the
behavior of the pool.

---

# 21. Success Criteria

The project will be considered successful when:

1. The pool can create a configured number of PostgreSQL connections.
2. Connections can be borrowed and returned.
3. Connections are reused.
4. The pool does not create more connections than configured.
5. Multiple threads can safely use the pool.
6. Threads wait when the pool is exhausted.
7. Connection acquisition can timeout.
8. Invalid connections can be detected and replaced.
9. The pool can shut down cleanly.
10. Automated tests verify the major behaviors.

---

# 22. Future Improvements

Possible future extensions:

- dynamic pool sizing
- connection idle timeout
- maximum connection lifetime
- leak detection
- metrics
- logging
- health checks
- configuration through environment variables
- connection wrappers
- transaction-aware connection handling
- benchmark comparison with direct JDBC connections
- integration with a Spring Boot application

These features should only be added after the core implementation is
understood and tested.