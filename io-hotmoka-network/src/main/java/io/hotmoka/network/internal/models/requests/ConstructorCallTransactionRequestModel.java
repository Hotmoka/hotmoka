package io.hotmoka.network.internal.models.requests;

import java.util.List;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.values.StorageValueModel;

public class ConstructorCallTransactionRequestModel extends NonInitialTransactionRequestModel {
    private String definingClass;
    private List<StorageValueModel> actuals;

    public void setDefiningClass(String definingClass) {
        this.definingClass = definingClass;
    }

    public void setActuals(List<StorageValueModel> actuals) {
        this.actuals = actuals;
    }

    public ConstructorCallTransactionRequest toBean() {
    	return new ConstructorCallTransactionRequest(
        	decodeBase64(getSignature()),
            getCaller().toBean(),
            getNonce(),
            getChainId(),
            getGasLimit(),
            getGasPrice(),
            getClasspath().toBean(),
            new ConstructorSignature(definingClass, actualsToTypes()),
            actuals.stream().map(StorageValueModel::toBean).toArray(StorageValue[]::new));
    }

    private StorageType[] actualsToTypes() {
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