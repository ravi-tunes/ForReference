package com.mycompany.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mojo(name = "validate-properties")
public class PropertyValidatorMojo extends AbstractMojo {

    private static final String[] REGIONS = {"APAC", "EMEA", "AMER"};
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * The path to the code folder to be scanned.
     */
    @Parameter(property = "codePath", required = true)
    private File codePath;

    /**
     * The path to the common configuration folder.
     */
    @Parameter(property = "commonConfigPath", required = true)
    private File commonConfigPath;
    
    // Data class to hold information about a discovered placeholder
    private static class PropertyPlaceholderInfo {
        final String beanId;
        final String propertyName;
        final String placeholder; // e.g., "${db.password}"
        final String propertyKey; // e.g., "db.password"
        final String sourceFile;
        final Set<String> unsatisfiedEnvironments = new HashSet<>();

        PropertyPlaceholderInfo(String beanId, String propertyName, String placeholder, String propertyKey, String sourceFile) {
            this.beanId = beanId;
            this.propertyName = propertyName;
            this.placeholder = placeholder;
            this.propertyKey = propertyKey;
            this.sourceFile = sourceFile;
        }

        @Override
        public String toString() {
            return String.format("BeanID: %-30s | Property: %-25s | Placeholder: %-30s | Unsatisfied In: %s",
                    beanId, propertyName, placeholder, String.join(", ", unsatisfiedEnvironments));
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        // --- 1. Validation of Input Paths ---
        if (!codePath.exists() || !codePath.isDirectory()) {
            throw new MojoExecutionException("codePath does not exist or is not a directory: " + codePath);
        }
        if (!commonConfigPath.exists() || !commonConfigPath.isDirectory()) {
            throw new MojoExecutionException("commonConfigPath does not exist or is not a directory: " + commonConfigPath);
        }

        getLog().info("Starting Property Validation...");
        getLog().info("Code Path: " + codePath.getAbsolutePath());
        getLog().info("Common Config Path: " + commonConfigPath.getAbsolutePath());

        try {
            // --- 2. Discover all placeholders in Spring XMLs ---
            List<PropertyPlaceholderInfo> placeholders = findPlaceholdersInXmls();
            if (placeholders.isEmpty()) {
                getLog().info("No property placeholders found in Spring XML files. Nothing to validate.");
                return;
            }
            getLog().info("Found " + placeholders.size() + " unique property placeholders to validate.");

            // --- 3. Discover all environment hierarchies ---
            Set<String> environments = discoverEnvironments();
            getLog().info("Found " + environments.size() + " environments to check: " + environments);

            // --- 4. Loop through each environment and validate ---
            for (String env : environments) {
                getLog().debug("Validating environment: " + env);
                Properties consolidatedProps = loadPropertiesForEnv(env);

                for (PropertyPlaceholderInfo p : placeholders) {
                    if (!consolidatedProps.containsKey(p.propertyKey)) {
                        p.unsatisfiedEnvironments.add(env.equals("") ? "root" : env);
                    }
                }
            }
            
            // --- 5. Generate the final report ---
            generateReport(placeholders);

        } catch (IOException e) {
            throw new MojoExecutionException("Failed during property validation", e);
        }
    }

    private List<PropertyPlaceholderInfo> findPlaceholdersInXmls() throws IOException {
        Map<String, PropertyPlaceholderInfo> uniquePlaceholders = new HashMap<>();
        Collection<File> xmlFiles = FileUtils.listFiles(codePath, new String[]{"xml"}, true);
        getLog().info("Scanning " + xmlFiles.size() + " XML files...");

        for (File xmlFile : xmlFiles) {
            try {
                DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
                XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
                // Disable validation to speed up and avoid DTD/XSD lookup issues
                reader.setValidating(false); 
                reader.loadBeanDefinitions(new FileSystemResource(xmlFile));

                for (String beanName : factory.getBeanDefinitionNames()) {
                    BeanDefinition beanDef = factory.getBeanDefinition(beanName);
                    beanDef.getPropertyValues().getPropertyValueList().forEach(pv -> {
                        if (pv.getValue() instanceof TypedStringValue) {
                            String value = ((TypedStringValue) pv.getValue()).getValue();
                            if (value != null) {
                                Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
                                while (matcher.find()) {
                                    String placeholder = matcher.group(0); // e.g., ${db.host}
                                    String key = matcher.group(1);       // e.g., db.host
                                    String uniqueId = beanName + ":" + pv.getName() + ":" + placeholder;
                                    
                                    uniquePlaceholders.putIfAbsent(uniqueId, new PropertyPlaceholderInfo(
                                        beanName,
                                        pv.getName(),
                                        placeholder,
                                        key,
                                        xmlFile.getName()
                                    ));
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                // Log and continue, as some XMLs might not be Spring beans
                getLog().warn("Could not parse file as Spring XML: " + xmlFile.getAbsolutePath() + ". Error: " + e.getMessage());
            }
        }
        return new ArrayList<>(uniquePlaceholders.values());
    }

    private Set<String> discoverEnvironments() {
        Set<String> environments = new HashSet<>();
        // Add root as a default environment
        environments.add(""); 

        for (File baseDir : Arrays.asList(commonConfigPath, codePath)) {
            for (String region : REGIONS) {
                File regionDir = new File(baseDir, region);
                if (regionDir.exists() && regionDir.isDirectory()) {
                    environments.add(region); // Add region-only
                    File[] envDirs = regionDir.listFiles(File::isDirectory);
                    if (envDirs != null) {
                        for (File envDir : envDirs) {
                            environments.add(region + "/" + envDir.getName());
                        }
                    }
                }
            }
        }
        return environments;
    }

    private Properties loadPropertiesForEnv(String env) throws IOException {
        Properties props = new Properties();
        String[] parts = env.split("/");
        String region = parts.length > 0 ? parts[0] : "";
        String subEnv = parts.length > 1 ? parts[1] : "";

        // 1. Common Config Path Hierarchy
        loadPropertiesFromDir(props, commonConfigPath); // Root
        if (!region.isEmpty()) loadPropertiesFromDir(props, new File(commonConfigPath, region));
        if (!subEnv.isEmpty()) loadPropertiesFromDir(props, new File(commonConfigPath, region + "/" + subEnv));
        
        // 2. Code Path Hierarchy
        loadPropertiesFromDir(props, codePath); // Root
        if (!region.isEmpty()) loadPropertiesFromDir(props, new File(codePath, region));
        if (!subEnv.isEmpty()) loadPropertiesFromDir(props, new File(codePath, region + "/" + subEnv));

        return props;
    }

    private void loadPropertiesFromDir(Properties props, File dir) throws IOException {
        if (dir.exists() && dir.isDirectory()) {
            Collection<File> propFiles = FileUtils.listFiles(dir, new String[]{"properties"}, false);
            for (File propFile : propFiles) {
                try (FileInputStream fis = new FileInputStream(propFile)) {
                    props.load(fis);
                }
            }
        }
    }
    
    private void generateReport(List<PropertyPlaceholderInfo> placeholders) {
        List<PropertyPlaceholderInfo> unresolved = placeholders.stream()
                .filter(p -> !p.unsatisfiedEnvironments.isEmpty())
                .sorted(Comparator.comparing(p -> p.beanId))
                .collect(Collectors.toList());

        if (unresolved.isEmpty()) {
            getLog().info("-------------------------------------------------------");
            getLog().info("SUCCESS: All properties were satisfied in all environments.");
            getLog().info("-------------------------------------------------------");
        } else {
            getLog().error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            getLog().error("VALIDATION FAILED: Found unresolved properties!");
            getLog().error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            getLog().error(String.format("%nFound %d properties with missing configuration:%n", unresolved.size()));
            
            for(PropertyPlaceholderInfo p : unresolved) {
                getLog().error(p.toString());
            }

            // To fail the build
            throw new RuntimeException("Property validation failed. See log for details.");
        }
    }
}