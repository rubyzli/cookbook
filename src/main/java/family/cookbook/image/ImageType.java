package family.cookbook.image;

import java.util.Arrays;
import java.util.Optional;

// The photo formats browsers can show, recognized by their first bytes rather than by the file
// name or the type the browser claims, which are easy to get wrong or fake
enum ImageType {
    JPEG("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}),
    GIF("image/gif", new byte[] {'G', 'I', 'F', '8'}),
    WEBP("image/webp", null);

    final String contentType;
    private final byte[] signature;

    ImageType(String contentType, byte[] signature) {
        this.contentType = contentType;
        this.signature = signature;
    }

    static Optional<ImageType> detect(byte[] head) {
        // WebP: "RIFF" <4 bytes of size> "WEBP"
        if (head.length >= 12 && startsWith(head, "RIFF".getBytes(), 0) && startsWith(head, "WEBP".getBytes(), 8)) {
            return Optional.of(WEBP);
        }
        return Arrays.stream(values())
                .filter(type -> type.signature != null && startsWith(head, type.signature, 0))
                .findFirst();
    }

    private static boolean startsWith(byte[] data, byte[] prefix, int offset) {
        if (data.length < offset + prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) return false;
        }
        return true;
    }
}
