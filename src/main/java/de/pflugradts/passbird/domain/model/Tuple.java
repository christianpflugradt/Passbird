package de.pflugradts.passbird.domain.model;

import lombok.Value;

@Value
public class Tuple<A, B> {
    public final A _1;
    public final B _2;
}
