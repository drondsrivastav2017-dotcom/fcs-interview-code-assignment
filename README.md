# Fulfilment & Cost Control — code assignment

This repository contains my solution for the assignment.

| What                                                       | Where                                                  |
| ---------------------------------------------------------- | ------------------------------------------------------ |
| The application, how to run it and the design decisions     | [java-assignment/README.md](java-assignment/README.md) |
| The tasks being solved                                      | [java-assignment/CODE_ASSIGNMENT.md](java-assignment/CODE_ASSIGNMENT.md) |
| Answers to the three code base questions                    | [java-assignment/QUESTIONS.md](java-assignment/QUESTIONS.md) |
| Answers to the case study scenarios                         | [case-study/CASE_STUDY.md](case-study/CASE_STUDY.md)   |
| Domain overview                                             | [case-study/BRIEFING.md](case-study/BRIEFING.md)       |
| CI pipeline (tests + 80% coverage gate)                     | [.github/workflows/ci.yml](.github/workflows/ci.yml)   |

## Quick start

```sh
cd java-assignment
./mvnw verify      # build, run the tests and enforce the coverage threshold
./mvnw quarkus:dev # run the application (needs Docker for the PostgreSQL dev service)
```

The test suite runs on an in-memory database, so it needs no Docker daemon.

## About the code base

Some of this code here is based on https://github.com/quarkusio/quarkus-quickstarts
