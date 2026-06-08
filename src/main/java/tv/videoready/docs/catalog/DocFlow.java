package tv.videoready.docs.catalog;

/**
 * One browsable documentation page backed by a Markdown file on the classpath ({@code documentation/}).
 */
public class DocFlow {

    private final String slug;
    private final String title;
    private final String classpathResourceFileName;
    private final String summary;
    private final DocCategory category;

    public DocFlow(String slug, String title, String classpathResourceFileName, String summary) {
        this(slug, title, classpathResourceFileName, summary, DocCategory.FLOW);
    }

    public DocFlow(String slug, String title, String classpathResourceFileName, String summary, DocCategory category) {
        this.slug = slug;
        this.title = title;
        this.classpathResourceFileName = classpathResourceFileName;
        this.summary = summary;
        this.category = category != null ? category : DocCategory.FLOW;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getClasspathResourceFileName() {
        return classpathResourceFileName;
    }

    public String getSummary() {
        return summary;
    }

    public DocCategory getCategory() {
        return category;
    }
}
