package io.hotmoka.network.util;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.values.*;
import io.hotmoka.network.model.storage.StorageModel;
import io.hotmoka.network.model.storage.StorageValueModel;
import io.hotmoka.network.model.transaction.MethodCallTransactionRequestModel;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class StorageResolver {

    public static MethodSignature resolveMethodSignature(MethodCallTransactionRequestModel request){

        if (request.isVoidReturnType())
            return new VoidMethodSignature(
                    request.getClassType(),
                    request.getMethodName(),
                    resolveStorageTypes(request.getValues()));

        return new NonVoidMethodSignature(
                request.getClassType(),
                request.getMethodName(),
                storageTypeFrom(request.getReturnType()),
                resolveStorageTypes(request.getValues()));
    }

    public static StorageValue[] resolveStorageValues(List<StorageValueModel> values) {
        return Stream.ofNullable(values)
                .flatMap(Collection::stream)
                .map(value -> storageValueFrom(value.getType(), value.getValue()))
                .filter(Objects::nonNull)
                .toArray(StorageValue[]::new);
    }

    public static StorageType[] resolveStorageTypes(List<StorageValueModel> values) {
        return Stream.ofNullable(values)
                .flatMap(Collection::stream)
                .map(value -> storageTypeFrom(value.getType()))
                .filter(Objects::nonNull)
                .toArray(StorageType[]::new);
    }

    /**
     * Creates a {@link io.hotmoka.beans.types.StorageType} from a given type
     * @param type the type
     * @return a {@link io.hotmoka.beans.types.StorageType}
     */
    public static StorageType storageTypeFrom(String type) {
        switch (type) {
            case "boolean": return BasicTypes.BOOLEAN;
            case "byte": return BasicTypes.BYTE;
            case "char": return BasicTypes.CHAR;
            case "short": return BasicTypes.SHORT;
            case "int": return BasicTypes.INT;
            case "long": return BasicTypes.LONG;
            case "float": return BasicTypes.FLOAT;
            case "double": return BasicTypes.DOUBLE;
            default: return new ClassType(type);
        }
    }

    /**
     * Creates a {@link io.hotmoka.beans.values.StorageValue} from a given type and value
     * @param type the type of the value
     * @param value the value
     * @return a {@link io.hotmoka.beans.values.StorageValue}
     */
    private static StorageValue storageValueFrom(String type, Object value) {
        try {
            switch (type) {
                case "boolean": return new BooleanValue((Boolean) value);
                case "byte": return new ByteValue(Byte.parseByte((String) value));
                case "char": return new CharValue(((String) value).charAt(0));
                case "short": return new ShortValue((Short) value);
                case "int": return new IntValue((Integer) value);
                case "long": return new LongValue((BigInteger) value);
                case "float": return new FloatValue((Float) value);
                case "double": return new DoubleValue((Double) value);
                default:
                    if (value == null)
                        return NullValue.INSTANCE;
                    else {
                        StorageModel storageModel = (StorageModel) value;
                        return new StorageReference(new LocalTransactionReference(storageModel.getHash()), storageModel.getProgressive());
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
