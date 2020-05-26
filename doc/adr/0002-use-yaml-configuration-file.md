# 2. use YAML Configuration file

Date: 2020-05-26

## Status

Accepted

## Context

In order to make PwMan3 customizable and better adjust it to personal needs it should have a configuration.

## Decision

The configuration should be provided as a file so that it is in a dedicated place, accessible and editable without launching PwMan3. We choose the yaml format to store all configuration properties because it is easy to read and to edit compared to xml and json, which add a lot of bloat (tags in xml, curly braces and quotes in json). It is also easier to read than Java's property format which does not make use of indents and is hard to read for large configuration files.

## Consequences

The validity of the configuration file must be verified by PwMan3. Also yaml has the disadvantage that left out or additional tabs will change the structure of the file. We decide that this disadvantage is still less significant than the additional bloat introduced by other formats such as xml and json.
