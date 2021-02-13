package io.takamaka.code.instrumentation.internal.instrumentationsOfMethod;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import io.takamaka.code.constants.Constants;
import io.takamaka.code.instrumentation.InstrumentationConstants;
import io.takamaka.code.instrumentation.internal.InstrumentedClassImpl;
import io.takamaka.code.verification.Dummy;

/**
 * Passes the trailing implicit parameters to calls to methods annotated as {@code @@FromContract}.
 * They are the caller and the payer of the callee and {@code null} (as a dummy argument).
 */
public class AddExtraArgsToCallsToFromContract extends InstrumentedClassImpl.Builder.MethodLevelInstrumentation {
	private final static ObjectType CONTRACT_OT = new ObjectType(Constants.CONTRACT_NAME);
	private final static ObjectType RUNTIME_OT = new ObjectType(Constants.RUNTIME_NAME);
	private final static ObjectType DUMMY_OT = new ObjectType(Dummy.class.getName());

	public AddExtraArgsToCallsToFromContract(InstrumentedClassImpl.Builder builder, MethodGen method) {
		builder.super(method);

		if (!method.isAbstract()) {
			InstructionList il = method.getInstructionList();
			List<InstructionHandle> callsToFromContract = StreamSupport.stream(il.spliterator(), false)
				.filter(ih -> isCallToFromContract(ih.getInstruction())).collect(Collectors.toList());

			for (InstructionHandle ih: callsToFromContract)
				passExtraArgsToCallToFromContract(il, ih, method.getName());
		}
	}

	/**
	 * Passes the trailing implicit parameters to the given call to a {@@code @FromContract}. They
	 * are the caller, the payer (if any) and {@code null} (for the dummy argument).
	 * 
	 * @param il the instructions of the method being instrumented
	 * @param ih the call to the entry
	 * @param callee the name of the method where the calls are being looked for
	 */
	private void passExtraArgsToCallToFromContract(InstructionList il, InstructionHandle ih, String callee) {
		InvokeInstruction invoke = (InvokeInstruction) ih.getInstruction();
		Type[] args = invoke.getArgumentTypes(cpg);
		String methodName = invoke.getMethodName(cpg);
		Type returnType = invoke.getReturnType(cpg);
		int slots = Stream.of(args).mapToInt(Type::getSize).sum();
		
		if (invoke instanceof INVOKEDYNAMIC) {
			INVOKEDYNAMIC invokedynamic = (INVOKEDYNAMIC) invoke;
			ConstantInvokeDynamic cid = (ConstantInvokeDynamic) cpg.getConstant(invokedynamic.getIndex());

			// this is an invokedynamic that calls a @FromContract: we must capture the calling contract
			Type[] expandedArgs = new Type[args.length + 1];
			System.arraycopy(args, 0, expandedArgs, 1, args.length);
			expandedArgs[0] = new ObjectType(className);
			ConstantInvokeDynamic expandedCid = new ConstantInvokeDynamic(cid.getBootstrapMethodAttrIndex(),
				cpg.addNameAndType(methodName, Type.getMethodSignature(returnType, expandedArgs)));
			int index = addInvokeDynamicToConstantPool(expandedCid);
			INVOKEDYNAMIC copied = (INVOKEDYNAMIC) invokedynamic.copy();
			copied.setIndex(index);
			ih.setInstruction(copied);

			// we park the arguments of the invokedynamic into new local variables
			int usedLocals = method.getMaxLocals();
			int offset = slots;
			for (int pos = args.length - 1; pos >= 0; pos--) {
				offset -= args[pos].getSize();
				il.insert(ih, InstructionFactory.createStore(args[pos], usedLocals + offset));
			}

			// we added the first, extra parameter (the caller)
			il.insert(ih, InstructionConst.ALOAD_0);

			// we push back the previous arguments of the invokedynamic
			offset = 0;
			for (Type arg: args) {
				il.insert(ih, InstructionFactory.createLoad(arg, usedLocals + offset));
				offset += arg.getSize();
			}
		}
		else {
			Type[] expandedArgs = new Type[args.length + 2];
			System.arraycopy(args, 0, expandedArgs, 0, args.length);
			expandedArgs[args.length] = CONTRACT_OT;
			expandedArgs[args.length + 1] = DUMMY_OT;

			Runnable error = () -> {
				throw new IllegalStateException("Cannot find stack pushers for calls inside " + callee);
			};

			boolean onThis = pushers.getPushers(ih, slots + 1, cpg, error).map(InstructionHandle::getInstruction).allMatch(ins -> ins instanceof LoadInstruction && ((LoadInstruction) ins).getIndex() == 0);

			if (onThis) {
				Type[] ourArgs = method.getArgumentTypes();
				if (verifiedClass.getJar().getAnnotations().isFromContract(className, method.getName(), ourArgs, method.getReturnType())) {
					int ourArgsSlots = Stream.of(ourArgs).mapToInt(Type::getSize).sum();
					// the call is inside a @FromContract: its last one minus argument is the caller: we pass it
					ih.setInstruction(new LoadCaller(ourArgsSlots + 1));
				}
				else {
					// the call must be inside a lambda that is part of a @FromContract: since it has no caller argument,
					// we must call this.caller() and pass its return value as caller for the target method
					ih.setInstruction(InstructionConst.ALOAD_0);
					ih = il.append(ih, factory.createInvoke(Constants.STORAGE_NAME, InstrumentationConstants.CALLER, CONTRACT_OT, Type.NO_ARGS, Const.INVOKESPECIAL));
				}

				il.append(ih, factory.createInvoke(invoke.getClassName(cpg), methodName, returnType, expandedArgs, invoke.getOpcode()));
				il.append(ih, factory.createGetStatic(Dummy.class.getName(), "ON_THIS", DUMMY_OT));
			}
			else {
				// the call must be inside a contract "this": we pass it
				ih.setInstruction(InstructionConst.ALOAD_0);
				il.append(ih, factory.createInvoke(invoke.getClassName(cpg), methodName, returnType, expandedArgs, invoke.getOpcode()));
				il.append(ih, InstructionConst.ACONST_NULL); // we pass null as Dummy
			}
		}
	}

	/**
	 * An ALOAD instruction that is used to load the calling contract.
	 * This allows us to distinguish the instruction from a normal ALOAD.
	 */
	class LoadCaller extends ALOAD {
		private LoadCaller(int n) {
			super(n);
		}
	}

	/**
	 * Determines if the given instruction calls a method annotated as {@code @@FromContract}.
	 * 
	 * @param instruction the instruction
	 * @return true if and only if that condition holds
	 */
	private boolean isCallToFromContract(Instruction instruction) {
		if (instruction instanceof INVOKEDYNAMIC)
			return bootstrapMethodsThatWillRequireExtraThis.contains(bootstraps.getBootstrapFor((INVOKEDYNAMIC) instruction));
		else if (instruction instanceof InvokeInstruction) {
			InvokeInstruction invoke = (InvokeInstruction) instruction;
			ReferenceType receiver = invoke.getReferenceType(cpg);
			// we do not consider calls added by instrumentation
			if (receiver instanceof ObjectType && !receiver.equals(RUNTIME_OT))
				return verifiedClass.getJar().getAnnotations().isFromContract(((ObjectType) receiver).getClassName(),
					invoke.getMethodName(cpg), invoke.getArgumentTypes(cpg), invoke.getReturnType(cpg));
		}

		return false;
	}

	/**
	 * BCEL does not (yet?) provide a method to add an invokedynamic constant into a
	 * constant pool. Hence we have to rely to a trick: first we add a new integer
	 * constant to the constant pool; then we replace it with the invokedynamic
	 * constant. Ugly, but it currently seem to be the only way.
	 * 
	 * @param cid the constant to add
	 * @return the index at which the constant has been added
	 */
	private int addInvokeDynamicToConstantPool(ConstantInvokeDynamic cid) {
		// first we check if an equal constant method handle was already in the constant pool
		int size = cpg.getSize(), index;
		for (index = 0; index < size; index++)
			if (cpg.getConstant(index) instanceof ConstantInvokeDynamic) {
				ConstantInvokeDynamic c = (ConstantInvokeDynamic) cpg.getConstant(index);
				if (c.getBootstrapMethodAttrIndex() == cid.getBootstrapMethodAttrIndex()
						&& c.getNameAndTypeIndex() == cid.getNameAndTypeIndex())
					return index; // found
			}

		// otherwise, we first add an integer that was not already there
		int counter = 0;
		do {
			index = cpg.addInteger(counter++);
		}
		while (cpg.getSize() == size);

		// and then replace the integer constant with the method handle constant
		cpg.setConstant(index, cid);

		return index;
	}
}