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

import io.hotmoka.verification.Bootstraps;
import io.hotmoka.verification.Dummy;
import io.hotmoka.verification.Resolver;
import io.hotmoka.verification.ThrowIncompleteClasspathError;

/**
 * An utility that implements resolving algorithms for field and methods.
 */
public class ResolverImpl implements Resolver {

	/**
	 * The class for which resolution is performed.
	 */
	private final VerifiedClassImpl verifiedClass;

	/**
	 * The constant pool of the class for which resolution is performed.
	 */
	private final ConstantPoolGen cpg;

	/**
	 * Builds the resolver.
	 * 
	 * @param clazz the class, the targets of whose instructions will be resolved
	 */
	ResolverImpl(VerifiedClassImpl clazz) {
		this.verifiedClass = clazz;
		this.cpg = clazz.getConstantPool();
	}

	@Override
	public Optional<Field> resolvedFieldFor(FieldInstruction fi) throws ClassNotFoundException {
		ReferenceType holder = fi.getReferenceType(cpg);
		if (holder instanceof ObjectType) {
			String name = fi.getFieldName(cpg);
			Class<?> type = verifiedClass.jar.bcelToClass.of(fi.getFieldType(cpg));
	
			return ThrowIncompleteClasspathError.insteadOfClassNotFoundException
				(() -> verifiedClass.jar.classLoader.resolveField(((ObjectType) holder).getClassName(), name, type));
		}
	
		return Optional.empty();
	}

	@Override
	public Optional<? extends Executable> resolvedExecutableFor(InvokeInstruction invoke) throws ClassNotFoundException {
		if (invoke instanceof INVOKEDYNAMIC) {
			Bootstraps bootstraps = verifiedClass.bootstraps;
			return bootstraps.getTargetOf(bootstraps.getBootstrapFor((INVOKEDYNAMIC) invoke));
		}

		String methodName = invoke.getMethodName(cpg);
		ReferenceType receiver = invoke.getReferenceType(cpg);
		// it is possible to call a method on an array: in that case, the callee is a method of java.lang.Object
		String receiverClassName = receiver instanceof ObjectType ? ((ObjectType) receiver).getClassName() : "java.lang.Object";
		Class<?>[] args = verifiedClass.jar.bcelToClass.of(invoke.getArgumentTypes(cpg));

		if (invoke instanceof INVOKESPECIAL && Const.CONSTRUCTOR_NAME.equals(methodName))
			return resolveConstructorWithPossiblyExpandedArgs(receiverClassName, args);
		else {
			Class<?> returnType = verifiedClass.jar.bcelToClass.of(invoke.getReturnType(cpg));

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
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	Optional<Constructor<?>> resolveConstructorWithPossiblyExpandedArgs(String className, Class<?>[] args) throws ClassNotFoundException {
		Optional<Constructor<?>> result = verifiedClass.jar.classLoader.resolveConstructor(className, args);
		// we try to add the instrumentation arguments. This is important when
		// a bootstrap calls an entry of a jar already installed (and instrumented)
		// in blockchain. In that case, it will find the target only with these
		// extra arguments added during instrumentation
		return result.isPresent() ? result : verifiedClass.jar.classLoader.resolveConstructor(className, expandArgsForFromContract(args));
	}

	/**
	 * Yields the resolved method from the given class with the given name, arguments and return type.
	 * If the method is a {@code @@FromContract} of a class already instrumented, it will yield its version with
	 * the instrumentation arguments added at its end.
	 * 
	 * @param className the name of the class from where the method is looked for
	 * @param methodName the name of the method
	 * @param args the arguments types of the method
	 * @param returnType the return type of the method
	 * @return the method, if any
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	Optional<java.lang.reflect.Method> resolveMethodWithPossiblyExpandedArgs(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		Optional<java.lang.reflect.Method> result = verifiedClass.jar.classLoader.resolveMethod(className, methodName, args, returnType);
		return result.isPresent() ? result : verifiedClass.jar.classLoader.resolveMethod(className, methodName, expandArgsForFromContract(args), returnType);
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
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	Optional<java.lang.reflect.Method> resolveInterfaceMethodWithPossiblyExpandedArgs(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		Optional<java.lang.reflect.Method> result = verifiedClass.jar.classLoader.resolveInterfaceMethod(className, methodName, args, returnType);
		return result.isPresent() ? result : verifiedClass.jar.classLoader.resolveInterfaceMethod(className, methodName, expandArgsForFromContract(args), returnType);
	}

	private Class<?>[] expandArgsForFromContract(Class<?>[] args) {
		Class<?>[] expandedArgs = new Class<?>[args.length + 2];
		System.arraycopy(args, 0, expandedArgs, 0, args.length);
		expandedArgs[args.length] = verifiedClass.jar.classLoader.getContract();
		expandedArgs[args.length + 1] = Dummy.class;
		return expandedArgs;
	}
}