package io.hotmoka.network.models.signatures;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;

/**
 * The model of the signature of a method or constructor.
 */
@Immutable
public abstract class CodeSignatureModel {

	/**
	 * The name of the class defining the method or constructor.
	 */
	public final String definingClass;

	/**
	 * The formal arguments of the method or constructor.
	 */
	private final List<String> formals;

	/**
	 * Builds the model of the signature of a method or constructor.
	 * 
	 * @param signature the original signature to copy
	 */
	protected CodeSignatureModel(CodeSignature signature) {
		this.definingClass = signature.definingClass.name;
		this.formals = signature.formals().map(CodeSignatureModel::nameOf).collect(Collectors.toList());
	}

	public final Stream<String> getFormals() {
		return formals.stream();
	}

	/**
	 * Yields the storage types of the formal arguments of this method or constructor.
	 * 
	 * @return the storage types
	 */
	protected final StorageType[] getFormalsAsTypes() {
		return getFormals().map(CodeSignatureModel::typeWithName).toArray(StorageType[]::new);
	}

	/**
	 * Yields a string representation of the given type.
	 * 
	 * @param type the type
	 * @return the string
	 */
	protected static String nameOf(StorageType type) {
    	if (type == null)
    		throw new InternalFailureException("unexpected null type");
    	else if (type instanceof BasicTypes || type instanceof ClassType)
    		return type.toString();
    	else
    		// TODO deal with enums
    		throw new InternalFailureException("unexpected storage type of class " + type.getClass().getName());
    }

	/**
	 * Yields the type with the given name.
	 * 
	 * @param name the name of the type
	 * @return the type
	 */
	protected static StorageType typeWithName(String name) {
    	if (name == null)
    		throw new InternalFailureException("unexpected null type name");

    	switch (name) {
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
        // TODO: deal with enums
        default:
        	return new ClassType(name);
    	}
	}
}