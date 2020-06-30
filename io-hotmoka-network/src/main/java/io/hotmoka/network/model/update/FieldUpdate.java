package io.hotmoka.network.model.update;

public class FieldUpdate extends Update {
    private String updateType;
    private String definingClass;
    private String value;
    private String javaType;
    private String name;

    public String getUpdateType() {
        return updateType;
    }

    public void setUpdateType(String updateType) {
        this.updateType = updateType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDefiningClass() {
        return definingClass;
    }

    public void setDefiningClass(String definingClass) {
        this.definingClass = definingClass;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
