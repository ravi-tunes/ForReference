package com.example;

import com.opencsv.CSVWriter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "scan")
public class SpringBeanScannerMojo extends AbstractMojo {

    @Parameter(property = "modulesRoot", required = true)
    private File modulesRoot;

    @Parameter(property = "propsRoot", required = true)
    private File propsRoot;

    @Parameter(property = "region")
    private String region;

    @Parameter(property = "environment")
    private String environment;

    @Parameter(property = "outputFile", defaultValue = "${project.build.directory}/bean-report.csv")
    private File outputFile;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    @Override
    public void execute() throws MojoExecutionException {
        try {
            validateInputs();
            getLog().info("Starting Spring Bean Scanner");
            getLog().info("Modules root: " + modulesRoot.getAbsolutePath());
            getLog().info("Properties root: " + propsRoot.getAbsolutePath());
            if (region != null) getLog().info("Region: " + region);
            if (environment != null) getLog().info("Environment: " + environment);

            // 1. Find XML configuration files
            List<Path> xmlFiles = findSpringConfigFiles(modulesRoot.toPath());
            getLog().info("Found " + xmlFiles.size() + " Spring XML files");

            // 2. Load property sources
            List<PropertySource> sources = loadPropertyHierarchy(propsRoot.toPath(), region, environment);
            List<String> sourceNames = sources.stream()
                .map(PropertySource::getName)
                .collect(Collectors.toList());
            getLog().info("Loaded " + sources.size() + " property sources");

            // 3. Parse XMLs and extract bean reports
            List<BeanReport> beanReports = parseBeanDefinitions(xmlFiles, sources);
            getLog().info("Found " + beanReports.size() + " beans with placeholders");

            // 4. Generate CSV report
            generateCsvReport(beanReports, sourceNames, outputFile);
            getLog().info("Report generated: " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            throw new MojoExecutionException("Scan failed: " + e.getMessage(), e);
        }
    }

    private void validateInputs() throws MojoExecutionException {
        if (!modulesRoot.exists() || !modulesRoot.isDirectory()) {
            throw new MojoExecutionException("Invalid modulesRoot: " + modulesRoot.getAbsolutePath());
        }
        if (!propsRoot.exists() || !propsRoot.isDirectory()) {
            throw new MojoExecutionException("Invalid propsRoot: " + propsRoot.getAbsolutePath());
        }
    }

    private List<Path> findSpringConfigFiles(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(this::isSpringConfigFile)
                .collect(Collectors.toList());
        }
    }

    private boolean isSpringConfigFile(Path file) {
        if (!file.getFileName().toString().endsWith(".xml")) return false;
        
        Path parent = file.getParent();
        if (parent == null) return false;
        if (!"spring".equals(parent.getFileName().toString())) return false;
        
        Path grandParent = parent.getParent();
        if (grandParent == null) return false;
        return "resources".equals(grandParent.getFileName().toString());
    }

    private List<PropertySource> loadPropertyHierarchy(Path propsRoot, String region, String env) throws IOException {
        List<PropertySource> sources = new ArrayList<>();
        
        // Load root properties
        loadPropertiesFromDir(propsRoot, sources);
        
        // Load region properties if specified
        if (region != null) {
            Path regionPath = propsRoot;
            for (String part : region.split("[/\\\\]")) {
                regionPath = regionPath.resolve(part);
                loadPropertiesFromDir(regionPath, sources);
            }
            
            // Load environment properties if specified
            if (env != null) {
                Path envPath = regionPath.resolve(env);
                loadPropertiesFromDir(envPath, sources);
            }
        }
        
        return sources;
    }

    private void loadPropertiesFromDir(Path dir, List<PropertySource> sources) throws IOException {
        if (!Files.isDirectory(dir)) return;
        
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".properties"))
                 .sorted()
                 .forEach(p -> {
                     try {
                         String relativePath = propsRoot.toAbsolutePath()
                             .relativize(p.toAbsolutePath())
                             .toString();
                         Properties props = loadProperties(p);
                         sources.add(new PropertySource(relativePath, props));
                         getLog().debug("Loaded properties: " + relativePath);
                     } catch (IOException e) {
                         getLog().warn("Failed to load properties: " + p + " - " + e.getMessage());
                     }
                 });
        }
    }

    private Properties loadProperties(Path path) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        }
        return props;
    }

    private List<BeanReport> parseBeanDefinitions(List<Path> xmlFiles, List<PropertySource> sources) {
        List<BeanReport> reports = new ArrayList<>();
        XMLInputFactory factory = createSecureXmlFactory();
        
        for (Path xml : xmlFiles) {
            try {
                parseXmlFile(xml, factory, sources, reports);
            } catch (Exception e) {
                getLog().warn("Skipping invalid XML: " + xml + " - " + e.getMessage());
            }
        }
        return reports;
    }

    private XMLInputFactory createSecureXmlFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        return factory;
    }

    private void parseXmlFile(Path xml, XMLInputFactory factory, 
                             List<PropertySource> sources, List<BeanReport> reports) 
        throws Exception {
        
        try (InputStream in = Files.newInputStream(xml)) {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            BeanReport currentBean = null;
            
            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String tag = reader.getLocalName();
                        if ("bean".equals(tag)) {
                            currentBean = createBeanReport(reader);
                        } else if (currentBean != null && "property".equals(tag)) {
                            processProperty(reader, currentBean, sources);
                        }
                        break;
                        
                    case XMLStreamConstants.END_ELEMENT:
                        if ("bean".equals(reader.getLocalName()) && currentBean != null) {
                            if (!currentBean.getProperties().isEmpty()) {
                                reports.add(currentBean);
                            }
                            currentBean = null;
                        }
                        break;
                }
            }
            reader.close();
        }
    }

    private BeanReport createBeanReport(XMLStreamReader reader) {
        String id = getAttribute(reader, "id");
        if (id == null || id.isEmpty()) {
            id = getAttribute(reader, "name");
        }
        String className = getAttribute(reader, "class");
        return new BeanReport(id, className);
    }

    private void processProperty(XMLStreamReader reader, BeanReport bean, 
                                List<PropertySource> sources) {
        String propName = getAttribute(reader, "name");
        String value = getAttribute(reader, "value");
        
        if (propName == null || value == null) return;
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        while (matcher.find()) {
            String key = matcher.group(1);
            Map<String, String> sourceValues = new LinkedHashMap<>();
            
            for (PropertySource source : sources) {
                sourceValues.put(source.getName(), 
                    source.getProperties().getProperty(key));
            }
            
            bean.addProperty(new PropertyReport(propName, key, sourceValues));
        }
    }

    private String getAttribute(XMLStreamReader reader, String name) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            if (name.equals(reader.getAttributeLocalName(i))) {
                return reader.getAttributeValue(i);
            }
        }
        return null;
    }

    private void generateCsvReport(List<BeanReport> beans, List<String> sources, 
                                  File output) throws IOException {
        // Create parent directories if needed
        if (!output.getParentFile().exists()) {
            output.getParentFile().mkdirs();
        }
        
        try (CSVWriter writer = new CSVWriter(
            new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8))) {
            
            // Create header
            List<String> headers = new ArrayList<>();
            headers.add("beanId");
            headers.add("className");
            headers.add("propertyName");
            headers.add("placeholderKey");
            headers.addAll(sources);
            headers.add("finalValue");
            headers.add("satisfiedIn");
            writer.writeNext(headers.toArray(new String[0]));
            
            // Write data rows
            for (BeanReport bean : beans) {
                for (PropertyReport prop : bean.getProperties()) {
                    List<String> row = new ArrayList<>();
                    row.add(bean.getBeanId());
                    row.add(bean.getClassName());
                    row.add(prop.getPropertyName());
                    row.add(prop.getPlaceholderKey());
                    
                    // Add values from each source
                    for (String source : sources) {
                        String value = prop.getValuesPerSource().get(source);
                        row.add(value != null ? value : "");
                    }
                    
                    row.add(prop.getFinalValue() != null ? prop.getFinalValue() : "");
                    row.add(prop.getSatisfiedIn());
                    writer.writeNext(row.toArray(new String[0]));
                }
            }
        }
    }

    private static class PropertySource {
        private final String name;
        private final Properties properties;
        
        PropertySource(String name, Properties properties) {
            this.name = name;
            this.properties = properties;
        }
        
        String getName() { return name; }
        Properties getProperties() { return properties; }
    }
}