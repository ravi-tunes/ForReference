package com.example;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Mojo(name = "scan")
public class SpringBeanScannerMojo extends AbstractMojo {

    private static final Logger logger = LoggerFactory.getLogger(SpringBeanScannerMojo.class);

    @Parameter(property = "modulesRoot", required = true)
    private File modulesRoot;

    @Parameter(property = "propsRoot", required = true)
    private File propsRoot;

    @Parameter(property = "region")
    private String region;

    @Parameter(property = "outputFile")
    private File outputFile;

    public void execute() throws MojoExecutionException {
        try {
            logger.info("Starting Spring Bean Scanner Maven Plugin");

            // Scan modules and parse beans
            List<File> springXmlFiles = scanModules(modulesRoot);
            List<BeanDefinition> beanDefinitions = parseBeans(springXmlFiles);

            // Load properties
            Properties properties = loadProperties(propsRoot, region);

            // Resolve placeholders and generate report
            List<BeanReport> reports = resolvePlaceholders(beanDefinitions, properties);
            generateReport(reports);

            logger.info("Spring Bean Scanner Maven Plugin completed successfully");
        } catch (Exception e) {
            logger.error("Error during execution", e);
            throw new MojoExecutionException("Error during execution", e);
        }
    }

    private List<File> scanModules(File modulesRoot) throws IOException {
        logger.info("Scanning modules in: {}", modulesRoot.getAbsolutePath());
        List<File> springXmlFiles = new ArrayList<>();
        Files.walk(Paths.get(modulesRoot.getAbsolutePath()))
             .filter(Files::isRegularFile)
             .filter(path -> path.toString().endsWith(".xml"))
             .forEach(path -> springXmlFiles.add(path.toFile()));
        logger.info("Found {} Spring XML files", springXmlFiles.size());
        return springXmlFiles;
    }

    private List<BeanDefinition> parseBeans(List<File> springXmlFiles) {
        logger.info("Parsing beans from Spring XML files");
        List<BeanDefinition> beanDefinitions = new ArrayList<>();
        GenericApplicationContext context = new GenericApplicationContext();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
        for (File file : springXmlFiles) {
            reader.loadBeanDefinitions(new FileSystemResource(file));
        }
        String[] beanNames = context.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = context.getBeanDefinition(beanName);
            beanDefinitions.add(beanDefinition);
        }
        logger.info("Parsed {} bean definitions", beanDefinitions.size());
        return beanDefinitions;
    }

    private Properties loadProperties(File propsRoot, String region) throws IOException {
        logger.info("Loading properties from: {}", propsRoot.getAbsolutePath());
        Properties properties = new Properties();
        Files.walk(Paths.get(propsRoot.getAbsolutePath()))
             .filter(Files::isRegularFile)
             .filter(path -> path.toString().endsWith(".properties"))
             .forEach(path -> {
                 try {
                     Properties props = PropertiesLoaderUtils.loadProperties(new FileSystemResource(path.toFile()));
                     properties.putAll(props);
                 } catch (IOException e) {
                     logger.error("Error loading properties file: {}", path, e);
                 }
             });
        logger.info("Loaded properties");
        return properties;
    }

    private List<BeanReport> resolvePlaceholders(List<BeanDefinition> beanDefinitions, Properties properties) {
        logger.info("Resolving placeholders");
        List<BeanReport> reports = new ArrayList<>();
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setProperties(properties);

        for (BeanDefinition beanDefinition : beanDefinitions) {
            String beanId = beanDefinition.getBeanClassName();
            String className = beanDefinition.getBeanClassName();
            BeanReport report = new BeanReport(beanId, className);

            // Resolve placeholders in bean properties
            if (beanDefinition.getPropertyValues() != null) {
                beanDefinition.getPropertyValues().forEach(property -> {
                    String propertyName = property.getName();
                    String propertyValue = (String) property.getValue();
                    if (propertyValue != null && propertyValue.contains("${")) {
                        String resolvedValue = configurer.resolvePlaceholder(propertyValue, properties);
                        report.addProperty(propertyName, propertyValue, resolvedValue);
                    }
                });
            }
            reports.add(report);
        }
        logger.info("Resolved placeholders for {} beans", reports.size());
        return reports;
    }

    private void generateReport(List<BeanReport> reports) throws IOException {
        logger.info("Generating report");
        if (outputFile == null) {
            outputFile = new File("report.csv");
        }

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("beanId,className,propertyName,placeholderKey,finalValue,satisfiedIn\n");
            for (BeanReport report : reports) {
                for (PropertyReport propertyReport : report.getProperties()) {
                    writer.write(String.format("%s,%s,%s,%s,%s,%s\n",
                            report.getBeanId(),
                            report.getClassName(),
                            propertyReport.getPropertyName(),
                            propertyReport.getPlaceholderKey(),
                            propertyReport.getFinalValue(),
                            propertyReport.getSatisfiedIn()));
                }
            }
        }
        logger.info("Report generated at: {}", outputFile.getAbsolutePath());
    }
}
