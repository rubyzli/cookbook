package family.cookbook.importer;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Locale;

@Component
public class HttpPageFetcher implements PageFetcher {

    static final int MAX_PAGE_BYTES = 5 * 1024 * 1024;
    static final int MAX_IMAGE_BYTES = 15 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 5;
    // Says honestly what is asking; "compatible" is how well-behaved bots identify themselves
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; FamilyCookbook/1.0; recipe import)";

    // Redirects are followed by hand, so every hop goes through the public-address check
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public Download fetchPage(URI uri, String acceptLanguage) {
        Download page = get(uri, "text/html,application/xhtml+xml;q=0.9,*/*;q=0.5", acceptLanguage, MAX_PAGE_BYTES);
        String type = page.contentType() == null ? "" : page.contentType().toLowerCase(Locale.ROOT);
        if (!type.isEmpty() && !type.contains("html")) {
            throw new RecipeImportException(HttpStatus.UNPROCESSABLE_CONTENT, "NOT_A_PAGE", "That link isn't a web page");
        }
        return page;
    }

    @Override
    public Download fetchImage(URI uri) {
        return get(uri, "image/*", null, MAX_IMAGE_BYTES);
    }

    private Download get(URI start, String accept, String acceptLanguage, int maxBytes) {
        URI current = start;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            PublicAddresses.requirePublic(current);
            HttpRequest.Builder request = HttpRequest.newBuilder(current)
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", accept);
            if (acceptLanguage != null && !acceptLanguage.isBlank()) request.header("Accept-Language", acceptLanguage);
            HttpResponse<InputStream> response = send(request.GET().build());
            int status = response.statusCode();
            if (status >= 300 && status < 400 && response.headers().firstValue("Location").isPresent()) {
                close(response);
                current = PublicAddresses.parseWebUrl(current.resolve(response.headers().firstValue("Location").get()).toString());
                continue;
            }
            if (status >= 200 && status < 300) {
                return new Download(current, read(response, maxBytes), response.headers().firstValue("Content-Type").orElse(null));
            }
            close(response);
            throw failure(status);
        }
        throw new RecipeImportException(HttpStatus.BAD_GATEWAY, "SITE_ERROR", "The site redirected too many times");
    }

    private HttpResponse<InputStream> send(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (HttpTimeoutException e) {
            throw new RecipeImportException(HttpStatus.GATEWAY_TIMEOUT, "TIMEOUT", "The site took too long to answer");
        } catch (IOException e) {
            throw new RecipeImportException(HttpStatus.BAD_GATEWAY, "UNREACHABLE", "The site can't be reached");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RecipeImportException(HttpStatus.BAD_GATEWAY, "UNREACHABLE", "The download was interrupted");
        }
    }

    private static byte[] read(HttpResponse<InputStream> response, int maxBytes) {
        try (InputStream in = response.body()) {
            byte[] body = in.readNBytes(maxBytes + 1);
            if (body.length > maxBytes) {
                throw new RecipeImportException(HttpStatus.BAD_GATEWAY, "TOO_LARGE", "The page is too large to import");
            }
            return body;
        } catch (IOException e) {
            throw new RecipeImportException(HttpStatus.BAD_GATEWAY, "UNREACHABLE", "The download broke off");
        }
    }

    private static void close(HttpResponse<InputStream> response) {
        try {
            response.body().close();
        } catch (IOException ignored) {
            // Nothing more to read from it anyway
        }
    }

    static RecipeImportException failure(int status) {
        if (status == 401 || status == 403 || status == 429 || status == 503) {
            return new RecipeImportException(HttpStatus.BAD_GATEWAY, "BLOCKED",
                    "The site refused the download (status " + status + ")");
        }
        if (status == 404 || status == 410) {
            return new RecipeImportException(HttpStatus.BAD_GATEWAY, "PAGE_NOT_FOUND", "The page doesn't exist (status " + status + ")");
        }
        return new RecipeImportException(HttpStatus.BAD_GATEWAY, "SITE_ERROR", "The site answered with an error (status " + status + ")");
    }
}
