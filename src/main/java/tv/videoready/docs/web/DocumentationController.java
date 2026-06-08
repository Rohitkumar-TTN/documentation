package tv.videoready.docs.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tv.videoready.docs.catalog.DocCategory;
import tv.videoready.docs.catalog.DocFlow;
import tv.videoready.docs.catalog.DocumentationCatalog;
import tv.videoready.docs.service.MarkdownDocumentationService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Serves the engineering documentation hub as HTML (Thymeleaf layout + rendered Markdown body).
 */
@Controller
public class DocumentationController {

    private final MarkdownDocumentationService markdownDocumentationService;

    @Autowired
    public DocumentationController(MarkdownDocumentationService markdownDocumentationService) {
        this.markdownDocumentationService = markdownDocumentationService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageTitle", "Tata Play — engineering docs hub");
        enrichModelForChrome(model, null, Boolean.TRUE);
        return "index";
    }

    /**
     * @param slug logical id from {@link DocumentationCatalog}, e.g. {@code onboarding}
     */
    @GetMapping("/flows/{slug}")
    public String flow(@PathVariable("slug") String slug, Model model, HttpServletResponse response) {
        Optional<DocFlow> flow = DocumentationCatalog.findBySlug(slug);
        if (!flow.isPresent()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("pageTitle", "Not found — engineering docs hub");
            enrichModelForChrome(model, null, Boolean.FALSE);
            model.addAttribute("unknownSlug", slug);
            return "notFound";
        }
        DocFlow doc = flow.get();
        try {
            String html = markdownDocumentationService.renderDocumentationFile(doc.getClasspathResourceFileName());
            model.addAttribute("pageTitle", doc.getTitle() + " — engineering docs hub");
            model.addAttribute("htmlContent", html);
            enrichModelForChrome(model, doc.getSlug(), Boolean.FALSE);
            return "doc";
        } catch (IOException ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            model.addAttribute("pageTitle", "Error — engineering docs hub");
            enrichModelForChrome(model, null, Boolean.FALSE);
            model.addAttribute("errorMessage", ex.getMessage());
            return "error";
        }
    }

    @GetMapping("/flows")
    public String flowsRedirect() {
        return "redirect:/";
    }

    /**
     * Shared layout: nav highlights, landing cards, full catalog for future use.
     */
    private void enrichModelForChrome(Model model, String currentSlug, Boolean onHome) {
        boolean home = Boolean.TRUE.equals(onHome);
        model.addAttribute("flows", DocumentationCatalog.all());
        model.addAttribute("onHome", home);
        model.addAttribute("currentSlug", currentSlug);
        model.addAttribute("navHomeActive", home);
        Optional<DocCategory> cat = Optional.ofNullable(currentSlug).flatMap(DocumentationCatalog::categoryOfSlug);
        model.addAttribute("navFlowsActive", !home && cat.orElse(null) == DocCategory.FLOW);
        model.addAttribute("navApisActive", !home && cat.orElse(null) == DocCategory.API);
        DocumentationCatalog.findBySlug("engineering-flows").ifPresent(f -> model.addAttribute("flowsLanding", f));
        DocumentationCatalog.findBySlug("engineering-apis").ifPresent(f -> model.addAttribute("apisLanding", f));
    }
}
