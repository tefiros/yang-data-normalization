package com.telefonica.yang.normalization.tool;

import com.telefonica.yang.normalization.NormalizationException;
import com.telefonica.yang.normalization.SchemaIndependentNormalizer;
import com.telefonica.yang.normalization.SerializationFormat;
import com.telefonica.yang.normalization.YangNode;
import com.telefonica.yang.normalization.YangNormalizer;
import com.telefonica.yang.normalization.YangTreeFormatter;
import com.telefonica.yang.normalization.encoding.Fingerprint;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

/**
 * Command line helper that normalizes documents and reports their fingerprint.
 *
 * <pre>
 * normalize &lt;xml|json|cbor&gt; &lt;file&gt; [more formats and files...]
 * </pre>
 *
 * <p>When several documents are supplied, their fingerprints are compared, which
 * is how the cross-format examples of the draft can be checked.
 */
public final class NormalizeCli {

    private NormalizeCli() {
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length % 2 != 0) {
            System.err.println("Usage: normalize <xml|json|cbor> <file> [<format> <file>...]");
            System.exit(2);
            return;
        }

        try {
            YangNormalizer normalizer = SchemaIndependentNormalizer.create();
            String reference = null;

            for (int i = 0; i < args.length; i += 2) {
                SerializationFormat format = SerializationFormat.fromName(args[i]);
                byte[] input = readFile(new File(args[i + 1]));

                YangNode tree = normalizer.parse(input, format);
                byte[] normalized = normalizer.encode(tree);
                String digest = Fingerprint.toHex(Fingerprint.sha256(normalized));

                System.out.println("File      : " + args[i + 1]);
                System.out.println("Format    : " + format);
                System.out.println("Normalized: " + normalized.length + " bytes");
                System.out.println("SHA-256   : " + digest);
                System.out.println(YangTreeFormatter.format(tree));

                if (reference == null) {
                    reference = digest;
                } else if (!reference.equals(digest)) {
                    System.err.println("Documents are NOT equivalent");
                    System.exit(1);
                    return;
                }
            }

            if (args.length > 2) {
                System.out.println("All documents are equivalent");
            }
        } catch (NormalizationException e) {
            System.err.println("Normalization failed: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Unable to read the input file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static byte[] readFile(File file) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        InputStream input = new FileInputStream(file);
        try {
            byte[] chunk = new byte[8192];
            int read;
            while ((read = input.read(chunk)) > 0) {
                buffer.write(chunk, 0, read);
            }
        } finally {
            input.close();
        }
        return buffer.toByteArray();
    }
}
