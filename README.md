# parameter-source

A ¡Laamella Gad! production.

## What problem does this solve?

Reading configuration parameters during application startup is often a little messy, leading to unnecessary long debugging times.
This tiny library wants to make it a solid experience.

Perceived problems are:
- sloppy null handling leading to confusing null pointer exceptions down the road
- sloppy type conversions (string to int, for example) leading to undescriptive errors
- sloppy file handling when we are dealing with a configuration file leading to undescriptive errors
- messy API's, different for every source of parameters
- arcane implementations making unit testing difficult

Offered solutions are:
- an API that has methods that will either fail hard when a required parameter is missing, or will deliver a Java 8 Optional when the parameter is not required
- file handling and type conversion work as tight as possible.
If a parameter is correct, it will be delivered, otherwise it will throw an exception so that an application does not start in an undefined state.
- tight support for the kind of values that are often seen as application parameters: strings, durations, urls...
- one approach to API naming and much reuse, so that parameter sources will look alike as much as possible.
Extending a class to define your own source is easy.
- extensive logging that will show every action. 

## Adding the dependency

Put this in your pom.xml:
```xml
<dependency>
	<groupId>com.laamella</groupId>
	<artifactId>parameter-source</artifactId>
	<version>1.0</version>
</dependency>
```

## Using a properties file as the source

This is the simplest way to start using a properties file as a source of parameters.
```java
PropertiesParameterSource source = new PropertiesParameterSource("application.properties");
```
Other constructor offer more flexibility.

## Using a database as the source

This is the simplest way to start using a database table as a source of parameters,
specifying the JDBC database properties and the significant properties of your parameter table.
```java
JdbcDatabaseParameterSource source = new JdbcDatabaseParameterSource("jdbc:h2:mem:db", "user", "passwd", "CONFIGURATION", "KEY", "VALUE");
```
Other constructor offer more flexibility.

## Using Java's Preferences API as the source

This is the simplest way to get access to the Preferences API.
```java
PreferencesParameterSource source = new PreferencesParameterSource();
```
Please read the Javadoc for some limitations on this implementation.

## Using JNDI as the source

This is the simplest way to start using JNDI as a source of parameters.
```java
JndiParameterSource source = new JndiParameterSource();
```
You may want to pass an InitialContext to the constructor instead.

## Using a static parameter source

This stores the key-value pairs in memory and can be used as a fallback for other sources,
or for testing.
```java
InMemoryParameterSource source = new InMemoryParameterSource()
    .put("abc", "def")
    .put("ghi", "jkl");
```

## Getting an optional string value
Getters that mention "Optional" in their name are optional.
This is implemented with Java 8's `Optional` class.
You can use it to supply a default value.
```java
Optional<String> value = source.getOptionalString("abc");
String value = source.getOptionalString("abc").orElse("myDefaultVal");
```

## Getting a required string value
Getters that do NOT mention "Optional" in their name are required.
These will throw a `ParameterSourceException` when the key is not found.
```java
String value = source.getString("abc");
```

## Getting other values

The pattern described above for string is the same for other types.

### The duration format

When requesting a duration, we get the string value for the key, then attempt to parse it as follows:

ISO 8601 inspired:
```
1D2H3M4S5MS6NS       -> 1 day + 2 hours + 3 minutes + 4 seconds + 5 milliseconds + 6 nanoseconds
1d2h3m4s5ms6ns       -> the same
1d 2h 3m 4s 5ms 6ns  -> the same
1d                   -> 1 day
1 day                -> 1 day
1000ms               -> 1000 milliseconds
3m 10s               -> 3 minutes + 10 seconds
3 minutes 10 seconds -> 3 minutes + 10 seconds
```

Traditional time format inspired:
```
12:14:16.1234  -> 12 hours + 14 minutes + 16 seconds + 123.4 milliseconds
14:16          -> 14 minutes + 16 seconds
16             -> 16 seconds
```

### The byte size format

When requesting a size in bytes, we get the string value for the key, then attempt to parse it as follows:

```
0 -> 0 bytes
10 -> 10 bytes
10 bytes -> 10 bytes
10B -> 10 bytes
10KB -> 10 kilobytes
1 kilobyte 512B -> 1536 bytes
```
Supported sizes are:

```
eb exabyte(s)
pb petabyte(s)
tb terabyte(s)
gb gigabyte(s)
mb megabyte(s)
kb kilobyte(s)
b byte(s)
```

### The host and port format

```
192.168.1.1:8080
192.168.1.1
bla.com:1234
bla.com
```
The only check is that the value following `:` is a number.

### The enum format

Enums values are case insensitive,
and underscores, whitespace, and quotations marks are ignored.

## Type conversions in general

The general pattern is:

1. get the value of the key
1. is the type of the value of the type that was requested? Return it.
1. is the type of the value related to the type that was requested? Convert it and return it.
1. is the type of the value `String`? Try to parse it, and construct and return the requested type.
1. fail with a `ParameterSourceException`

Some requested types may skip step 2 because they have no commonly used type.

So in general, the pattern is to do maximum effort to return a value, but without causing unexpected behaviour. 
 
## Reducing repetition in keys

Often keys are (or emulate) a path.
For example: properties files use dots as separators, JNDI uses slashes.
To avoid having to specify the same paths over and over again, we can make `SubParameterSource`s.

Here is an example for dealing with a good old log4j.properties file:

```
log4j.rootLogger=INFO, stdout

log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.Target=System.out
```

```java
PropertiesParameterSource source = new PropertiesParameterSource("log4j.properties");
SubParameterSource log4j = source.subSource("log4j");
log4j.getString("rootLogger"); // INFO, stdout

SubParameterSource stdoutAppender = log4j.subSource("appender.stdout");
stdoutAppender.getString(""); // org.apache.log4j.ConsoleAppender
stdoutAppender.getString("Target"); // System.out
```

## Chaining sources

Sources can be chained so that parameters that come from different places can be hidden behind a single `ParameterSource`.

```java
JndiParameterSource jndiParameterSource = new JndiParameterSource();
PropertiesParameterSource propertiesParameterSource = new PropertiesParameterSource("...");

FallbackParameterSource source = jndiParameterSource.withFallback(propertiesParameterSource);
```
Now `source` will look in `jndiParameterSource` first,
and if that does not contain the requested key it will look in `propertiesParameterSource`.

Fallbacks can be chained as much as required.

If you have trouble with different types of keys in these sources,
it might help to use "subSource" to set the path so tight that the keys do not need path separators anymore.

## Stubbing a source for unit testing

`StubParameterSource` are meant for testing.
These return the contained stub value for every key.
Note that conversions will still happen,
so storing a string stub value and requesting an integer will try to convert the string to an integer.

## Writing your own parameter source

First, extend `ParameterSource`.
Now you will have to choose between overriding `getOptionalObject` or `getOptionalString`.
If your source is a true object store, meaning that it can store values of any type, override `getOptionalObject`.
If your source is a string store, meaning that it stores only strings, override `getOptionalString`.

Try to catch any exceptions and wrap them in `ParameterSourceException`s.

If you want to override the handling of a certain type,
always override the optional version of the method since the required version always calls the optional one.
Consider opening an issue on the github project if you disagree with the default behaviour.
Keep to the pattern indicated in "type conversions in general."

## The project

As this library is focused on scratching one small itch,
it should reach maturity pretty quickly and not require a lot of releases.
Issues and PR's are welcome.
