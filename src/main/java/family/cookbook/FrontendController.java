package family.cookbook;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// The React app handles its own routes (/recipes/123, /recipes/new, ...) in the browser. When one of
// those URLs is opened directly or reloaded, the request reaches Spring, so answer it with the app's
// index.html and let React Router pick the page.
//
// Not forwarded, so they reach their real handlers:
// - /api/**, so unknown API paths stay 404s instead of returning HTML
// - /assets/**, the JS and CSS files Vite builds into static/assets
// - top-level files such as /favicon.svg (a dot in the first segment)
@Controller
public class FrontendController {

    // Spring only allows ** at the end of a pattern, so exclusions are decided by the first segment
    @GetMapping("/{segment:^(?!api$|assets$)[^.]*$}/**")
    public String forwardToApp() {
        return "forward:/index.html";
    }
}
