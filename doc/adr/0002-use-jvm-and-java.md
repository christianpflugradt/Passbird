# 2. use JVM and Java

Date: 2020-05-26

## Status

Accepted

## Context

PwMan3 is a desktop application running on a system on which a user needs to access their passwords.

## Decision

We choose to use the JVM because it is mostly platform independent and it allows us to run PwMan3 on almost any device. We won't need to ship different versions for different architectures or operating systems and given that we don't use code that addresses the API of a specific operating system we only need to test PwMan3 against one operating system and can be certain it will work on others as well. We further choose to use Java as the main language because we have the most experience with it. Additionally the JVM environment will allow us to use libraries from other JVM languages such as Scala and Kotlin.

## Consequences

PwMan3 requires Java to run, so Java must be installed on the system. There are other consequences of using Java but we find them negligible.