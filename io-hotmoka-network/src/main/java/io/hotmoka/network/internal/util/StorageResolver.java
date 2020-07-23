package io.hotmoka.network.internal.util;

import java.math.BigInteger;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.ByteValue;
import io.hotmoka.beans.values.CharValue;
import io.hotmoka.beans.values.DoubleValue;
import io.hotmoka.beans.values.FloatValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.NullValue;
import io.hotmoka.beans.values.ShortValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.network.exception.ReferenceNotFoundException;
import io.hotmoka.network.exception.TypeNotFoundException;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.ValueModel;
import io.hotmoka.network.internal.models.transactions.MethodCallTransactionRequestModel;
import io.hotmoka.network.json.JSONTransactionReference;

public class StorageResolver {

    public static byte[] decodeBase64(String value) {
        return Base64.getDecoder().decode(value);
    }

    /**
     * Creates a {@link io.hotmoka.beans.values.StorageReference} for the given storage model
     * @param storageModel the storage model which hold hash of the storage reference and the progressive
     * @return a {@link io.hotmoka.beans.values.StorageReference}
     */
    public static StorageReference resolveStorageReference(StorageReferenceModel storageModel) {
        return new StorageReference(JSONTransactionReference.fromJSON(storageModel.getTransaction()), storageModel.getProgressive());
    }

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
    public static StorageValue[] resolveStorageValues(List<ValueModel> values) {
        return Stream.ofNullable(values)
                .flatMap(Collection::stream)
                .map(StorageResolver::storageValueFrom)
                .filter(Objects::nonNull)
                .toArray(StorageValue[]::new);
    }

    /**
     * Creates the {@link io.hotmoka.beans.types.StorageType} array from a list of values which have a type
     * @param values the values
     * @return an array of {@link io.hotmoka.beans.types.StorageType}
     */
    public static StorageType[] resolveStorageTypes(List<ValueModel> values) {
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

        if (type == null)
            throw new TypeNotFoundException("Value type not supplied");

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

    /**
     * Creates a {@link io.hotmoka.beans.values.StorageValue} from a given storage value model
     * @param valueModel the storage value model
     * @return a {@link io.hotmoka.beans.values.StorageValue}
     */
    private static StorageValue storageValueFrom(ValueModel valueModel) {

        if (valueModel.getType() == null)
            throw new TypeNotFoundException("Value type not supplied");

        switch (valueModel.getType()) {
            case "boolean":
                return new BooleanValue(Boolean.parseBoolean(valueModel.getValue()));
            case "byte":
                return new ByteValue(Byte.parseByte(valueModel.getValue()));
            case "char":
                return new CharValue(valueModel.getValue().charAt(0));
            case "short":
                return new ShortValue(Short.parseShort(valueModel.getValue()));
            case "int":
                return new IntValue(Integer.parseInt(valueModel.getValue()));
            case "long":
                return new LongValue(Long.parseLong(valueModel.getValue()));
            case "float":
                return new FloatValue(Float.parseFloat(valueModel.getValue()));
            case "double":
                return new DoubleValue(Double.parseDouble(valueModel.getValue()));
            case "java.math.BigInteger":
                return new BigIntegerValue(BigInteger.valueOf(Long.parseLong(valueModel.getValue())));
            case "java.lang.String":
                return new StringValue(valueModel.getValue());
            case "null":
                return NullValue.INSTANCE;
            default:
                if (valueModel.getReference() != null) {
                    return new StorageReference(new LocalTransactionReference(valueModel.getReference().getTransaction()), valueModel.getReference().getProgressive());
                } else
                    throw new ReferenceNotFoundException();
        }
    }
}
