package io.takamaka.code.instrumentation.internal.instrumentationsOfClass;

import java.util.Optional;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodType;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Method;
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

import io.takamaka.code.instrumentation.internal.InstrumentedClass;
import io.takamaka.code.verification.Constants;
import it.univr.bcel.StackMapReplacer;

/**
 * An instrumentation that desugars bootstrap methods that invoke an entry as their target code.
 * They are the compilation of method references to entries. Since entries
 * receive extra parameters, we transform those bootstrap methods by calling
 * brand new target code, that calls the entry with a normal invoke instruction.
 */
public class DesugarBootstrapsInvokingEntries extends InstrumentedClass.Builder.ClassLevelInstrumentation {
	private final static String EXTRA_LAMBDA_NAME = Constants.INSTRUMENTATION_PREFIX + "lambda";

	public DesugarBootstrapsInvokingEntries(InstrumentedClass.Builder builder) {
		builder.super();
		verifiedClass.getBootstraps().getBootstrapsLeadingToEntries().forEach(this::desugarBootstrapCallingEntry);
	}

	private void desugarBootstrapCallingEntry(BootstrapMethod bootstrap) {
		if (verifiedClass.getBootstraps().lambdaIsEntry(bootstrap))
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
			Optional<Method> old = getMethods()
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
		String lambdaName = getNewNameForPrivateMethod(EXTRA_LAMBDA_NAME);

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

		MethodGen addedLambda = new MethodGen(InstrumentedClass.PRIVATE_SYNTHETIC, lambdaReturnType, lambdaArgs, null, lambdaName, className, il, cpg);
		addMethod(addedLambda, false);
		bootstrapMethodsThatWillRequireExtraThis.add(bootstrap);
	}

	private void makeFromStaticToInstance(Method old) {
		MethodGen _new = new MethodGen(old, className, cpg);
		_new.isStatic(false);
		if (!_new.isAbstract())
			// we increase the indexes of the local variables used in the method
			for (InstructionHandle ih : _new.getInstructionList()) {
				Instruction ins = ih.getInstruction();
				if (ins instanceof LocalVariableInstruction) {
					int index = ((LocalVariableInstruction) ins).getIndex();
					if (ins instanceof IINC)
						ih.setInstruction(new IINC(index + 1, ((IINC) ins).getIncrement()));
					else if (ins instanceof LoadInstruction)
						ih.setInstruction(InstructionFactory.createLoad(((LoadInstruction) ins).getType(cpg), index + 1));
					else if (ins instanceof StoreInstruction)
						ih.setInstruction(InstructionFactory.createStore(((LoadInstruction) ins).getType(cpg), index + 1));
				}
			}

		StackMapReplacer.of(_new);
		replaceMethod(old, _new.getMethod());
	}
}