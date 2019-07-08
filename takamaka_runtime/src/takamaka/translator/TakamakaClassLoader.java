package takamaka.translator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.Storage;

/**
 * A class loader used to access the definition of the classes
 * of a Takamaka program.
 */
public class TakamakaClassLoader extends URLClassLoader implements AutoCloseable {

	private final static String CONTRACT_CLASS_NAME = "takamaka.lang.Contract";
	private final static String STORAGE_CLASS_NAME = Storage.class.getName();

	/**
	 * The class token of the contract class.
	 */
	public final Class<?> contractClass;

	/**
	 * The class token of the storage class.
	 */
	public final Class<?> storageClass;

	/**
	 * Builds a class loader with the given URLs.
	 */
	protected TakamakaClassLoader(URL[] urls) {
		super(urls, ClassLoader.getSystemClassLoader());

		try {
			this.contractClass = loadClass(CONTRACT_CLASS_NAME);
			this.storageClass = loadClass(STORAGE_CLASS_NAME);
		} catch (ClassNotFoundException e) {
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
		try {
			return storageClass.isAssignableFrom(loadClass(className));
		} catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}
	}

	/**
	 * Checks if a class is a contract or subclass of contract.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 */
	public final boolean isContract(String className) {
		try {
			return contractClass.isAssignableFrom(loadClass(className));
		} catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}
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
	 * Determines if an instance field of a storage class is transient.
	 * 
	 * @param className the class from which the field must be looked-up. This is guaranteed to be a storage class
	 * @param fieldName the name of the field
	 * @param fieldType the type of the field
	 * @return true if and only if that condition holds
	 */
	public final boolean isTransient(String className, String fieldName, Class<?> fieldType) {
		try {
			Class<?> clazz = loadClass(className);
			
			do {
				Optional<Field> match = Stream.of(clazz.getDeclaredFields())
					.filter(field -> field.getName().equals(fieldName) && fieldType == field.getType())
					.findFirst();

				if (match.isPresent())
					return Modifier.isTransient(match.get().getModifiers());
			}
			while (clazz != storageClass && clazz != contractClass);
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}

		return false;
	}

	/**
	 * Determines if an instance field of a storage class is transient or final.
	 * 
	 * @param className the class from which the field must be looked-up. This is guaranteed to be a storage class
	 * @param fieldName the name of the field
	 * @param fieldType the type of the field
	 * @return true if and only if that condition holds
	 */
	public final boolean isTransientOrFinal(String className, String fieldName, Class<?> fieldType) {
		try {
			Class<?> clazz = loadClass(className);
			
			do {
				Optional<Field> match = Stream.of(clazz.getDeclaredFields())
					.filter(field -> field.getName().equals(fieldName) && fieldType == field.getType())
					.findFirst();

				if (match.isPresent()) {
					int modifiers = match.get().getModifiers();
					return Modifier.isTransient(modifiers) || Modifier.isFinal(modifiers);
				}
			}
			while (clazz != storageClass && clazz != contractClass);
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}

		return false;
	}

	/**
	 * Determines if the given constructor or method is annotated as payable.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return true if and only if that condition holds
	 */
	public final boolean isPayable(String className, String methodName, Type[] formals, Type returnType) {
		return getAnnotation(className, methodName, formals, returnType, Payable.class) != null;
	}

	/**
	 * Determines if the given constructor or method is annotated as entry.
	 * Yields the argument of the annotation.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return the value of the annotation, if it is a contract. For instance, for {@code @@Entry(PayableContract.class)}
	 *         this return value will be {@code takamaka.lang.PayableContract.class}
	 */
	public final Class<?> isEntry(String className, String methodName, Type[] formals, Type returnType) {
		Annotation annotation = getAnnotation(className, methodName, formals, returnType, Entry.class);
		if (annotation != null) {
			Class<?> contractClass = ((Entry) annotation).value();
			return contractClass != Object.class ? contractClass : this.contractClass;
		}

		return null;
	}

	/**
	 * Gets the given annotation from the given constructor or method.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method or constructor
	 * @param returnType the return type of the method or constructor
	 * @param annotation the class token of the annotation
	 * @return the annotation, if any. Yields {@code null} if the method or constructor has no such annotation
	 */
	public final Annotation getAnnotation(String className, String methodName, Type[] formals, Type returnType, Class<? extends Annotation> annotation) {
		if (methodName.equals(Const.CONSTRUCTOR_NAME))
			return getAnnotationOfConstructor(className, formals, annotation);
		else
			return getAnnotationOfMethod(className, methodName, formals, returnType, annotation);
	}

	private Annotation getAnnotationOfConstructor(String className, Type[] formals, Class<? extends Annotation> annotation) {
		Class<?>[] formalsClass = Stream.of(formals).map(this::bcelToClass).toArray(Class[]::new);

		try {
			Class<?> clazz = loadClass(className);
			Optional<Constructor<?>> definition = Stream.of(clazz.getDeclaredConstructors())
				.filter(c -> Arrays.equals(c.getParameterTypes(), formalsClass))
				.findFirst();

			return definition.isPresent() ? definition.get().getAnnotation(annotation) : null;
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}
	}

	private Annotation getAnnotationOfMethod(String className, String methodName, Type[] formals, Type returnType, Class<? extends Annotation> annotation) {
		Class<?> returnTypeClass = bcelToClass(returnType);
		Class<?>[] formalsClass = Stream.of(formals).map(this::bcelToClass).toArray(Class[]::new);

		try {
			Class<?> clazz = loadClass(className);
			Optional<java.lang.reflect.Method> definition = Stream.of(clazz.getDeclaredMethods())
				.filter(m -> m.getName().equals(methodName) && m.getReturnType() == returnTypeClass && Arrays.equals(m.getParameterTypes(), formalsClass))
				.findFirst();

			if (definition.isPresent()) {
				Annotation result = definition.get().getAnnotation(annotation);
				if (result != null)
					return result;

				if (Modifier.isPrivate(definition.get().getModifiers()))
					return null;
			}

			Class<?> superclass = clazz.getSuperclass();
			if (superclass == null)
				return null;
			else
				return getAnnotationOfMethod(superclass.getName(), methodName, formals, returnType, annotation);
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}
	}

	/**
	 * Computes the Java token class for the given BCEL type.
	 * 
	 * @param type the BCEL type
	 * @return type the class token corresponding to {@code type}
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
			try {
				return loadClass(type.toString()); //getSignature().replace('/', '.'));
			}
			catch (ClassNotFoundException e) {
				throw new IncompleteClasspathError(e);
			}
		else { // array
			Class<?> elementsClass = bcelToClass(((ArrayType) type).getElementType());
			// trick: we build an array of 0 elements just to access its class token
			return java.lang.reflect.Array.newInstance(elementsClass, 0).getClass();
		}
	}
}