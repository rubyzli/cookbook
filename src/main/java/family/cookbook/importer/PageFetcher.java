package family.cookbook.importer;

import java.net.URI;

// Downloads pages and photos for importing. HttpPageFetcher does it for real; tests use a fake.
public interface PageFetcher {

    // finalUri: where the page ended up after redirects
    record Download(URI finalUri, byte[] body, String contentType) {
    }

    Download fetchPage(URI uri, String acceptLanguage);

    Download fetchImage(URI uri);
}
