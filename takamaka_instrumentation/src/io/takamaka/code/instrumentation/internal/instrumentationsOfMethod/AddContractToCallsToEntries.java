package io.takamaka.code.instrumentation.internal.instrumentationsOfMethod;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import io.takamaka.code.instrumentation.internal.InstrumentedClassImpl;
import io.takamaka.code.verification.Constants;
import io.takamaka.code.verification.Dummy;

/**
 * Passes the trailing implicit parameters to calls to entries. They are the
 * contract where the entry is called and {@code null} (for the dummy argument).
 */
public class AddContractToCallsToEntries extends InstrumentedClassImpl.Builder.MethodLevelInstrumentation {
	private final static ObjectType CONTRACT_OT = new ObjectType(Constants.CONTRACT_NAME);
	private final static ObjectType DUMMY_OT = new ObjectType(Dummy.class.getName());

	public AddContractToCallsToEntries(InstrumentedClassImpl.Builder builder, MethodGen method) {
		builder.super(method);

		if (!method.isAbstract()) {
			InstructionList il = method.getInstructionList();
			List<InstructionHandle> callsToEntries = StreamSupport.stream(il.spliterator(), false)
					.filter(ih -> isCallToEntry(ih.getInstruction())).collect(Collectors.toList());

			for (InstructionHandle ih : callsToEntries)
				passContractToCallToEntry(il, ih, method.getName());
		}
	}

	/**
	 * Passes the trailing implicit parameters to the given call to an entry. They
	 * are the contract where the entry is called and {@code null} (for the dummy argument).
	 * 
	 * @param il the instructions of the method being instrumented
	 * @param ih the call to the entry
	 * @param callee the name of the method where the calls are being looked for
	 */
	private void passContractToCallToEntry(InstructionList il, InstructionHandle ih, String callee) {
		InvokeInstruction invoke = (InvokeInstruction) ih.getInstruction();
		if (invoke instanceof INVOKEDYNAMIC) {
			INVOKEDYNAMIC invokedynamic = (INVOKEDYNAMIC) invoke;
			String methodName = invoke.getMethodName(cpg);
			ConstantInvokeDynamic cid = (ConstantInvokeDynamic) cpg.getConstant(invokedynamic.getIndex());

			// this is an invokedynamic that calls an entry: we must capture the calling contract
			Type[] args = invoke.getArgumentTypes(cpg);
			Type[] expandedArgs = new Type[args.length + 1];
			System.arraycopy(args, 0, expandedArgs, 1, args.length);
			expandedArgs[0] = new ObjectType(className);
			ConstantInvokeDynamic expandedCid = new ConstantInvokeDynamic(cid.getBootstrapMethodAttrIndex(), cpg
					.addNameAndType(methodName, Type.getMethodSignature(invoke.getReturnType(cpg), expandedArgs)));
			int index = addInvokeDynamicToConstantPool(expandedCid);
			INVOKEDYNAMIC copied = (INVOKEDYNAMIC) invokedynamic.copy();
			copied.setIndex(index);
			ih.setInstruction(copied);

			int slots = Stream.of(args).mapToInt(Type::getSize).sum();
			forEachPusher(ih, slots, where -> {
				il.append(where, where.getInstruction());
				where.setInstruction(InstructionConst.ALOAD_0);
			}, () -> {
				throw new IllegalStateException("Cannot find stack pushers for calls inside " + callee);
			});
		}
		else {
			Type[] args = invoke.getArgumentTypes(cpg);
			Type[] expandedArgs = new Type[args.length + 2];
			System.arraycopy(args, 0, expandedArgs, 0, args.length);
			expandedArgs[args.length] = CONTRACT_OT;
			expandedArgs[args.length + 1] = DUMMY_OT;

			ih.setInstruction(InstructionConst.ALOAD_0); // the call must be inside a contract "this"
			il.append(ih, factory.createInvoke(invoke.getClassName(cpg), invoke.getMethodName(cpg),
					invoke.getReturnType(cpg), expandedArgs, invoke.getOpcode()));
			il.append(ih, InstructionConst.ACONST_NULL); // we pass null as Dummy
		}
	}

	/**
	 * Determines if the given instruction calls an entry.
	 * 
	 * @param instruction the instruction
	 * @return true if and only if that condition holds
	 */
	private boolean isCallToEntry(Instruction instruction) {
		if (instruction instanceof INVOKEDYNAMIC)
			return bootstrapMethodsThatWillRequireExtraThis
				.contains(verifiedClass.getBootstraps().getBootstrapFor((INVOKEDYNAMIC) instruction));
		else if (instruction instanceof InvokeInstruction) {
			InvokeInstruction invoke = (InvokeInstruction) instruction;
			ReferenceType receiver = invoke.getReferenceType(cpg);
			if (receiver instanceof ObjectType)
				return verifiedClass.getJar().getAnnotations().isEntryPossiblyAlreadyInstrumented(((ObjectType) receiver).getClassName(),
					invoke.getMethodName(cpg), invoke.getSignature(cpg));
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