package com.telefonica.yang.normalization;

import com.telefonica.yang.normalization.codec.CborYangParser;
import com.telefonica.yang.normalization.codec.JsonYangParser;
import com.telefonica.yang.normalization.codec.XmlYangParser;
import com.telefonica.yang.normalization.codec.YangParser;
import com.telefonica.yang.normalization.encoding.DeterministicAstEncoder;

import java.util.EnumMap;
import java.util.Map;

/**
 * Default normalizer implementing the schema-independent profile described in
 * draft-mendez-opsawg-yang-state-normalization.
 *
 * <p>The following aspects are deliberately out of scope and require a future
 * schema-aware profile: {@code ordered-by user} semantics, default values,
 * {@code identityref} and {@code leafref} resolution, YANG SIDs, and
 * type-dependent value canonicalization.
 *
 * <p>Instances are immutable and safe for concurrent use.
 */
public final class SchemaIndependentNormalizer implements YangNormalizer {

    private final NormalizationOptions options;
    private final Map<SerializationFormat, YangParser> parsers;

    private SchemaIndependentNormalizer(NormalizationOptions options) {
        this.options = options;
        this.parsers = new EnumMap<SerializationFormat, YangParser>(SerializationFormat.class);
        this.parsers.put(SerializationFormat.XML, new XmlYangParser(options));
        this.parsers.put(SerializationFormat.JSON, new JsonYangParser(options));
        this.parsers.put(SerializationFormat.CBOR, new CborYangParser(options));
    }

    public static YangNormalizer create() {
        return create(NormalizationOptions.defaults());
    }

    public static YangNormalizer create(NormalizationOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("Options cannot be null");
        }
        return new SchemaIndependentNormalizer(options);
    }

    public NormalizationOptions options() {
        return options;
    }

    @Override
    public YangNode parse(byte[] input, SerializationFormat format) throws NormalizationException {
        if (format == null) {
            throw new NormalizationException("Serialization format cannot be null");
        }
        return parsers.get(format).parse(input);
    }

    @Override
    public byte[] encode(YangNode node) throws NormalizationException {
        return DeterministicAstEncoder.encode(node);
    }

    @Override
    public byte[] normalize(byte[] input, SerializationFormat format)
            throws NormalizationException {
        return encode(parse(input, format));
    }
}
