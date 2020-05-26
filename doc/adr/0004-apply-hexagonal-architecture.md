# 4. apply Hexagonal Architecture

Date: 2020-05-26

## Status

Accepted

## Context

PwMan3 has has multiple dependencies to external resources. It is driven via stdin, replies via stout and the system clipboard, stores data in a database file and uses a keystore for encryption. We want PwMan3 to be as flexible as possible when implementing interactions with other resources later on. For example it should be possible to run PwMan3 as a server software communicating over http with a client without rewriting the whole code base from scratch.

## Decision

To separate the domain and the core application from the technical adapters that define communication with external resources, we want to use a hexagonal architecture. Combined with domain-driven design we hope to achieve a healthier growth of PwMan3 when new functionality is added. A hexagonal architecture ensures a clean separation of concerns between different adapters, the core application logic and the non technical domain logic and it also makes it easier for programmers unfamiliar with the code base to get started because the structure of a hexagonal architecture is well known.

To decouple adapters from the application each adapter should have exactly one public entry point represented by a interface in the application layer named ...AdapterPort. Thus when adressing an adapter port from the application layer it is obvious, that logic from an adapter is invoked. As specified in hexagonal architecture adapters might not talk directly to each other, instead the application layer orchestrates communication if necessary. This will ensure there are no dependencies between adapters.

## Consequences

A hexagonal architecture itself is no guarantee for healthy growth. Each new feature or change requires careful consideration how it should be reflected in the code base.
