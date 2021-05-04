package com.github.bohnman;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.SelectorUtils;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Mojo that generates package-info.java files
 */
@Mojo(
        name = "generate",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES
)
public class PackageInfoMojo extends AbstractMojo {

    private static final String PACKAGE_INFO_JAVA = "package-info.java";
    private static final Pattern PACKAGE_STATEMENT_PATTERN = Pattern.compile("^\\s*package\\s[\\S]+?\\s*;$", Pattern.MULTILINE);

    @Parameter(defaultValue = "${project.basedir}/src/main/java")
    private File sourceDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/package-info")
    private File outputDirectory;

    @Parameter
    private List<Package> packages;

    @Parameter(defaultValue = "(\\s*//\\s*<replace>\\s*)(.*?)(\\s*//\\s*</replace>\\s*)")
    private Pattern inlineReplacePattern;

    @Parameter(defaultValue = "true")
    private boolean generate;

    @Parameter(defaultValue = "true")
    private boolean inline;

    @Component
    private BuildContext buildContext;

    private Map<String, String> templateCache = new HashMap<>();


    public PackageInfoMojo() {
    }

    public void setSourceDirectory(File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void setPackages(List<Package> packages) {
        this.packages = packages;
    }

    public void setInlineReplacePattern(String inlineReplacePattern) {
        this.inlineReplacePattern = Pattern.compile(inlineReplacePattern, Pattern.MULTILINE);
    }

    public void setGenerate(boolean generate) {
        this.generate = generate;
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public void execute() throws MojoExecutionException {
        try {
            getLog().info(format("Generating package-info.java: %s", this));
            generate();
            getLog().debug("Done.");
        } catch (MojoExecutionException e) {
            throw e;
        } catch (RuntimeException | Error e) {
            throw new MojoExecutionException("Error generating package info files", e);
        }
    }

    private void generate() throws MojoExecutionException {
        if (packages == null || packages.isEmpty()) {
            getLog().warn("Skipping generate. No <packages/> declaration found.");
            return;
        }
        if (!sourceDirectory.exists()) {
            getLog().info(format("Skipping generate. No Source Directory found in this module: [%s].", sourceDirectory.getAbsolutePath()));
            return;
        }

        validate();
        loadTemplates();
        if (buildContext == null) {
            getLog().info(format("Using defaultBuildContext"));
            buildContext = new DefaultBuildContext();
        }
        walk(sourceDirectory, outputDirectory, "");
    }

    private void validate() throws MojoExecutionException {
        getLog().debug("Validating parameters");

        if (!sourceDirectory.isDirectory()) {
            throw new MojoExecutionException(format("Source Directory [%s] is not a directory.", sourceDirectory.getAbsolutePath()));
        }

        if (generate) {
            if (outputDirectory.exists() && !outputDirectory.isDirectory()) {
                throw new MojoExecutionException(format("Output Directory [%s] is not a directory.", outputDirectory.getAbsolutePath()));
            }

            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                throw new MojoExecutionException(format("Failed to create Output Directory [%s].", outputDirectory.getAbsolutePath()));
            }

        }

        for (int i = 0; i < packages.size(); i++) {
            Package pkg = packages.get(i);

            if (StringUtils.isEmpty(pkg.getPattern())) {
                throw new MojoExecutionException(format("<package/>[%s]: <pattern/> is required.", i));
            }

            if (pkg.getTemplate() == null) {
                throw new MojoExecutionException(format("<package/>[%s]: <template/> is required.", i));
            }

            if (!pkg.getTemplate().exists()) {
                throw new MojoExecutionException(format("<package/>[%s]: template [%s] does not exist.", i, pkg.getTemplate()));
            }

            if (!pkg.getTemplate().isFile()) {
                throw new MojoExecutionException(format("<package/>[%s]: template [%s] is not a file.", i, pkg.getTemplate()));
            }
        }

    }

    private void loadTemplates() throws MojoExecutionException {
        getLog().debug("Loading templates");

        for (int i = 0; i < packages.size(); i++) {
            Package pkg = packages.get(i);

            try {
                String source = loadTemplate(pkg.getTemplate());
                templateCache.put(pkg.getTemplate().getAbsolutePath(), source);
                getLog().debug(format("Loaded template %s", source));
            } catch (IOException e) {
                throw new MojoExecutionException(format("<package/>[%s]: Error loading template [%s]", i, pkg.getTemplate()), e);
            }
        }
    }

    private String loadTemplate(File template) throws IOException {
        try (InputStream inputStream = new FileInputStream(template)) {
            return IOUtil.toString(inputStream);
        }
    }

    private void walk(File currentSourceDirectory, File currentOutputDirectory, String packageName) throws MojoExecutionException {
        File[] javaFiles = currentSourceDirectory.listFiles(file -> file.isFile() && file.getName().endsWith(".java"));

        if (javaFiles != null && javaFiles.length > 0) {
            File packageInfo = Arrays.stream(javaFiles).filter(javaFile -> javaFile.getName().equals(PACKAGE_INFO_JAVA))
                    .findAny()
                    .orElse(null);

            Package pkg = findMatch(packageName);

            if (pkg == null) {
                getLog().debug(format("Package [%s] does not match any patterns", packageName));
            } else if (packageInfo == null) {
                if (generate) {
                    write(pkg, packageName, currentOutputDirectory);
                }
            } else if (inline) {
                replacePackageInfo(pkg, packageName, packageInfo);
            } else {
                getLog().debug(format("Skipping package [%s], main source already has a package-info.java", packageName));
            }
        }

        File[] childSourceDirectories = currentSourceDirectory.listFiles(file -> file.isDirectory() && !file.isHidden());

        if (childSourceDirectories != null && childSourceDirectories.length > 0) {
            for (File childSourceDirectory : childSourceDirectories) {
                String childName = childSourceDirectory.getName();
                String childPackageName = packageName.isEmpty() ? childName : packageName + '.' + childName;

                walk(childSourceDirectory, new File(currentOutputDirectory, childName), childPackageName);
            }
        }
    }

    private void replacePackageInfo(Package pkg, String packageName, File packageInfo) throws MojoExecutionException {
        getLog().debug(format("Existing package-info.java found: [%s], looking for replacement markers", packageInfo));

        if (templateCache.containsKey(packageInfo.getAbsolutePath())) {
            getLog().debug(format("%s is a template file, skipping.", packageInfo));
            return;
        }

        String packageInfoContents;

        try (FileInputStream inputStream = new FileInputStream(packageInfo)) {
            packageInfoContents = IOUtil.toString(inputStream);
        } catch (IOException e) {
            throw new MojoExecutionException(format("Unable to read %s", packageInfo), e);
        }

        Matcher matcher = inlineReplacePattern.matcher(packageInfoContents);

        if (!matcher.find()) {
            getLog().debug(format("Replace Pattern [%s] not found, skipping.", inlineReplacePattern));
            return;
        }

        StringBuffer buffer = new StringBuffer();
        String templateSource = templateCache.get(pkg.getTemplate().getAbsolutePath());
        String replacement = packageName.isEmpty() ? "" : format("package %s;", packageName);
        String packageSource = PACKAGE_STATEMENT_PATTERN.matcher(templateSource).replaceFirst(replacement);

        matcher.appendReplacement(buffer, format("$1%s$3", packageSource));
        matcher.appendTail(buffer);

        String replacedSource = buffer.toString();

        if (!replacedSource.equals(packageInfoContents)) {
            try (OutputStream outputStream = buildContext.newFileOutputStream(packageInfo)) {
                 IOUtil.copy(new ByteArrayInputStream(replacedSource.getBytes()), outputStream);
            } catch (IOException e) {
                throw new MojoExecutionException(format("Unable to write to [%s]", packageInfo), e);
            }
        }
    }

    private Package findMatch(String packageName) {
        for (Package pkg : packages) {
            if (matches(pkg, packageName)) {
                return pkg;
            }
        }

        return null;
    }

    private boolean matches(Package pkg, String packageName) {
        return SelectorUtils.matchPath(pkg.getPattern(), packageName, ".", pkg.isCaseSensitive());
    }

    private void write(Package pkg, String packageName, File currentOutputDirectory) throws MojoExecutionException {
        getLog().debug(format("Writing package [%s] to [%s].", packageName, currentOutputDirectory));

        if (!currentOutputDirectory.exists() && !currentOutputDirectory.mkdirs()) {
            throw new MojoExecutionException(format("Unable to create [%s]", currentOutputDirectory));
        }

        File packageInfoOutputFile = new File(currentOutputDirectory, PACKAGE_INFO_JAVA);
        String replacement = packageName.isEmpty() ? "" : format("package %s;", packageName);
        String templateSource = templateCache.get(pkg.getTemplate().getAbsolutePath());
        String packageSource = PACKAGE_STATEMENT_PATTERN.matcher(templateSource).replaceFirst(replacement);

        try (
                InputStream inputStream = new ByteArrayInputStream(packageSource.getBytes());
                OutputStream outputStream = buildContext.newFileOutputStream(packageInfoOutputFile)) {
            write(inputStream, outputStream);
        } catch (IOException e) {
            throw new MojoExecutionException(format("Error writing [%s] to [%s]", packageSource, packageInfoOutputFile), e);
        }
    }

    private void write(InputStream inputStream, OutputStream outputStream) throws IOException {
        IOUtil.copy(inputStream, outputStream);
    }

    @Override
    public String toString() {
        return "{" +
                "sourceDirectory=" + sourceDirectory +
                ", outputDirectory=" + outputDirectory +
                ", packages=" + packages +
                ", inlineReplacePattern='" + inlineReplacePattern + '\'' +
                ", generate=" + generate +
                ", inline=" + inline +
                '}';
    }
}
