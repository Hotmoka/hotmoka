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
import java.util.*;
import java.util.stream.Stream;

public class StorageResolver {

    /**
     * Creates a {@link io.hotmoka.beans.values.StorageReference} for the given hash input
     * @param hash the hash of the storage reference
     * @param progressive the progressive
     * @return a {@link io.hotmoka.beans.values.StorageReference}
     */
    public static StorageReference resolveStorageReference(String hash, BigInteger progressive) {
        return new StorageReference(new LocalTransactionReference(hash), progressive);
    }

    /**
     * Creates a {@link io.hotmoka.beans.references.LocalTransactionReference} array from a list of dependencies
     * @param dependencies the list of dependencies
     * @return an array of  {@link io.hotmoka.beans.references.LocalTransactionReference}
     */
    public static LocalTransactionReference[] resolveJarDependencies(List<StorageModel> dependencies) {
        return Stream.ofNullable(dependencies)
                .flatMap(Collection::stream)
                .map(storageModel -> new LocalTransactionReference(storageModel.getHash()))
                .toArray(LocalTransactionReference[]::new);
    }

    /**
     * Creates a {@link io.hotmoka.beans.signatures.MethodSignature} from a request
     * @param request the request
     * @return the {@link io.hotmoka.beans.signatures.MethodSignature}
     */
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

    /**
     * Creates the {@link io.hotmoka.beans.values.StorageValue} array from a list of values
     * @param values the values
     * @return an array of {@link io.hotmoka.beans.values.StorageValue}
     */
    public static StorageValue[] resolveStorageValues(List<StorageValueModel> values) {
        return Stream.ofNullable(values)
                .flatMap(Collection::stream)
                .map(value -> storageValueFrom(value.getType(), value.getValue()))
                .filter(Objects::nonNull)
                .toArray(StorageValue[]::new);
    }

    /**
     * Creates the {@link io.hotmoka.beans.types.StorageType} array from a list of values which have a type
     * @param values the values
     * @return an array of {@link io.hotmoka.beans.types.StorageType}
     */
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
                case "long": return new LongValue(Long.valueOf((Integer) value));
                case "float": return new FloatValue((Float) value);
                case "double": return new DoubleValue((Double) value);
                case "java.math.BigInteger": return new BigIntegerValue(BigInteger.valueOf(Long.valueOf((Integer) value)));
                case "java.lang.String": return new StringValue((String) value);
                case "null": return NullValue.INSTANCE;
                default:
                    if (value instanceof Map) {
                        HashMap<String, Object> storageModel = (HashMap<String, Object>) value;
                        return new StorageReference(new LocalTransactionReference((String) storageModel.get("hash")), BigInteger.valueOf(Long.valueOf((Integer) storageModel.get("progressive"))));
                    } else
                        throw new TypeNotPresentException("" + type, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
