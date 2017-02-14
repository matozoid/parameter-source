package com.laamella.parameter_source;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class InMemoryStringParameterSource implements StringParameterSource {
    private final Map<String, String> storage = new HashMap<>();

    public InMemoryStringParameterSource put(String key, String s) {
        requireNonNull(key);

        storage.put(key, s);
        return this;
    }

    @Override
    public Optional<String> getOptionalString(String key) {
        requireNonNull(key);

        return Optional.ofNullable(storage.get(key));
    }
}