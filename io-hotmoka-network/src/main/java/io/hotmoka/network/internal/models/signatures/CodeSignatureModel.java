package io.hotmoka.network.internal.models.signatures;

import java.util.List;
import java.util.stream.Collectors;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;

/**
 * The model of the signature of a method or constructor.
 */
public abstract class CodeSignatureModel {

	/**
	 * The name of the class defining the method or constructor.
	 */
	private String definingClass;

	/**
	 * The formal arguments of the method or constructor.
	 */
	private List<String> formals;

	/**
	 * For Spring.
	 */
	protected CodeSignatureModel() {}

	/**
	 * Builds the model of the signature of a method or constructor.
	 * 
	 * @param parent the original signature to copy
	 */
	protected CodeSignatureModel(CodeSignature parent) {
		this.definingClass = parent.definingClass.name;
		this.formals = parent.formals().map(CodeSignatureModel::nameOf).collect(Collectors.toList());
	}

	/**
	 * For Spring.
	 */
	public void setDefiningClass(String definingClass) {
		this.definingClass = definingClass;
	}

	/**
	 * Yields the name of the class defining the method or constructor.
	 * 
	 * @return the name of the class
	 */
	protected final String getDefiningClass() {
		return definingClass;
	}

	/**
	 * For Spring.
	 */
	public void setFormals(List<String> formals) {
		this.formals = formals;
	}

	/**
	 * Yields the storage types of the formal arguments of this method or constructor.
	 * 
	 * @return the storage types
	 */
	protected final StorageType[] getFormalsAsTypes() {
		return formals.stream().map(CodeSignatureModel::typeWithName).toArray(StorageType[]::new);
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