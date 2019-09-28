package takamaka.whitelisted;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An object that knows about the fields, methods and constructors that can be called from
 * Takamaka code and their proof-obligations.
 */
public class WhiteListingWizard {
	private final static String WHITE_LISTED_ROOT = WhiteListingWizard.class.getPackage().getName();

	/**
	 * The class loader used to load the classes of the Takamaka program.
	 */
	private final ResolvingClassLoader classLoader;

	/**
	 * Builds an object that knows about fields, methods and constructors that can be called from
	 * Takamaka code and their proof-obligations.
	 * 
	 * @param classLoader the class loader used to load the classes of the Takamaka program
	 */
	public WhiteListingWizard(ResolvingClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Looks for a white-listing model of the given field. That is a field declaration
	 * that justifies why the field is white-listed. It can be the field itself, if it
	 * belongs to a class installed in blockchain, or otherwise a field of a white-listing
	 * class, if it belongs to some Java run-time support class.
	 * 
	 * @param field the field whose model is looked for
	 * @return the model of its white-listing, if it exists
	 */
	public final Optional<Field> whiteListingModelOf(Field field) {
		// if the class defining the field has been loaded by the blockchain class loader,
		// then it comes from blockchain and the field is white-listed
		if (field.getDeclaringClass().getClassLoader() == classLoader)
			return Optional.of(field);
		// otherwise, since fields cannot be redefined in Java, either is the field explicitly
		// annotated as white-listed, or it is not white-listed
		else if (field.isAnnotationPresent(WhiteListed.class))
			return Optional.of(field);
		else
			return fieldInWhiteListedLibraryFor(field);
	}


	/**
	 * Looks for a white-listing model of the given method or constructor. That is a constructor declaration
	 * that justifies why the method or constructor is white-listed. It can be the method or constructor itself, if it
	 * belongs to a class installed in blockchain, or otherwise a method or constructor of a white-listing
	 * class, if it belongs to some Java run-time support class.
	 * 
	 * @param executable the method or constructor whose model is looked for
	 * @return the model of its white-listing, if it exists
	 * @throws ClassNotFoundException if some class could not be found during the look-up of the model
	 */
	public final Optional<? extends Executable> whiteListingModelOf(Executable executable) throws ClassNotFoundException {
		// if the class defining the constructor has been loaded by the blockchain class loader,
		// then it comes from blockchain and the constructor is white-listed
		Class<?> declaringClass = executable.getDeclaringClass();

		if (declaringClass.getClassLoader() == classLoader)
			return Optional.of(executable);
		// otherwise, since constructors cannot be redefined in Java, either is the constructor explicitly
		// annotated as white-listed, or it is not white-listed
		else if (executable.isAnnotationPresent(WhiteListed.class))
			return Optional.of(executable);
		else if (executable instanceof Constructor<?>)
			return constructorInWhiteListedLibraryFor((Constructor<?>) executable);
		else {
			java.lang.reflect.Method method = (java.lang.reflect.Method) executable;
			Optional<? extends Executable> result = methodInWhiteListedLibraryFor(method);
			if (result.isPresent())
				return result;

			// a method might not be explicitly white-listed, but it might override a method
			// of a superclass that is white-listed. Hence we check that possibility
			if (!Modifier.isStatic(method.getModifiers()) && !Modifier.isPrivate(method.getModifiers())) {
				Class<?> superclass = declaringClass.getSuperclass();
				if (superclass != null) {
					Optional<java.lang.reflect.Method> overridden = classLoader.resolveMethod(superclass.getName(), method.getName(), method.getParameterTypes(), method.getReturnType());
					if (overridden.isPresent()) {
						result = whiteListingModelOf(overridden.get());
						if (result.isPresent())
							return result;
					}
				}

				for (Class<?> superinterface: declaringClass.getInterfaces()) {
					Optional<java.lang.reflect.Method> overridden = classLoader.resolveMethod(superinterface.getName(), method.getName(), method.getParameterTypes(), method.getReturnType());
					if (overridden.isPresent()) {
						result = whiteListingModelOf(overridden.get());
						if (result.isPresent())
							return result;
					}
				}
			}
		}

		return Optional.empty();
	}

	private Optional<Field> fieldInWhiteListedLibraryFor(Field field) {
		String expandedClassName = WHITE_LISTED_ROOT + "." + field.getDeclaringClass().getName();
		Class<?> classInWhiteListedLibrary;
	
		try {
			classInWhiteListedLibrary = Class.forName(expandedClassName);
		}
		catch (ClassNotFoundException e) {
			// the field is not in the library of white-listed code
			return Optional.empty();
		}
	
		return Stream.of(classInWhiteListedLibrary.getDeclaredFields())
			.filter(field2 -> field2.getType() == field.getType() && field2.getName().equals(field.getName()))
			.findFirst();
	}

	private Optional<Constructor<?>> constructorInWhiteListedLibraryFor(Constructor<?> constructor) {
		String expandedClassName = WHITE_LISTED_ROOT + "." + constructor.getDeclaringClass().getName();
		Class<?> classInWhiteListedLibrary;
	
		try {
			classInWhiteListedLibrary = Class.forName(expandedClassName);
		}
		catch (ClassNotFoundException e) {
			// the constructor is not in the library of white-listed code
			return Optional.empty();
		}
	
		try {
			// if the constructor has been reported in the white-listed library, then it is automatically white-listed
			return Optional.of(classInWhiteListedLibrary.getDeclaredConstructor(constructor.getParameterTypes()));
		}
		catch (NoSuchMethodException e) {
			return Optional.empty();
		}
	}

	private Optional<java.lang.reflect.Method> methodInWhiteListedLibraryFor(java.lang.reflect.Method method) {
		Class<?> declaringClass = method.getDeclaringClass();

		// Method Object.getClass() is white-listed but we cannot put it in the white-listed library,
		// since that method is final in Object
		if (declaringClass == Object.class && "getClass".equals(method.getName()))
			try {
				return Optional.of(Object.class.getMethod("getClass"));
			}
			catch (NoSuchMethodException e) {
				// this will never happen
				throw new IllegalStateException("Cannot find method Object.getClass()");
			}

		String expandedClassName = WHITE_LISTED_ROOT + "." + declaringClass.getName();
		Class<?> classInWhiteListedLibrary;
	
		try {
			classInWhiteListedLibrary = Class.forName(expandedClassName);
		}
		catch (ClassNotFoundException e) {
			// the method is not in the library of white-listed code
			return Optional.empty();
		}
	
		Optional<java.lang.reflect.Method> methodInWhiteListedLibrary = Stream.of(classInWhiteListedLibrary.getDeclaredMethods())
			.filter(method2 -> method2.getReturnType() == method.getReturnType() && method2.getName().equals(method.getName())
						&& Arrays.equals(method2.getParameterTypes(), method.getParameterTypes()))
			.findFirst();
	
		return methodInWhiteListedLibrary;
	}
}