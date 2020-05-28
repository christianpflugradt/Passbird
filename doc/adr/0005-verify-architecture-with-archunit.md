# 5. verify Architecture with ArchUnit

Date: 2020-05-26

## Status

Accepted

## Context

PwMan3 uses domain-driven design in a hexagonal architecture. Its code base is expected to grow a lot over the next few years.

## Decision

To keep code quality high and ensure that changes adhere to the given architectural constraints we want to run ArchUnit tests as part of the CI/CD pipeline.

## Consequences

When a new architectural constraint arises an ArchUnit test should be written to verify it is adhered to.
