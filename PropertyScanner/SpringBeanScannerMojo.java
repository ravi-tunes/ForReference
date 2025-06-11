package com.example;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maven Mojo to scan Spring XML beans, resolve property placeholders via a layered .properties hierarchy,
 * and produce a CSV report.
 */
@Mojo(name = "scan")
public class SpringBeanScannerMojo extends AbstractMojo {

    @Parameter(property = "modulesRoot", required = true)
    private File modulesRoot;

    @Parameter(property = "propsRoot", required = true)
    private File propsRoot;

    @Parameter(property = "region")
    private String region;

    @Parameter(property = "outputFile", defaultValue = "${project.build.directory}/bean-report.csv")
    private File outputFile;

    // Pattern to extract keys inside ${...}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    @Override
    public void execute() throws MojoExecutionException {
        try {
            getLog().info("Modules root: " + modulesRoot.getAbsolutePath());
            getLog().info("Properties root: " + propsRoot.getAbsolutePath());
            if (region != null) {
                getLog().info("Region: " + region);
            }

            // 1. Find all Spring XMLs under **/resources/spring/
            List<Path> xmlFiles = scanXmlFiles(modulesRoot.toPath());
            getLog().info("Found " + xmlFiles.size() + " XML files.");

            // 2. Load .properties hierarchy
            List<PropertySource> sources = loadPropertySources(propsRoot.toPath(), region);
            List<String> sourceNames = sources.stream()
                    .map(PropertySource::getName)
                    .collect(Collectors.toList());
            getLog().info("Loaded property sources: " + sourceNames);

            // 3. Parse beans and collect reports
            List<BeanReport> beanReports = new ArrayList<>();
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            for (Path xml : xmlFiles) {
                getLog().info("Parsing " + xml);
                Document doc = db.parse(xml.toFile());
                NodeList beans = doc.getDocumentElement().getElementsByTagName("bean");
                for (int i = 0; i < beans.getLength(); i++) {
                    Element bean = (Element) beans.item(i);
                    String beanId = bean.getAttribute("id");
                    if (beanId.isEmpty()) {
                        beanId = bean.getAttribute("name");
                    }
                    String className = bean.getAttribute("class");
                    BeanReport report = new BeanReport(beanId, className);

                    NodeList props = bean.getElementsByTagName("property");
                    for (int j = 0; j < props.getLength(); j++) {
                        Element p = (Element) props.item(j);
                        String propName = p.getAttribute("name");
                        String value = p.getAttribute("value");
                        if (value != null) {
                            Matcher m = PLACEHOLDER_PATTERN.matcher(value);
                            while (m.find()) {
                                String key = m.group(1);
                                // Build a map of each source -> its value for this key
                                Map<String,String> map = new LinkedHashMap<>();
                                for (PropertySource src : sources) {
                                    map.put(src.getName(), src.getProperties().getProperty(key));
                                }
                                // Create and attach property report
                                PropertyReport pr = new PropertyReport(propName, key, map);
                                report.addProperty(pr);
                            }
                        }
                    }
                    if (!report.getProperties().isEmpty()) {
                        beanReports.add(report);
                    }
                }
            }

            // 4. Write CSV
            writeCsv(beanReports, sourceNames, outputFile);
            getLog().info("Report written to " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            throw new MojoExecutionException("Scan failed", e);
        }
    }

    private List<Path> scanXmlFiles(Path root) throws IOException {
        try (Stream<Path> s = Files.find(root, Integer.MAX_VALUE,
                (path, attrs) -> path.toString().endsWith(".xml") &&
                        path.toString().contains("/resources/spring/"))) {
            return s.filter(p -> p.toString().endsWith(".xml")
                    && p.toString().contains(File.separator + "resources" + File.separator + "spring" + File.separator))
                    .collect(Collectors.toList());
        }
    }

    private List<PropertySource> loadPropertySources(Path propsRoot, String region) throws IOException {
        List<PropertySource> list = new ArrayList<>();
        // Global default
        Path global = propsRoot.resolve("app.default.properties");
        if (Files.exists(global)) {
            list.add(new PropertySource("app.default", load(propsRoot.resolve("app.default.properties"))));
        }
        // Regional defaults
        if (region != null && !region.isEmpty()) {
            Path cum = propsRoot;
            for (String part : region.split("[/\\\\]")) {
                cum = cum.resolve(part);
                Path regDef = cum.resolve("applied.default.properties");
                if (Files.exists(regDef)) {
                    list.add(new PropertySource(part + "/applied.default", load(regDef)));
                }
            }
            // Leaf overrides
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(cum, "*.properties")) {
                for (Path p : ds) {
                    if (!p.getFileName().toString().equals("applied.default.properties")) {
                        list.add(new PropertySource(cum.getFileName() + "/" + p.getFileName(), load(p)));
                    }
                }
            }
        }
        return list;
    }

    private Properties load(Path path) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        }
        return props;
    }

    private void writeCsv(List<BeanReport> beans,
                          List<String> sources,
                          File out) throws IOException {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(out.toPath()), StandardCharsets.UTF_8))) {
            // Header
            List<String> hdr = new ArrayList<>(Arrays.asList("beanId","className","propertyName","placeholder"));
            hdr.addAll(sources);
            hdr.addAll(Arrays.asList("finalValue","satisfiedIn"));
            pw.println(toCsv(hdr));

            // Rows
            for (BeanReport br : beans) {
                for (PropertyReport pr : br.getProperties()) {
                    List<String> cols = new ArrayList<>();
                    cols.add(br.getBeanId());
                    cols.add(br.getClassName());
                    cols.add(pr.getPropertyName());
                    cols.add(pr.getPlaceholderKey());
                    for (String src : sources) {
                        String v = pr.getValuesPerSource().get(src);
                        cols.add(v == null ? "" : v);
                    }
                    cols.add(pr.getFinalValue() == null ? "" : pr.getFinalValue());
                    cols.add(pr.getSatisfiedIn());
                    pw.println(toCsv(cols));
                }
            }
        }
    }

    private String toCsv(List<String> cols) {
        return cols.stream()
                .map(s -> "\"" + s.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(","));
    }

    /** Simple container for a named Properties layer. */
    private static class PropertySource {
        private final String name;
        private final Properties props;
        PropertySource(String name, Properties props) {
            this.name = name;
            this.props = props;
        }
        String getName() { return name; }
        Properties getProperties() { return props; }
    }
}