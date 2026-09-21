# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
There are three strategies living side by side today:

  - Store     -> active record: Store extends PanacheEntity and StoreResource calls
                 Store.findById / persist / delete directly from the HTTP layer.
  - Product   -> repository: ProductRepository implements PanacheRepository and is
                 injected into ProductResource.
  - Warehouse -> ports and adapters: a plain domain model (Warehouse), a port
                 (WarehouseStore), a JPA entity (DbWarehouse) and an adapter
                 (WarehouseRepository), with the rules living in use cases.

Yes, I would converge them, and the direction I would take is the warehouse one - for
concrete reasons rather than for purity:

  - Testability. The warehouse rules are unit tested in milliseconds against a mocked
    WarehouseStore (see CreateWarehouseUseCaseTest). To test an equivalent rule in
    StoreResource I have to boot Quarkus and a database, because the rule, the HTTP
    concern and the persistence call sit in the same method.
  - The entity is the API contract. Store and Product are serialised straight to JSON,
    so a column rename is a breaking API change and any field added for persistence
    reasons leaks to clients. The warehouse side maps explicitly between the API bean,
    the domain model and the JPA entity, so the three evolve independently.
  - Business rules end up in the HTTP layer. StoreResource is where "name is
    mandatory" lives; there is no place to put a rule that must also hold when the
    same operation is triggered by a batch job or a message consumer.
  - Transaction boundaries. With active record the boundary is the resource method by
    default, which is exactly what made the legacy synchronisation of task 2 unsafe.
    A use case gives an explicit and reusable boundary.

What I would not do is rewrite Product and Store just to make the code look uniform.
The cost is justified when a module gains real invariants; until then the repository
style of Product is a reasonable middle ground and is one step away from a port. What
I would do first, in order: (1) stop returning JPA entities from the resources and
introduce request/response records, (2) move the validation out of the resources into
a service, (3) extract a port once a second caller or a real invariant appears.

One inconsistency I deliberately left in place and would address in a real refactor:
WarehouseResourceImpl still injects the WarehouseRepository adapter for its two read
operations, because WarehouseStore has no "find by technical id" method. I kept the
given port untouched and confined the leak to reads, but the honest fix is to add that
method to the port so the REST adapter depends only on the domain.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Spec first (the Warehouse API)
  + The contract is a reviewable artifact that exists before the implementation, so
    consumers, QA and the team can agree on it and work in parallel against mocks.
  + The compiler enforces it: if a path or a payload changes, the implementation stops
    compiling. The spec cannot silently drift away from the code.
  + Documentation, client SDKs and mock servers come for free from the same file.
  - The generated signatures constrain you. A real example from this assignment: the
    spec documents 201 for POST /warehouse, but the generated interface returns
    Warehouse instead of Response, so the status code cannot be set from the handler.
    I had to add a small response filter (WarehouseCreationStatusFilter) to honour the
    contract - exactly the kind of workaround this approach tends to produce.
  - The generated beans are anemic DTOs, so you always pay for a mapping layer, and
    you inherit the generator's opinions plus one more version to keep up to date.
  - Build friction: sources under target/generated-sources, IDEs that need to be told
    about them, and a failure mode ("regenerate the code") unfamiliar to newcomers.

Code first (Product and Store)
  + Fast and idiomatic: full access to the framework, no mapping ceremony, the obvious
    choice for a small internal endpoint.
  + No build-time magic, so the code you read is the code that runs.
  - The spec becomes an afterthought. Either it does not exist, or it is written by
    hand and drifts from reality - which is worse, because people trust it.
  - Nothing prevents a breaking change: renaming a field, or returning an entity with
    one extra column, ships silently to the consumers.

My choice: spec first for anything with consumers outside the team - which is the case
here, since this system already synchronises with a legacy store manager and would
realistically serve partner or front-end clients - and code first for internal or
short-lived endpoints. The important part is that this should not be a free choice per
developer: whichever is used, the specification must be published as a build artifact
and verified in CI. When I go code first I generate the spec from the annotated code
(quarkus-smallrye-openapi) and fail the build when the committed spec and the generated
one diverge; that gives most of the safety of spec first without the code generation.
I would also keep the generation limited to the interface and the DTOs, as it is done
here, rather than generating the handler implementations.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I prioritise by risk and by the cost of the feedback loop, not by layer.

1. Business rules, as unit tests with mocked ports (fastest, highest value).
   The warehouse validations and the replacement flow are where a mistake is expensive
   and silent: a wrong capacity check lets the company commit to a warehouse it cannot
   operate. They are pure logic, so they are tested in isolation against a mocked
   WarehouseStore and LocationResolver - every rule, including the messages returned.
   The whole set runs in well under a second, so nobody is tempted to skip it.

2. One integration test per endpoint, covering the happy path and the error codes that
   matter (@QuarkusTest with real HTTP calls and a real database). These catch what
   unit tests structurally cannot: serialisation, routing, exception mapping, CDI
   wiring, transaction boundaries and the SQL that is actually produced.

3. Targeted tests on the risky mechanics rather than on the easy code. The one I care
   about most here is the transactional guarantee of task 2: one test asserts that the
   legacy system is notified with a persisted store, another asserts that it is NOT
   notified when the transaction rolls back, and a third asserts that a failure of the
   legacy system does not lose the committed data. That behaviour is invisible in a
   happy-path test and would silently regress the day someone moves the call back into
   the resource method.

4. A thin end-to-end layer against the packaged application (the *IT tests), kept
   small on purpose: slow, environment dependent, and the first thing to become flaky.

What I deliberately do not do is chase a number. Generated code is excluded from the
report, and I would rather have three meaningful assertions on a rule than a test that
walks a getter to lift a percentage.

Keeping it effective over time:
  - The gate is automated. `./mvnw verify` fails below 80% (jacoco:check) and CI runs
    it on every push and pull request, so coverage cannot erode quietly. The threshold
    is a floor that detects neglect, not a target to optimise.
  - Tests must stay fast and independent. This suite needs no Docker daemon, and no
    test depends on another's data or on the execution order. A suite that is slow or
    order-dependent gets disabled, and a disabled suite protects nothing.
  - Every production bug gets a failing test first, then the fix. That is what keeps
    the suite aligned with where the system actually breaks.
  - Review the tests, not only the coverage delta: does the test fail if I break the
    rule? Mutation testing (PIT) over the domain package is a good periodic check of
    exactly that, without paying for it on every build.
  - Revisit the trade-offs as the project grows. I run the suite on H2 for speed and to
    keep CI free of a Docker dependency; the risk is divergence from PostgreSQL, so in
    a real project I would also run the same suite against a PostgreSQL container
    (Testcontainers / Quarkus Dev Services) on the merge queue or nightly.
```
