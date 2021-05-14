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
import org.apache.bcel.generic.Instruction;
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

import io.hotmoka.instrumentation.InstrumentationConstants;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import it.univr.bcel.StackMapReplacer;

/**
 * An instrumentation that desugars bootstrap methods that invoke an entry as their target code.
 * They are the compilation of method references to entries. Since entries
 * receive extra parameters, we transform those bootstrap methods by calling
 * brand new target code, that calls the entry with a normal invoke instruction.
 */
public class DesugarBootstrapsInvokingEntries extends InstrumentedClassImpl.Builder.ClassLevelInstrumentation {
	private final static short PRIVATE_SYNTHETIC = Const.ACC_PRIVATE | Const.ACC_SYNTHETIC;

	public DesugarBootstrapsInvokingEntries(InstrumentedClassImpl.Builder builder) {
		builder.super();
		bootstraps.getBootstrapsLeadingToEntries().forEach(this::desugarBootstrapCallingEntry);
	}

	private void desugarBootstrapCallingEntry(BootstrapMethod bootstrap) {
		if (bootstraps.lambdaIsEntry(bootstrap))
			desugarLambdaEntry(bootstrap);
		else
			desugarLambdaCallingEntry(bootstrap);
	}

	private void desugarLambdaCallingEntry(BootstrapMethod bootstrap) {
		int[] args = bootstrap.getBootstrapArguments();
		ConstantMethodHandle mh = (ConstantMethodHandle) cpg.getConstant(args[1]);
		int invokeKind = mh.getReferenceKind();

		if (invokeKind == Const.REF_invokeStatic) {
			// we instrument bootstrap methods that call a static lambda that calls an entry:
			// the problem is that the instrumentation of the entry will need local 0 (this)
			// to pass the calling contract, consequently it must be made into an instance method

			ConstantMethodref mr = (ConstantMethodref) cpg.getConstant(mh.getReferenceIndex());
			ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
			String methodName = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
			String methodSignature = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
			Optional<MethodGen> old = getMethods()
				.filter(method -> method.getName().equals(methodName)
						&& method.getSignature().equals(methodSignature) && method.isPrivate())
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

	private void desugarLambdaEntry(BootstrapMethod bootstrap) {
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

		il.append(factory.createInvoke(entryClassName, entryName, entryReturnType, entryArgs,
			invokeCorrespondingToBootstrapInvocationType(invokeKind)));
		il.append(InstructionFactory.createReturn(lambdaReturnType));

		MethodGen addedLambda = new MethodGen(PRIVATE_SYNTHETIC, lambdaReturnType, lambdaArgs, null, lambdaName, className, il, cpg);
		addMethod(addedLambda, false);
		bootstrapMethodsThatWillRequireExtraThis.add(bootstrap);
	}

	private void makeFromStaticToInstance(MethodGen method) {
		method.isStatic(false);
		if (!method.isAbstract()) {
			// we increase the indexes of the local variables used in the method
			for (InstructionHandle ih: method.getInstructionList()) {
				Instruction ins = ih.getInstruction();
				if (ins instanceof LocalVariableInstruction) {
					int index = ((LocalVariableInstruction) ins).getIndex();
					if (ins instanceof IINC)
						ih.setInstruction(new IINC(index + 1, ((IINC) ins).getIncrement()));
					else if (ins instanceof LoadInstruction)
						ih.setInstruction(InstructionFactory.createLoad(((LoadInstruction) ins).getType(cpg), index + 1));
					else if (ins instanceof StoreInstruction)
						ih.setInstruction(InstructionFactory.createStore(((StoreInstruction) ins).getType(cpg), index + 1));
				}
			}

			StackMapReplacer.of(method);
		}
	}
}