# Objective

To design, implement, and test 3 failure-handling policies explicit for concurrent microservice execution. Running microservices in parallel makes failures nondeterministic, giving the need of handling them deliberatly. The focus is to have clear and predictable failure semantics for fan-in/fan-out concurrency pattern.

# System

### Microservice

- Each microservice has its own unique "serviceId".
- Each exposes "CompletableFuture<String> retrieveAsync(String message)" asynchronous operation.
- Concurrently executes with nondeterministic completion order.

### AsyncProcessor

- Result aggregation with CompletableFuture
- Fan-out calls to multiple microservices
- List order is preserved unless if it's changed by a policy on purpose
- Cloud pattern modeling that's used in distributed systems

---

# 3 Failure Policies

### Fail-Fast (Atomic Policy)

#### How does it work ?

- if any microservice fails -> whole computation fails instantly
- Concurrent runnig of all microservices
- Once first failure occcurs, aggregate operation completes exceptionally
- No return of partial resuls

#### When should it be used ?

- there are partial results that are meaningless or that are dangerous
- Need of strong consistency
- System must not proceed if there is uncertainty

##### Examples

- Payments (transaction aborts if validation service fails)
- Authentication (if a checking stepfails then the whole login attempt fails)

#### What are the risks ?

- Availability is reduced since one failing service causes the entire system to be blocked
- Cascading failures (requests can break from one bad microservice)

### Fail-Partial (Best-Effort Policy)

#### How does it work ?

- Returns only successful results, ignoring failures
- All microservices run concurrently
- final output has only successful operations, failures don't abort operation

#### When should it be used ?

- Partial data is valuable
- Gracefull degradation of system
- If failure happens then unrelated results shoudn't be blocked

##### Examples

-Analytics dashboard (if data source is down then the rest of data can still be shown)
Search aggregation (if one provider fails, still return other results)

#### What are the risks ?

- Downstream systems can be mislead by incomplete results
- If failures aren't logged/monitored, they might go unnoticed
- User must know about possibility of partial output

### Fail-Soft (FallbackPolicy)

#### How does it work ?

-  Failures replaced with predefined fallback value
-  Computation always completes normally
-  All microservices run concurrently
-  Final output: mix of real and fallback values

#### When should it be used ?

- High availability importance > accuracy
- System always returns a response
- Degraded output = acceptable

##### Examples

- Recommendation system (model fails -> default suggestion)

#### What are the risks ?

- Hidden failures (fallback values can cause issues)
- Silent Degradation
- Debugging difficulty (accumulation of failures could happen) 
