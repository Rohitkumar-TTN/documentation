package tv.videoready.docs.service;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Service
public class MarkdownDocumentationService {

    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    public MarkdownDocumentationService() {
        Iterable<Extension> extensions = Collections.singletonList(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.htmlRenderer = HtmlRenderer.builder().extensions(extensions).build();
    }

    /**
     * Loads {@code documentation/<fileName>} from the classpath and renders GitHub-flavoured Markdown to HTML.
     */
    public String renderDocumentationFile(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource("documentation/" + fileName);
        if (!resource.exists()) {
            throw new IOException("Missing classpath resource: documentation/" + fileName
                    + " (run Gradle processResources so ../docs is copied).");
        }
        String markdown;
        try (InputStream in = resource.getInputStream()) {
            markdown = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
        Node document = parser.parse(markdown);
        return htmlRenderer.render(document);
    }
}
