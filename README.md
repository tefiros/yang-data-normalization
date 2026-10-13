# YANG Data Normalization

Java 8 library that implements the schema-independent normalization procedure
described by
[`draft-mendez-opsawg-yang-state-normalization`](https://github.com/ana-mp26/yang-state-normalization).
Equivalent XML, RFC 7951 JSON, and RFC 9254 CBOR representations are converted
to a common abstract syntax tree (AST), deterministically ordered, and encoded
as an unambiguous byte sequence suitable for comparison, hashing, or signing.

## Usage

```java
YangNormalizer normalizer = SchemaIndependentNormalizer.create();

byte[] normalized = normalizer.normalizeJson(
        "{\"interface\":{\"name\":\"eth0\",\"enabled\":true}}");
String fingerprint = Fingerprint.toHex(Fingerprint.sha256(normalized));
```

The AST can be inspected separately:

```java
YangNode tree = normalizer.parse(jsonBytes, SerializationFormat.JSON);
System.out.println(YangTreeFormatter.format(tree));
```

Run the command-line comparison tool with pairs of formats and files:

```text
gradlew run --args="xml state.xml json state.json cbor state.cbor"
```

It exits with status `0` when every input has the same SHA-256 fingerprint and
status `1` when inputs differ or normalization fails.

For a guided demonstration that prints the input documents, the AST, the
normalized size, and the fingerprints, run:

```text
gradlew runDemo
```

The demo is implemented in
[`SimpleDemo.java`](src/main/java/com/telefonica/yang/normalization/tool/SimpleDemo.java).

## Normalization model

Each AST node contains an identifier and either a textual leaf value or child
nodes. Arrays become repeated sibling nodes with the same identifier. At every
level, nodes are sorted by their complete deterministic encoding, so sibling,
list-entry, and leaf-list order does not affect the result while duplicates
remain significant.

The normalized binary representation starts with the version marker
`YANG-NORM-01`. Node identifiers and values are UTF-8 and length-prefixed to
prevent structural ambiguity.

By default, serialization prefixes are removed to align XML local names with
JSON and CBOR member names. XML leaf whitespace is preserved because its
significance cannot be established without a YANG schema. Both behaviors can
be changed with `NormalizationOptions`.

## Scope and limitations

The implementation follows the draft's schema-independent profile. It does not
resolve YANG SIDs or schema-dependent semantics such as defaults,
`identityref`, `leafref`, type-specific lexical canonicalization, or
`ordered-by user`.

## Build and test

```text
gradlew clean test
```

## License

This project is licensed under the Apache License, Version 2.0. See
[`LICENSE`](LICENSE) for the full text.
