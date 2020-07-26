package io.hotmoka.network.internal.util;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.requests.MethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.StorageValueModel;

public class StorageResolver {

    /**
     * Creates a {@link io.hotmoka.beans.references.LocalTransactionReference} array from a list of dependencies
     * @param dependencies the list of dependencies
     * @return an array of  {@link io.hotmoka.beans.references.LocalTransactionReference}
     */
    public static LocalTransactionReference[] resolveJarDependencies(List<StorageReferenceModel> dependencies) {
        return Stream.ofNullable(dependencies)
                .flatMap(Collection::stream)
                .map(storageModel -> new LocalTransactionReference(storageModel.getTransaction()))
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
                    request.getConstructorType(),
                    request.getMethodName(),
                    resolveStorageTypes(request.getValues()));

        return new NonVoidMethodSignature(
                request.getConstructorType(),
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
                .map(StorageValueModel::toBean)
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