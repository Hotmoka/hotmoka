package io.hotmoka.network.internal.models.requests;

import java.util.List;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.network.internal.models.values.StorageValueModel;

public abstract class MethodCallTransactionRequestModel extends NonInitialTransactionRequestModel {
	private String constructorType;
    private boolean voidReturnType;
    private String methodName;
    private String returnType;
    private List<StorageValueModel> actuals;

    public void setConstructorType(String constructorType) {
        this.constructorType = constructorType;
    }

    public void setVoidReturnType(boolean voidReturnType) {
        this.voidReturnType = voidReturnType;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    protected List<StorageValueModel> getActuals() {
        return actuals;
    }

    public void setActuals(List<StorageValueModel> actuals) {
        this.actuals = actuals;
    }

    /**
     * Creates a {@link io.hotmoka.beans.signatures.MethodSignature} from a request
     * @param request the request
     * @return the {@link io.hotmoka.beans.signatures.MethodSignature}
     */
    protected MethodSignature resolveMethodSignature() {
        if (voidReturnType)
            return new VoidMethodSignature(
            		constructorType,
                    methodName,
                    actualsToBean());

        return new NonVoidMethodSignature(
        		constructorType,
                methodName,
                storageTypeFrom(returnType),
                actualsToBean());
    }

    private StorageType[] actualsToBean() {
        return actuals.stream()
                .map(value -> storageTypeFrom(value.getType()))
                .toArray(StorageType[]::new);
    }

    /**
     * Creates a {@link io.hotmoka.beans.types.StorageType} from a given type
     * @param type the type
     * @return a {@link io.hotmoka.beans.types.StorageType}
     */
    private static StorageType storageTypeFrom(String type) {
        if (type == null)
            throw new InternalFailureException("unexpected null value type");

        switch (type) {
            case "boolean":
                return BasicTypes.BOOLEAN;
            case "byte":
                return BasicTypes.BYTE;
            case "char":
                return BasicTypes.CHAR;
            case "short":
                return BasicTypes.SHORT;
            case "int":
                return BasicTypes.INT;
            case "long":
                return BasicTypes.LONG;
            case "float":
                return BasicTypes.FLOAT;
            case "double":
                return BasicTypes.DOUBLE;
            default:
                return new ClassType(type);
        }
    }
}