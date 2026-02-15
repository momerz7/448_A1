# Objective

To design, implement, and test 3 failure-handling policies explicit for concurrent microservice execution. Running microservices in parallel makes failures nondeterministic, giving the need of handling them deliberatly. The focus is to have clear and predictable failure semantics for fan-in/fan-out concurrency pattern.

# System

## Microservice

- Each microservice has its own unique "serviceId".
- Each exposes "CompletableFuture<String> retrieveAsync(String message)" asynchronous operation.
- Concurrently executes with nondeterministic completion order.

## AsyncProcessor

- Result aggregation with CompletableFuture
- Fan-out calls to multiple microservices
- List order is preserved unless if it's changed by a policy on purpose
- Cloud pattern modeling that's used in distributed systems
