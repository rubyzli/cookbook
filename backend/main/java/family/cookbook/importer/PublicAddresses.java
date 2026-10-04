package family.cookbook.importer;

import org.springframework.http.HttpStatus;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

// Imports fetch whatever address someone types in, from the server. On a deployed server that must
// not reach things only the server can see (the database, cloud metadata at 169.254.169.254, the
// home router), so only public internet addresses on the usual web ports are allowed.
final class PublicAddresses {

    private static final Set<Integer> WEB_PORTS = Set.of(-1, 80, 443);

    private PublicAddresses() {
    }

    // A typed-in address as a URI, if it's a plain http(s) web address
    static URI parseWebUrl(String raw) {
        try {
            URI uri = new URI(raw.strip());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null) {
                throw invalid();
            }
            return uri;
        } catch (URISyntaxException e) {
            throw invalid();
        }
    }

    // Checked again for every redirect, since a public page can redirect to a private address
    static void requirePublic(URI uri) {
        if (!WEB_PORTS.contains(uri.getPort())) {
            throw notAllowed();
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(uri.getHost());
        } catch (UnknownHostException e) {
            throw new RecipeImportException(HttpStatus.BAD_GATEWAY, "UNKNOWN_SITE", "The site " + uri.getHost() + " couldn't be found");
        }
        for (InetAddress address : addresses) {
            if (!isPublic(address)) throw notAllowed();
        }
    }

    static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] b = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = b[0] & 0xFF;
            int second = b[1] & 0xFF;
            // 0.0.0.0/8, 100.64.0.0/10 (carrier-grade NAT), 192.0.0.0/24, 198.18.0.0/15 (benchmarking), 240.0.0.0/4
            return !(first == 0 || (first == 100 && second >= 64 && second < 128) || (first == 192 && second == 0 && (b[2] & 0xFF) == 0)
                    || (first == 198 && (second == 18 || second == 19)) || first >= 240);
        }
        if (address instanceof Inet6Address) {
            // fc00::/7 unique local addresses, and IPv4-mapped addresses judged as IPv4
            if ((b[0] & 0xFE) == 0xFC) return false;
            boolean mapped = b[10] == (byte) 0xFF && b[11] == (byte) 0xFF;
            for (int i = 0; i < 10 && mapped; i++) mapped = b[i] == 0;
            if (mapped) {
                try {
                    return isPublic(InetAddress.getByAddress(new byte[] {b[12], b[13], b[14], b[15]}));
                } catch (UnknownHostException e) {
                    return false;
                }
            }
        }
        return true;
    }

    private static RecipeImportException invalid() {
        return new RecipeImportException(HttpStatus.BAD_REQUEST, "INVALID_URL",
                "Enter a web address starting with http:// or https://");
    }

    private static RecipeImportException notAllowed() {
        return new RecipeImportException(HttpStatus.BAD_REQUEST, "ADDRESS_NOT_ALLOWED",
                "Only public web pages can be imported");
    }
}
