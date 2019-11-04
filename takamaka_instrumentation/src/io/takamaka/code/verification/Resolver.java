package io.takamaka.code.verification;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.Optional;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;

import io.takamaka.code.instrumentation.Dummy;
import io.takamaka.code.verification.internal.ThrowIncompleteClasspathError;

/**
 * An utility that implements resolving algorithms for field and methods.
 */
public class Resolver {

	/**
	 * The class for which resolution is performed.
	 */
	private final VerifiedClass clazz;

	/**
	 * The constant pool of the class for which resolution is performed.
	 */
	private final ConstantPoolGen cpg;

	/**
	 * Builds the resolver.
	 * 
	 * @param clazz the class, the targets of whose instructions will be resolved
	 */
	Resolver(VerifiedClass clazz) {
		this.clazz = clazz;
		this.cpg = clazz.getConstantPool();
	}

	public Optional<Field> resolvedFieldFor(FieldInstruction fi) {
		ReferenceType holder = fi.getReferenceType(cpg);
		if (holder instanceof ObjectType) {
			String name = fi.getFieldName(cpg);
			Class<?> type = clazz.jar.bcelToClass.of(fi.getFieldType(cpg));
	
			return ThrowIncompleteClasspathError.insteadOfClassNotFoundException
				(() -> clazz.jar.classLoader.resolveField(((ObjectType) holder).getClassName(), name, type));
		}
	
		return Optional.empty();
	}

	public Optional<? extends Executable> resolvedExecutableFor(InvokeInstruction invoke) {
		if (invoke instanceof INVOKEDYNAMIC) {
			Bootstraps bootstraps = clazz.bootstraps;
			return bootstraps.getTargetOf(bootstraps.getBootstrapFor((INVOKEDYNAMIC) invoke));
		}

		String methodName = invoke.getMethodName(cpg);
		ReferenceType receiver = invoke.getReferenceType(cpg);
		// it is possible to call a method on an array: in that case, the callee is a method of java.lang.Object
		String receiverClassName = receiver instanceof ObjectType ? ((ObjectType) receiver).getClassName() : "java.lang.Object";
		Class<?>[] args = clazz.jar.bcelToClass.of(invoke.getArgumentTypes(cpg));

		if (invoke instanceof INVOKESPECIAL && Const.CONSTRUCTOR_NAME.equals(methodName))
			return resolveConstructorWithPossiblyExpandedArgs(receiverClassName, args);
		else {
			Class<?> returnType = clazz.jar.bcelToClass.of(invoke.getReturnType(cpg));

			if (invoke instanceof INVOKEINTERFACE)
				return resolveInterfaceMethodWithPossiblyExpandedArgs(receiverClassName, methodName, args, returnType);
			else
				return resolveMethodWithPossiblyExpandedArgs(receiverClassName, methodName, args, returnType);
		}
	}

	/**
	 * Yields the resolved constructor in the given class with the given arguments.
	 * If the constructor is an {@code @@Entry} of a class already instrumented, it will yield its version with
	 * the instrumentation arguments added at its end.
	 * 
	 * @param className the name of the class where the constructor is looked for
	 * @param args the arguments types of the constructor
	 * @return the constructor, if any
	 */
	Optional<Constructor<?>> resolveConstructorWithPossiblyExpandedArgs(String className, Class<?>[] args) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Optional<Constructor<?>> result = clazz.jar.classLoader.resolveConstructor(className, args);
			// we try to add the instrumentation arguments. This is important when
			// a bootstrap calls an entry of a jar already installed (and instrumented)
			// in blockchain. In that case, it will find the target only with these
			// extra arguments added during instrumentation
			return result.isPresent() ? result : clazz.jar.classLoader.resolveConstructor(className, expandArgsForEntry(args));
		});
	}

	/**
	 * Yields the resolved method from the given class with the given name, arguments and return type.
	 * If the method is an {@code @@Entry} of a class already instrumented, it will yield its version with
	 * the instrumentation arguments added at its end.
	 * 
	 * @param className the name of the class from where the method is looked for
	 * @param methodName the name of the method
	 * @param args the arguments types of the method
	 * @param returnType the return type of the method
	 * @return the method, if any
	 */
	Optional<java.lang.reflect.Method> resolveMethodWithPossiblyExpandedArgs(String className, String methodName, Class<?>[] args, Class<?> returnType) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Optional<java.lang.reflect.Method> result = clazz.jar.classLoader.resolveMethod(className, methodName, args, returnType);
			return result.isPresent() ? result : clazz.jar.classLoader.resolveMethod(className, methodName, expandArgsForEntry(args), returnType);
		});
	}

	/**
	 * Yields the resolved method from the given class with the given name, arguments and return type,
	 * as it would be resolved for a call to an interface method.
	 * If the method is an {@code @@Entry} of a class already instrumented, it will yield its version with
	 * the instrumentation arguments added at its end.
	 * 
	 * @param className the name of the class from where the method is looked for
	 * @param methodName the name of the method
	 * @param args the arguments types of the method
	 * @param returnType the return type of the method
	 * @return the method, if any
	 */
	Optional<java.lang.reflect.Method> resolveInterfaceMethodWithPossiblyExpandedArgs(String className, String methodName, Class<?>[] args, Class<?> returnType) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Optional<java.lang.reflect.Method> result = clazz.jar.classLoader.resolveInterfaceMethod(className, methodName, args, returnType);
			return result.isPresent() ? result : clazz.jar.classLoader.resolveInterfaceMethod(className, methodName, expandArgsForEntry(args), returnType);
		});
	}

	private Class<?>[] expandArgsForEntry(Class<?>[] args) throws ClassNotFoundException {
		Class<?>[] expandedArgs = new Class<?>[args.length + 2];
		System.arraycopy(args, 0, expandedArgs, 0, args.length);
		expandedArgs[args.length] = clazz.jar.classLoader.contractClass;
		expandedArgs[args.length + 1] = Dummy.class;
		return expandedArgs;
	}
}