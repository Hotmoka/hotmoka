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

package io.hotmoka.instrumentation.internal.instrumentationsOfClass;

import java.util.Optional;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodType;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.Type;

import io.hotmoka.instrumentation.internal.InstrumentationConstants;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl.Builder.ClassLevelInstrumentation;
import io.hotmoka.verification.api.IllegalJarException;

/**
 * An instrumentation that desugars bootstrap methods that invoke {@code @@FromContract} code.
 * They are the compilation of method references to {@code @@FromContract} code. Since
 * {@code @@FromContract} code receives extra parameters, we transform those bootstrap methods
 * by calling brand new target code, that calls the {@code @@FromContract} code with
 * a normal invoke instruction.
 */
public class DesugarBootstrapsInvokingFromContract extends ClassLevelInstrumentation {
	private final static short PRIVATE_SYNTHETIC = Const.ACC_PRIVATE | Const.ACC_SYNTHETIC;

	/**
	 * Builds the instrumentation.
	 * 
	 * @param builder the builder of the class being instrumented
	 */
	public DesugarBootstrapsInvokingFromContract(InstrumentedClassImpl.Builder builder) throws IllegalJarException {
		builder.super();

		for (var bootstrap: bootstraps.getBootstrapsLeadingToFromContract().toArray(BootstrapMethod[]::new))
			desugarBootstrapCallingFromContract(bootstrap);
	}

	private void desugarBootstrapCallingFromContract(BootstrapMethod bootstrap) throws IllegalJarException {
		if (bootstraps.lambdaIsFromContract(bootstrap))
			desugarLambdaFromContract(bootstrap);
		else
			desugarLambdaCallingFromContract(bootstrap);
	}

	private void desugarLambdaCallingFromContract(BootstrapMethod bootstrap) throws IllegalJarException {
		int[] args = bootstrap.getBootstrapArguments();
		if (args.length <= 1)
			throw new IllegalJarException("Not enough bootstrap arguments");

		if (!(cpg.getConstant(args[1]) instanceof ConstantMethodHandle mh))
			throw new IllegalJarException("Illegal constant");

		int invokeKind = mh.getReferenceKind();

		if (invokeKind == Const.REF_invokeStatic) {
			// we instrument bootstrap methods that call a static lambda that calls a @FromContract:
			// the problem is that the instrumentation of the @FromContract will need local 0 (this)
			// to pass the calling contract, consequently it must be made into an instance method

			if (!(cpg.getConstant(mh.getReferenceIndex()) instanceof ConstantMethodref mr))
				throw new IllegalJarException("Illegal constant");

			if (!(cpg.getConstant(mr.getNameAndTypeIndex()) instanceof ConstantNameAndType nt))
				throw new IllegalJarException("Illegal constant");

			if (!(cpg.getConstant(nt.getNameIndex()) instanceof ConstantUtf8 cu8))
				throw new IllegalJarException("Illegal constant");

			String methodName = cu8.getBytes();

			if (!(cpg.getConstant(nt.getSignatureIndex()) instanceof ConstantUtf8 cu8_2))
				throw new IllegalJarException("Illegal constant");

			String methodSignature = cu8_2.getBytes();

			Optional<MethodGen> old = getMethods()
				.filter(method -> method.getName().equals(methodName) && method.getSignature().equals(methodSignature) && method.isPrivate())
				.findFirst();

			old.ifPresent(method -> {
				// we can modify the method handle since the lambda is becoming an instance
				// method and all calls must be made through invokespecial
				mh.setReferenceKind(Const.REF_invokeSpecial);
				makeFromStaticToInstance(method);
				bootstrapMethodsThatWillRequireExtraThis.add(bootstrap);
			});
		}
	}

	private void desugarLambdaFromContract(BootstrapMethod bootstrap) {
		int[] args = bootstrap.getBootstrapArguments();
		ConstantMethodHandle mh = (ConstantMethodHandle) cpg.getConstant(args[1]);
		int invokeKind = mh.getReferenceKind();
		ConstantMethodref mr = (ConstantMethodref) cpg.getConstant(mh.getReferenceIndex());
		int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
		String entryClassName = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
		ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
		String entryName = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
		String entrySignature = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
		Type[] entryArgs = Type.getArgumentTypes(entrySignature);
		Type entryReturnType = Type.getReturnType(entrySignature);
		String implementedInterfaceMethosSignature = ((ConstantUtf8) cpg
				.getConstant(((ConstantMethodType) cpg.getConstant(args[2])).getDescriptorIndex())).getBytes();
		Type lambdaReturnType = Type.getReturnType(implementedInterfaceMethosSignature);

		// we replace the target code: it was an invokeX C.entry(pars):r and we transform it
		// into invokespecial className.lambda(C, pars):r where the name "lambda" is
		// not used in className. The extra parameter className is not added for
		// constructor references, since they create the new object themselves
		String lambdaName = getNewNameForPrivateMethod(InstrumentationConstants.EXTRA_LAMBDA);

		Type[] lambdaArgs;
		if (invokeKind == Const.REF_newInvokeSpecial)
			lambdaArgs = entryArgs;
		else {
			lambdaArgs = new Type[entryArgs.length + 1];
			System.arraycopy(entryArgs, 0, lambdaArgs, 1, entryArgs.length);
			lambdaArgs[0] = new ObjectType(entryClassName);
		}

		String lambdaSignature = Type.getMethodSignature(lambdaReturnType, lambdaArgs);

		// replace inside the bootstrap method
		args[1] = addMethodHandleToConstantPool(new ConstantMethodHandle(Const.REF_invokeSpecial,
			cpg.addMethodref(className, lambdaName, lambdaSignature)));

		// we create the target code: it is a new private synthetic instance method inside className,
		// called lambdaName and with signature lambdaSignature; its code loads all its
		// explicit parameters on the stack then calls the entry and returns its value (if any)
		InstructionList il = new InstructionList();
		if (invokeKind == Const.REF_newInvokeSpecial) {
			il.append(factory.createNew(entryClassName));
			if (lambdaReturnType != Type.VOID)
				il.append(InstructionConst.DUP);
		}

		int local = 1;
		for (Type arg : lambdaArgs) {
			il.append(InstructionFactory.createLoad(arg, local));
			local += arg.getSize();
		}

		il.append(factory.createInvoke(entryClassName, entryName, entryReturnType, entryArgs, invokeCorrespondingToBootstrapInvocationType(invokeKind)));
		il.append(InstructionFactory.createReturn(lambdaReturnType));

		addMethod(new MethodGen(PRIVATE_SYNTHETIC, lambdaReturnType, lambdaArgs, null, lambdaName, className, il, cpg));
		bootstrapMethodsThatWillRequireExtraThis.add(bootstrap);
	}

	private void makeFromStaticToInstance(MethodGen method) {
		method.isStatic(false);

		if (!method.isAbstract()) {
			// we increase the indexes of the local variables used in the method
			for (InstructionHandle ih: method.getInstructionList())
				if (ih.getInstruction() instanceof LocalVariableInstruction lvi) {
					int index = lvi.getIndex();

					if (lvi instanceof IINC iinc)
						ih.setInstruction(new IINC(index + 1, iinc.getIncrement()));
					else if (lvi instanceof LoadInstruction load)
						ih.setInstruction(InstructionFactory.createLoad(load.getType(cpg), index + 1));
					else if (lvi instanceof StoreInstruction store)
						ih.setInstruction(InstructionFactory.createStore(store.getType(cpg), index + 1));
				}
		}
	}
}