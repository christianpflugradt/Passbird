# 3. apply Domain-driven Design

Date: 2020-05-26

## Status

Accepted

## Context

PwMan3 in its current state has a small domain but a big technical overhead because all data must be represented and transferred as byte arrays and those must also be overwritten with random bytes after use. All data stored for a longer time at runtime must be encrypted, thus all of this data that is processed in any way must be decrypted and data added or modified must be encrypted as well.

## Decision

To separate the domain, which deals with storing and handing out passwords, from the technical overhead described in the context and to ensure a healthy growth when new domain functionality is added we choose to apply domain-driven design.

## Consequences

PwMan3 will have a central domain package which is free of any technical stuff and may not have dependencies to code outside of the domain. All classes within the domain must be assigned to any of the building blocks defined in domain-driven design.