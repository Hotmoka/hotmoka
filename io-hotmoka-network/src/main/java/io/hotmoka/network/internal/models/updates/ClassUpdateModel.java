package io.hotmoka.network.internal.models.updates;

public class ClassUpdateModel extends UpdateModel {
    private String className;
    private String jar;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getJar() {
        return jar;
    }

    public void setJar(String jar) {
        this.jar = jar;
    }
}
