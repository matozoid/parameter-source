package com.laamella.parameter_source;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FallbackParameterSourceTest {
    @Test
    public void whenFallingBackThenItWorks() {
        FallbackParameterSource parameterSource = new InMemoryParameterSource()
                .put("a", "1")
                .withFallback(new InMemoryParameterSource()
                        .put("b", "2")
                        .withFallback(new InMemoryParameterSource()
                                .put("a", "3")));

        assertEquals("1", parameterSource.getString("a"));
        assertEquals(2, parameterSource.getInteger("b"));
        assertEquals(false, parameterSource.getOptionalString("c").isPresent());
    }
}
