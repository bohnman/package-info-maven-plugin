package com.github.bohnman;

import java.io.File;

public class Package {

    private String pattern;

    private boolean caseSensitive = true;

    private File template;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public File getTemplate() {
        return template;
    }

    public void setTemplate(File template) {
        this.template = template;
    }

    @Override
    public String toString() {
        return "{" +
                "pattern='" + pattern + '\'' +
                ", caseSensitive=" + caseSensitive +
                ", template=" + template +
                '}';
    }
}
