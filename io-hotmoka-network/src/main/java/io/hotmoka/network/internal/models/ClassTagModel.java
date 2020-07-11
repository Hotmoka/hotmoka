package io.hotmoka.network.internal.models;

public class ClassTagModel {
    private String className;
    private String jarHash;

    public String getJarHash() {
        return jarHash;
    }

    public void setJarHash(String jarHash) {
        this.jarHash = jarHash;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
