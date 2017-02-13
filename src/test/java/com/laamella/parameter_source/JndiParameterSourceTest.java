package com.laamella.parameter_source;

import org.junit.Test;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JndiParameterSourceTest {
    private final InitialContext initialContextMock = mock(InitialContext.class);
    private final JndiParameterSource jndiParameterSource = new JndiParameterSource(initialContextMock);

    @Test
    public void whenRequiredParameterExistsThenReturnIt() throws NamingException {
        when(initialContextMock.lookup("abc")).thenReturn("def");
        Optional<Object> value = jndiParameterSource.getOptionalValueFromSource("abc");
        assertEquals("def", value.get());
    }

    @Test
    public void whenRequiredParameterIsNullThenException() throws NamingException {
        when(initialContextMock.lookup("abc")).thenReturn(null);
        Optional<Object> abc = jndiParameterSource.getOptionalValueFromSource("abc");
        assertEquals(false, abc.isPresent());
    }

    @Test
    public void whenRequiredParameterIsNotFoundThenException() throws NamingException {
        when(initialContextMock.lookup("abc")).thenThrow(new NamingException());
        Optional<Object> abc = jndiParameterSource.getOptionalValueFromSource("abc");
        assertEquals(false, abc.isPresent());
    }
}
