# Java Code Assignment

Warehouse colocation management service built with Quarkus. The tasks being solved are described in
[CODE_ASSIGNMENT.md](CODE_ASSIGNMENT.md); the domain is described in [BRIEFING.md](BRIEFING.md) and
the written answers are in [QUESTIONS.md](QUESTIONS.md).

## Requirements

- JDK 17 or newer (`JAVA_HOME` set and `java` on the path)
- Docker is **optional**: it is only needed to run the application against PostgreSQL. The test
  suite and the `local-h2` run profile below both use an in-memory database and need no Docker
  daemon, and a `local-mysql` profile is available for running against an existing MySQL.

## Running the tests

```sh
./mvnw verify
```

This compiles the project, runs the whole test suite and fails the build if line or instruction
coverage drops below 80%. The HTML report is written to `target/site/jacoco/index.html`.

To run only the tests, without the coverage gate:

```sh
./mvnw test
```

## Running the application

Dev mode (Quarkus Dev Services starts a PostgreSQL container automatically, so Docker must be
running):

```sh
./mvnw quarkus:dev
```

Dev mode **without Docker**, on an in-memory database — same live reload, nothing to install. This
is also the command to use as a Maven run configuration in IntelliJ:

```sh
./mvnw quarkus:dev -Plocal-h2 -Dquarkus.profile=local
```

Against a local **MySQL** instead — useful to inspect the data with MySQL Workbench, since the rows
survive a restart. Create the schema once, then run with the `local-mysql` profile:

```sql
CREATE DATABASE IF NOT EXISTS fulfilment;
CREATE USER IF NOT EXISTS 'quarkus_test'@'localhost' IDENTIFIED BY 'quarkus_test';
GRANT ALL PRIVILEGES ON fulfilment.* TO 'quarkus_test'@'localhost';
```

```sh
./mvnw quarkus:dev -Plocal-mysql -Dquarkus.profile=mysql
```

Host, port, database and credentials can be overridden with the `MYSQL_HOST`, `MYSQL_PORT`,
`MYSQL_DB`, `MYSQL_USER` and `MYSQL_PASSWORD` environment variables. MySQL has no sequences, so this
profile seeds from `import-mysql.sql`, which resets Hibernate's sequence-emulation tables with an
`UPDATE` rather than the `ALTER SEQUENCE` used by PostgreSQL and H2.

Or as a packaged application on the same in-memory database (data resets on restart):

```sh
./mvnw package -Plocal-h2 -DskipTests -Dquarkus.profile=local
java -Dquarkus.profile=local -jar target/quarkus-app/quarkus-run.jar
```

The `local-h2` Maven profile only raises the H2 driver from test to compile scope; it is not part of
the default build, so it changes nothing for the normal PostgreSQL setup or for CI.

As a packaged application, against a local PostgreSQL:

```sh
docker run -it --rm=true --name quarkus_test \
  -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test \
  -p 15432:5432 postgres:13.3

./mvnw package
java -jar ./target/quarkus-app/quarkus-run.jar
```

If your PostgreSQL listens on the standard port instead of `15432`, override the URL at startup
rather than editing the file:

```sh
java -Dquarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/quarkus_test \
  -jar target/quarkus-app/quarkus-run.jar
```

The connection properties are in `src/main/resources/application.properties` and the schema is
recreated and seeded from `src/main/resources/import.sql` at every start.

Then open <http://localhost:8080/index.html>.

`WarehouseEndpointIT` is a `@QuarkusIntegrationTest` that runs against the packaged application and
therefore needs a running PostgreSQL. It is not part of the default build (the failsafe plugin is
only configured in the `native` profile, as in the original project); the same scenarios are covered
by `WarehouseEndpointTest` during `./mvnw verify`.

## API

### Warehouse — generated from `src/main/resources/openapi/warehouse-openapi.yaml`

| Method   | Path                                       | Description                                       |
| -------- | ------------------------------------------ | ------------------------------------------------- |
| `GET`    | `/warehouse`                               | List the active warehouses                        |
| `POST`   | `/warehouse`                               | Create a warehouse (`201`)                        |
| `GET`    | `/warehouse/{id}`                          | Get an active warehouse by its technical id       |
| `DELETE` | `/warehouse/{id}`                          | Archive a warehouse (`204`)                       |
| `POST`   | `/warehouse/{businessUnitCode}/replacement`| Replace the active warehouse of that business unit|

```sh
curl -X POST http://localhost:8080/warehouse -H 'Content-Type: application/json' \
  -d '{"businessUnitCode":"MWH.100","location":"VETSBY-001","capacity":80,"stock":10}'
```

### Fulfillment (bonus) — associating warehouses as fulfilment units

| Method   | Path                                        | Description                                  |
| -------- | ------------------------------------------- | -------------------------------------------- |
| `GET`    | `/fulfillment`                              | List all the associations                    |
| `GET`    | `/fulfillment/store/{storeId}`              | List the associations of a store             |
| `GET`    | `/fulfillment/warehouse/{businessUnitCode}` | List the associations of a warehouse         |
| `POST`   | `/fulfillment`                              | Associate a warehouse/product/store (`201`)  |
| `DELETE` | `/fulfillment/{id}`                         | Remove an association (`204`)                |

```sh
curl -X POST http://localhost:8080/fulfillment -H 'Content-Type: application/json' \
  -d '{"storeId":1,"productId":1,"warehouseBusinessUnitCode":"MWH.001"}'
```

`Store` and `Product` keep their original CRUD endpoints under `/store` and `/product`.

## What was implemented

**1. Location** — `LocationGateway.resolveByIdentifier` resolves a location by its identifier,
ignoring case and surrounding spaces, and returns `null` when it is unknown so that the caller can
produce a meaningful error.

**2. Store** — the legacy system is no longer called inline. `StoreResource` publishes a
`StoreSyncEvent` and `LegacyStoreSynchronizer` observes it during `TransactionPhase.AFTER_SUCCESS`,
so the downstream system is only notified with data that is effectively committed, and a rolled back
transaction never reaches it. The event carries a detached copy of the store, because the observer
runs after the persistence context is closed, and a failure of the legacy system is logged instead
of failing a request whose data is already committed.

**3. Warehouse** — creation, retrieval, replacement and archiving, with the validations required by
the assignment: the business unit code must be free, the location must exist, the location must not
have reached its maximum number of warehouses, the capacity must fit in the remaining capacity of
the location and must accommodate the informed stock. A replacement additionally requires that the
new capacity accommodates the stock of the warehouse being replaced and that both stocks match.

`ReplaceWarehouseUseCase` archives the previous unit and then delegates to `CreateWarehouseUseCase`,
in a single transaction. Archiving first releases the business unit code and the location capacity,
so the new unit is validated against the state the company will actually operate in; if the creation
fails, the whole replacement is rolled back.

Warehouses are never physically deleted: archiving sets `archivedAt` and the row is kept, which is
what preserves the history of the business unit (and the cost history discussed in the case study).
The active warehouses are the ones returned by the API.

**Bonus** — `Fulfillment` associates a warehouse, a product and a store, enforcing the three
constraints: at most 2 warehouses per product in a store, at most 3 warehouses per store and at most
5 product types per warehouse.

## Design decisions and assumptions

- **The warehouse is referenced by its business unit code in the fulfilment associations**, not by
  row id. A replacement archives the row but keeps the business unit code, and the new unit is
  expected to take over the fulfilment duties of the one it replaced.
- **`{id}` in the warehouse API is the technical id**, as in the OpenAPI specification and in the
  provided integration test, while the replacement endpoint is addressed by business unit code.
- **A replacement may target a different location.** The briefing describes replacements in the same
  area, but the constraint is not listed among the required validations, so instead of forbidding it
  the new location is fully validated (existence, number of warehouses and capacity).
- **Errors** are raised as domain exceptions (`ValidationException`, `ResourceNotFoundException`) and
  translated to `400` and `404` by `ExceptionMappers`, keeping JAX-RS types out of the domain.
- **`WarehouseCreationStatusFilter`** restores the `201` documented in the specification: the
  generated interface returns `Warehouse` instead of `Response`, so the status cannot be set by the
  handler itself.
- **Tests run on H2** rather than on a PostgreSQL container, so the suite is fast and CI needs no
  Docker daemon. The trade-off (fidelity to the production database) is discussed in
  [QUESTIONS.md](QUESTIONS.md).

## Tests and coverage

| Test                       | What it covers                                                        |
| -------------------------- | --------------------------------------------------------------------- |
| `*UseCaseTest`             | Every warehouse business rule, in isolation, with mocked ports        |
| `LocationGatewayTest`      | Location resolution, including unknown and blank identifiers          |
| `WarehouseRepositoryTest`  | Persistence adapter, including archiving as a soft delete             |
| `WarehouseEndpointTest`    | The warehouse API: happy paths and every rejection, over real HTTP    |
| `StoreEndpointTest`        | Store CRUD and the transactional guarantee of the legacy sync         |
| `ProductEndpointTest`      | Product CRUD and its error responses                                  |
| `FulfillmentEndpointTest`  | The bonus feature and its three constraints                           |

Coverage is collected by the `quarkus-jacoco` extension (the standard agent does not see the classes
loaded by the Quarkus test classloader) and reported and enforced by `jacoco-maven-plugin`. The
generated API package is excluded. Current line coverage is **94%**, against a required minimum of
80% — and it is understated, since coverage produced by the plain unit tests is not recorded by the
extension.

## CI

`.github/workflows/ci.yml` runs on every push and pull request: it builds the project, runs the
tests, enforces the 80% coverage threshold, publishes a coverage summary in the job summary and
uploads the test and coverage reports as artifacts.

## Troubleshooting

Using **IntelliJ**, in case the generated code is not recognized and you have compilation failures,
you may need to add `target/generated-sources/jaxrs` as "generated sources".

**Build fails with `Java 23 (67) is not supported by the current version of Byte Buddy`** — Quarkus
3.13.3 supports up to Java 22, so build and run on JDK 17 or 21. In IntelliJ the relevant setting is
*Build, Execution, Deployment → Build Tools → Maven → Runner → JRE*, not only the project SDK.

**The seeded `BESTÅ` name prints as `BEST?` in the console** — display only, on JDK 17 on Windows,
where `file.encoding` defaults to `Cp1252`. The stored data and the API responses are correct UTF-8
on every JDK. Add `-Djvm.args=-Dfile.encoding=UTF-8` to the dev command if you want the log to read
cleanly.
