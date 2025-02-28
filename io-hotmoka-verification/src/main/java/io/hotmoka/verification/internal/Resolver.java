/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.verification.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

import io.hotmoka.verification.BcelToClassTransformers;
import io.hotmoka.verification.api.BcelToClassTransformer;
import io.hotmoka.verification.api.Bootstraps;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.verification.api.VerifiedJar;
import io.hotmoka.whitelisting.Dummy;

/**
 * A utility that implements resolving algorithms for field and methods.
 */
public class Resolver {

	/**
	 * The class for which resolution is performed.
	 */
	private final VerifiedClassImpl verifiedClass;

	/**
	 * The class loader used to load {@code verifiedClass}.
	 */
	private final TakamakaClassLoader classLoader;

	/**
	 * A utility to transform BCEL types into classes.
	 */
	private final BcelToClassTransformer bcelToClass;

	/**
	 * The constant pool of the class for which resolution is performed.
	 */
	private final ConstantPoolGen cpg;

	/**
	 * Builds the resolver.
	 * 
	 * @param clazz the class, the targets of whose instructions will be resolved
	 */
	Resolver(VerifiedClassImpl clazz) {
		this.verifiedClass = clazz;
		VerifiedJar jar = clazz.getJar();
		this.classLoader = jar.getClassLoader();
		this.bcelToClass = BcelToClassTransformers.of(classLoader);
		this.cpg = clazz.getConstantPool();
	}


	/**
	 * Yields the field signature that would be accessed by the given instruction.
	 * 
	 * @param fi the instruction
	 * @return the signature, if any
	 * @throws IllegalJarException if some class of the Takamaka program cannot be found
	 */
	public Optional<Field> resolvedFieldFor(FieldInstruction fi) throws IllegalJarException {
		if (fi.getReferenceType(cpg) instanceof ObjectType ot) {
			String name = fi.getFieldName(cpg);

			try {
				Class<?> type = bcelToClass.of(fi.getFieldType(cpg));
				return classLoader.resolveField(ot.getClassName(), name, type);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalJarException(e);
			}
		}
	
		return Optional.empty();
	}

	/**
	 * Yields the method or constructor signature that would be accessed by the given instruction.
	 * At run time, that signature or one of its redefinitions (for non-private non-final methods) will be called.
	 * 
	 * @param invoke the instruction
	 * @return the signature
	 * @throws IllegalJarException if some class of the Takamaka program cannot be found
	 */
	public Optional<? extends Executable> resolvedExecutableFor(InvokeInstruction invoke) throws IllegalJarException {
		if (invoke instanceof INVOKEDYNAMIC invokedynamic) {
			Bootstraps bootstraps = verifiedClass.getBootstraps();
			return bootstraps.getTargetOf(bootstraps.getBootstrapFor(invokedynamic));
		}

		String methodName = invoke.getMethodName(cpg);
		ReferenceType receiver = invoke.getReferenceType(cpg);
		// it is possible to call a method on an array: in that case, the callee is a method of java.lang.Object
		String receiverClassName = receiver instanceof ObjectType ot ? ot.getClassName() : "java.lang.Object";

		try {
			Class<?>[] args = bcelToClass.of(invoke.getArgumentTypes(cpg));

			if (invoke instanceof INVOKESPECIAL && Const.CONSTRUCTOR_NAME.equals(methodName))
				return resolveConstructorWithPossiblyExpandedArgs(receiverClassName, args);
			else {
				Class<?> returnType = bcelToClass.of(invoke.getReturnType(cpg));

				if (invoke instanceof INVOKEINTERFACE)
					return resolveInterfaceMethodWithPossiblyExpandedArgs(receiverClassName, methodName, args, returnType);
				else
					return resolveMethodWithPossiblyExpandedArgs(receiverClassName, methodName, args, returnType);
			}
		}
		catch (ClassNotFoundException e) {
			throw new IllegalJarException(e);
		}
	}

	/**
	 * Yields the resolved constructor in the given class with the given arguments.
	 * If the constructor is a {@code @@FromContract} of a class already instrumented,
	 * it will yield its version with the instrumentation arguments added at its end.
	 * 
	 * @param className the name of the class where the constructor is looked for
	 * @param args the arguments types of the constructor
	 * @return the constructor, if any
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
	 */
	Optional<Constructor<?>> resolveConstructorWithPossiblyExpandedArgs(String className, Class<?>[] args) throws ClassNotFoundException {
		Optional<Constructor<?>> result = classLoader.resolveConstructor(className, args);
		// we try to add the instrumentation arguments. This is important when
		// a bootstrap calls a from contract of a jar already installed (and instrumented)
		// in the node. In that case, it will find the target only with these
		// extra arguments added during instrumentation
		return result.isPresent() ? result : classLoader.resolveConstructor(className, expandArgsForFromContract(args));
	}

	/**
	 * Yields the resolved method from the given class with the given name, arguments and return type.
	 * If the method is a {@code @@FromContract} of a class already instrumented, it will yield
	 * its version with the instrumentation arguments added at its end.
	 * 
	 * @param className the name of the class from where the method is looked for
	 * @param methodName the name of the method
	 * @param args the arguments types of the method
	 * @param returnType the return type of the method
	 * @return the method, if any
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
	 */
	Optional<Method> resolveMethodWithPossiblyExpandedArgs(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		Optional<Method> result = classLoader.resolveMethod(className, methodName, args, returnType);
		return result.isPresent() ? result : classLoader.resolveMethod(className, methodName, expandArgsForFromContract(args), returnType);
	}

	/**
	 * Yields the resolved method from the given class with the given name, arguments and return type,
	 * as it would be resolved for a call to an interface method. If the method is a {@code @@FromContract}
	 * of a class already instrumented, it will yield its version with the instrumentation arguments added at its end.
	 * 
	 * @param className the name of the class from where the method is looked for
	 * @param methodName the name of the method
	 * @param args the arguments types of the method
	 * @param returnType the return type of the method
	 * @return the method, if any
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
	 */
	Optional<Method> resolveInterfaceMethodWithPossiblyExpandedArgs(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		Optional<Method> result = classLoader.resolveInterfaceMethod(className, methodName, args, returnType);
		return result.isPresent() ? result : classLoader.resolveInterfaceMethod(className, methodName, expandArgsForFromContract(args), returnType);
	}

	private Class<?>[] expandArgsForFromContract(Class<?>[] args) {
		Class<?>[] expandedArgs = new Class<?>[args.length + 2];
		System.arraycopy(args, 0, expandedArgs, 0, args.length);
		expandedArgs[args.length] = classLoader.getContract();
		expandedArgs[args.length + 1] = Dummy.class;
		return expandedArgs;
	}
}