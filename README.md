# In Kotlin the preDestroy bound to the request scope is not called at the end of a probe

## How to reproduce

- start the app `./mvnw clean quarkus:dev`
- call the <http://localhost:8080/q/health> a few times and see the release not being invoked -> both in the webpage and in the logs

This is like this since... at least quarkus 3.6 (I think that's where I started using probes). And it still like this after quarkus 3.7.

## How to solve the issue

add this line

```
# src/main/resources/application.properties
quarkus.smallrye-health.context-propagation=true
```

## Example of Health not releasing resources

The sources contain 3 files

- `Endpoint.kt` which exposes:
  - expose <http://localhost:8080/> that accesses a "DbSource" then release it manually
  - expose <http://localhost:8080/auto> that accesses a "DbSource" then release it automatically thanks to the `@PreDestroy`

- `DbSource.kt` that emulates a pool of 4 tokens (like a DbPool)
  - `acquire` tries to acquire a token and immediately returns upon success
  - when the pool is exhausted `acquire` will block up to 3s then throw
  - `release` returns a token to the pool
  - `DbSource.kt` is marked as `@RequestScoped` and has a `@PreDestroy` method that does the `release` when the request is finished

- `ReadinessAndLiveness.kt` that implements HealCheck that tries to acquires a token

### The issue

The calling HealthCheck <http://localhost:8080/q/health/> although marked as RequestScoped does not call the `@PreDestroy` after the first call.

Example of logs when calling 5 times the HealthCheck:

```sh
mvn quarkus:dev
...
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/
2024-02-20 11:19:44,222 INFO  [io.quarkus] (Quarkus Main Thread) microprofile-health-quickstart 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.6.7) started in 1.572s. Listening on: http://0.0.0.0:8080

2024-02-20 11:19:44,225 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2024-02-20 11:19:44,226 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [cdi, resteasy-reactive, smallrye-context-propagation, smallrye-health, vertx]
2024-02-20 11:19:51,067 INFO  [myp.DbSource] (vert.x-worker-thread-1) Acquire...
2024-02-20 11:19:51,068 INFO  [myp.DbSource] (vert.x-worker-thread-1) Acquire OK!1
2024-02-20 11:19:51,077 WARN  [myp.DbSource] (vert.x-worker-thread-1) endOfRequest called
2024-02-20 11:19:51,078 INFO  [myp.DbSource] (vert.x-worker-thread-1) Release
2024-02-20 11:19:52,782 INFO  [myp.DbSource] (vert.x-worker-thread-1) Acquire...
2024-02-20 11:19:52,783 INFO  [myp.DbSource] (vert.x-worker-thread-1) Acquire OK!1
2024-02-20 11:19:53,621 INFO  [myp.DbSource] (vert.x-worker-thread-1) Acquire...
2024-02-20 11:19:53,623 INFO  [myp.DbSource] (vert.x-worker-thread-1) Acquire OK!0
2024-02-20 11:19:54,124 INFO  [myp.DbSource] (vert.x-worker-thread-1) Acquire...
2024-02-20 11:19:57,127 INFO  [io.sma.health] (vert.x-worker-thread-1) SRHCK01001: Reporting health down status: {"status":"DOWN","checks":[{"name":"DbConnection","status":"DOWN","data":{"Exception":"Could not acquire on time"}}]}
```

### For other endpoints it works well

Calling <http://localhost:8080/> or <http://localhost:8080/auto> works and releases the connection each time

```sh
mvn quarkus:dev
...
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/
2024-02-20 11:18:36,981 INFO  [io.quarkus] (Quarkus Main Thread) microprofile-health-quickstart 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.6.7) started in 1.559s. Listening on: http://0.0.0.0:8080

2024-02-20 11:18:36,984 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2024-02-20 11:18:36,985 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [cdi, resteasy-reactive, smallrye-context-propagation, smallrye-health, vertx]
2024-02-20 11:18:49,695 INFO  [myp.Endpoint] (executor-thread-1) manual
2024-02-20 11:18:49,698 INFO  [myp.DbSource] (executor-thread-1) Acquire...
2024-02-20 11:18:49,699 INFO  [myp.DbSource] (executor-thread-1) Acquire OK!1
2024-02-20 11:18:49,699 INFO  [myp.DbSource] (executor-thread-1) Release
2024-02-20 11:18:49,703 WARN  [myp.DbSource] (executor-thread-1) endOfRequest called
2024-02-20 11:18:52,604 INFO  [myp.Endpoint] (executor-thread-1) manual
2024-02-20 11:18:52,605 INFO  [myp.DbSource] (executor-thread-1) Acquire...
2024-02-20 11:18:52,605 INFO  [myp.DbSource] (executor-thread-1) Acquire OK!1
2024-02-20 11:18:52,606 INFO  [myp.DbSource] (executor-thread-1) Release
2024-02-20 11:18:52,606 WARN  [myp.DbSource] (executor-thread-1) endOfRequest called
2024-02-20 11:18:53,090 INFO  [myp.Endpoint] (executor-thread-1) manual
2024-02-20 11:18:53,091 INFO  [myp.DbSource] (executor-thread-1) Acquire...
2024-02-20 11:18:53,091 INFO  [myp.DbSource] (executor-thread-1) Acquire OK!1
2024-02-20 11:18:53,092 INFO  [myp.DbSource] (executor-thread-1) Release
2024-02-20 11:18:53,092 WARN  [myp.DbSource] (executor-thread-1) endOfRequest called
2024-02-20 11:18:53,471 INFO  [myp.Endpoint] (executor-thread-1) manual
2024-02-20 11:18:53,472 INFO  [myp.DbSource] (executor-thread-1) Acquire...
2024-02-20 11:18:53,472 INFO  [myp.DbSource] (executor-thread-1) Acquire OK!1
2024-02-20 11:18:53,473 INFO  [myp.DbSource] (executor-thread-1) Release
2024-02-20 11:18:53,473 WARN  [myp.DbSource] (executor-thread-1) endOfRequest called
2024-02-20 11:18:56,115 INFO  [myp.Endpoint] (executor-thread-1) manual
2024-02-20 11:18:56,116 INFO  [myp.DbSource] (executor-thread-1) Acquire...
2024-02-20 11:18:56,116 INFO  [myp.DbSource] (executor-thread-1) Acquire OK!1
2024-02-20 11:18:56,117 INFO  [myp.DbSource] (executor-thread-1) Release
2024-02-20 11:18:56,117 WARN  [myp.DbSource] (executor-thread-1) endOfRequest called
2024-02-20 11:19:03,914 INFO  [myp.Endpoint] (executor-thread-1) auto
2024-02-20 11:19:03,915 INFO  [myp.DbSource] (executor-thread-1) Acquire...
2024-02-20 11:19:03,915 INFO  [myp.DbSource] (executor-thread-1) Acquire OK!1
2024-02-20 11:19:03,916 WARN  [myp.DbSource] (executor-thread-1) endOfRequest called
2024-02-20 11:19:03,916 INFO  [myp.DbSource] (executor-thread-1) Release
2024-02-20 11:19:04,668 INFO  [myp.Endpoint] (executor-thread-1) auto
2024-02-20 11:19:04,669 INFO  [myp.DbSource] (executor-thread-1) Acquire...
2024-02-20 11:19:04,670 INFO  [myp.DbSource] (executor-thread-1) Acquire OK!1
2024-02-20 11:19:04,670 WARN  [myp.DbSource] (executor-thread-1) endOfRequest called
2024-02-20 11:19:04,671 INFO  [myp.DbSource] (executor-thread-1) Release
2024-02-20 11:19:04,965 INFO  [myp.Endpoint] (executor-thread-1) auto
2024-02-20 11:19:04,965 INFO  [myp.DbSource] (executor-thread-1) Acquire...
2024-02-20 11:19:04,966 INFO  [myp.DbSource] (executor-thread-1) Acquire OK!1
2024-02-20 11:19:04,966 WARN  [myp.DbSource] (executor-thread-1) endOfRequest called
2024-02-20 11:19:04,967 INFO  [myp.DbSource] (executor-thread-1) Release
2024-02-20 11:19:05,232 INFO  [myp.Endpoint] (executor-thread-1) auto
2024-02-20 11:19:05,232 INFO  [myp.DbSource] (executor-thread-1) Acquire...
2024-02-20 11:19:05,233 INFO  [myp.DbSource] (executor-thread-1) Acquire OK!1
2024-02-20 11:19:05,233 WARN  [myp.DbSource] (executor-thread-1) endOfRequest called
2024-02-20 11:19:05,234 INFO  [myp.DbSource] (executor-thread-1) Release
```
