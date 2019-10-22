package takamaka.instrumentation;

import java.math.BigInteger;
import java.net.URL;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import takamaka.instrumentation.internal.ThrowIncompleteClasspathError;
import takamaka.lang.Storage;
import takamaka.whitelisted.ResolvingClassLoader;

/**
 * A class loader used to access the definition of the classes
 * of a Takamaka program.
 */
public class TakamakaClassLoader extends ResolvingClassLoader {

	/**
	 * The class token of the contract class.
	 */
	public final Class<?> contractClass;

	/**
	 * The class token of the externally owned account class.
	 */
	public final Class<?> externallyOwnedAccount;

	/**
	 * The class token of the storage class.
	 */
	public final Class<?> storageClass;

	/**
	 * Builds a class loader with the given URLs.
	 */
	public TakamakaClassLoader(URL[] urls) {
		super(urls);

		try {
			this.contractClass = loadClass("takamaka.lang.Contract");
			this.externallyOwnedAccount = loadClass("takamaka.lang.ExternallyOwnedAccount");
			this.storageClass = loadClass(Storage.class.getName());
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}
	}

	/**
	 * Determines if a class is a storage class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that class extends {@link takamaka.lang.Storage}
	 */
	public final boolean isStorage(String className) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> storageClass.isAssignableFrom(loadClass(className)));
	}

	/**
	 * Checks if a class is a contract.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 */
	public final boolean isContract(String className) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> contractClass.isAssignableFrom(loadClass(className)));
	}

	/**
	 * Determines if a field of a storage class, having the given field, is lazily loaded.
	 * 
	 * @param type the type
	 * @return true if and only if that condition holds
	 */
	public final boolean isLazilyLoaded(Class<?> type) {
		return !type.isPrimitive() && type != String.class && type != BigInteger.class && !type.isEnum();
	}

	/**
	 * Determines if a field of a storage class, having the given field, is eagerly loaded.
	 * 
	 * @param type the type
	 * @return true if and only if that condition holds
	 */
	public final boolean isEagerlyLoaded(Class<?> type) { //TODO: also externally
		return !isLazilyLoaded(type);
	}

	/**
	 * Computes the Java token class for the given BCEL type.
	 * 
	 * @param type the BCEL type
	 * @return the class token corresponding to {@code type}
	 */
	public final Class<?> bcelToClass(Type type) {
		if (type == BasicType.BOOLEAN)
			return boolean.class;
		else if (type == BasicType.BYTE)
			return byte.class;
		else if (type == BasicType.CHAR)
			return char.class;
		else if (type == BasicType.DOUBLE)
			return double.class;
		else if (type == BasicType.FLOAT)
			return float.class;
		else if (type == BasicType.INT)
			return int.class;
		else if (type == BasicType.LONG)
			return long.class;
		else if (type == BasicType.SHORT)
			return short.class;
		else if (type == BasicType.VOID)
			return void.class;
		else if (type instanceof ObjectType)
			return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> loadClass(type.toString()));
		else { // array
			Class<?> elementsClass = bcelToClass(((ArrayType) type).getElementType());
			// trick: we build an array of 0 elements just to access its class token
			return java.lang.reflect.Array.newInstance(elementsClass, 0).getClass();
		}
	}

	/**
	 * Computes the Java token classes for the given BCEL types.
	 * 
	 * @param types the BCEL types
	 * @return the class tokens corresponding to {@code types}
	 */
	public final Class<?>[] bcelToClass(Type[] types) {
		Class<?>[] result = new Class<?>[types.length];
		for (int pos = 0; pos < result.length; pos++)
			result[pos] = bcelToClass(types[pos]);
	
		return result;
	}
}