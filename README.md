# java-plist

java-plist serializes and deserializes Java Beans into the [plist](https://en.wikipedia.org/wiki/Property_list) XML format.

## Goals

- No third-party dependencies.
- Java 1.8 and higher.
- Must be as ergonomic and fast as possible, in that order.

[![Javadoc](https://img.shields.io/badge/JavaDoc-Online-green)](https://vieiro.github.io/java-plist/javadoc/)

## Usage

java-plist is available in Maven Central under the following coordinates:

```xml
<dependency>
    <groupId>net.vieiro</groupId>
    <artifactId>java-plist</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Reading `plist` files (deserializing Java objects)

Read a Java Bean from a file:

```java
File f = ...
MyBean bean = (MyBean) PListIO.read(f);
```

Or from an `InputStream`:

```java
try (InputStream input = ...) {
    MyBean bean = (MyBean) PListIO.read(input);
}
```

Write a Java Bean to a `File`:

```java
File file = ...
MyBean bean = ...
PListIO.write(file, bean);
```

Or to an `OutputStream`:

```java
MyBean bean = ...
try (OutputStream output = ...) {
    PListIO.write(output, bean);
}
```

### Writing `plist` files (serializing Java objects)

Use `PListIO.write(File)` or `PListIO.write(OutputStream)` to write/deserialize Java Bean from a `plist` file.

## Notes

### Java Bean requisites

`java-plist` uses introspection to serialize and deserialize Java Beans.

Java Beans must:

- Have a public empty constructor.
- Have public getters and setters.
- Do **NOT** need to implement `java.io.Serializable`.

### Handling Enum, Instants and binary objects

`java-plist` will:

- Convert Java Beans to plist `<dict>`, with a set of properties.
- Add `<key>net.vieiro.plist.persistence.class</key>` and `<key>net.vieiro.plist.persistence.enum</key>` entries for proper serialization/deserialization of Java Beans.
- Convert Java's `Set` to plist `<array>`.
- Convert Java's `List` to plist `<array>`.
- Convert Java`s `Instant` to plist `<date>`.
- Convert Java's `byte[]` to plist `<data>` using Base64 encoding.
- Convert Java's `Long` (`Integer`) to plist `<integer>` (Java's `Integer` are automatically mapped to `Long` not to lose precision).
- Convert Java`s `Double` (`Float`) to plist `<real>` (Java's `Float` are automatically mapped to `Double` not to lose precision).
- Convert Java`s `Map` to plist `<dict>` (where Map key's are Strings).
- Convert Java's `Double` to plist `<true/>` and `<false/>`.
- Convert Java`s `null` to proper plist empty entries.

## Building and testing

As any other Maven project:

```
$ ./mvnw clean install
```

## Contributions

Contributions that compile, pass the tests, comply with the license and meet the goals are welcome.

## License

java-plist is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0.html). See [LICENSE](./LICENSE) for details.

