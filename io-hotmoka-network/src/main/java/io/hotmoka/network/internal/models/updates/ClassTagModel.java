package io.hotmoka.network.internal.models.updates;

import io.hotmoka.beans.updates.ClassTag;

public class ClassTagModel {
    private String className;
    private String jarHash;

    public ClassTagModel() {}

    public ClassTagModel(ClassTag tag) {
    	className = tag.className;
    	jarHash = tag.jar.getHash();
    }

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
