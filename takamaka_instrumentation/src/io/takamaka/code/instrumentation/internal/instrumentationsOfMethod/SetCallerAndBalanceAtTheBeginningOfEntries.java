package io.takamaka.code.instrumentation.internal.instrumentationsOfMethod;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.RET;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.StackConsumer;
import org.apache.bcel.generic.StackProducer;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.Type;

import io.takamaka.code.instrumentation.internal.InstrumentedClass;
import io.takamaka.code.instrumentation.internal.HeightAtBytecode;
import io.takamaka.code.verification.Constants;
import io.takamaka.code.verification.Dummy;

/**
 * Sets the caller at the beginning of entries and updates the balance
 * at the beginning of payable entries.
 */
public class SetCallerAndBalanceAtTheBeginningOfEntries extends InstrumentedClass.Builder.MethodLevelInstrumentation {
	private final static ObjectType CONTRACT_OT = new ObjectType(Constants.CONTRACT_NAME);
	private final static ObjectType DUMMY_OT = new ObjectType(Dummy.class.getName());
	private final static String PAYABLE_ENTRY = "payableEntry";
	private final static String ENTRY = "entry";
	private final static Type[] ENTRY_ARGS = { CONTRACT_OT };

	public SetCallerAndBalanceAtTheBeginningOfEntries(InstrumentedClass.Builder builder, MethodGen method) {
		builder.super(method);

		Optional<Class<?>> callerContract;
		if (isContract && (callerContract = verifiedClass.getJar().getAnnotations().isEntry(className, method.getName(),
				method.getArgumentTypes(), method.getReturnType())).isPresent())
			instrumentEntry(method, callerContract.get(), verifiedClass.getJar().getAnnotations().isPayable(className, method.getName(),
				method.getArgumentTypes(), method.getReturnType()));
	}

	/**
	 * Instruments an entry, by setting the caller and transferring funds for payable entries.
	 * 
	 * @param method the entry
	 * @param callerContract the class of the caller contract
	 * @param isPayable true if and only if the entry is payable
	 */
	private void instrumentEntry(MethodGen method, Class<?> callerContract, boolean isPayable) {
		// slotForCaller is the local variable used for the extra "caller" parameter;
		// there is no need to shift the local variables one slot up, since the use
		// of caller is limited to the prolog of the synthetic code
		int slotForCaller = addExtraParameters(method);
		if (!method.isAbstract())
			setCallerAndBalance(method, callerContract, slotForCaller, isPayable);
	}

	/**
	 * Instruments an entry by calling the contract method that sets caller and balance.
	 * 
	 * @param method the entry
	 * @param callerContract the class of the caller contract
	 * @param slotForCaller the local variable for the caller implicit argument
	 * @param isPayable true if and only if the entry is payable
	 */
	private void setCallerAndBalance(MethodGen method, Class<?> callerContract, int slotForCaller, boolean isPayable) {
		InstructionList il = method.getInstructionList();

		// the call to the method that sets caller and balance cannot be put at the
		// beginning of the method, always: for constructors, Java bytecode requires
		// that their code starts with a call to a constructor of the superclass
		InstructionHandle where = determineWhereToSetCallerAndBalance(il, method, slotForCaller);
		InstructionHandle start = il.getStart();

		il.insert(start, InstructionFactory.createThis());
		il.insert(start, InstructionFactory.createLoad(CONTRACT_OT, slotForCaller));
		if (callerContract != classLoader.getContract())
			il.insert(start, factory.createCast(CONTRACT_OT, Type.getType(callerContract)));
		if (isPayable) {
			// a payable entry method can have a first argument of type int/long/BigInteger
			Type amountType = method.getArgumentType(0);
			il.insert(start, InstructionFactory.createLoad(amountType, 1));
			Type[] paybleEntryArgs = new Type[] { CONTRACT_OT, amountType };
			il.insert(where, factory.createInvoke(className, PAYABLE_ENTRY, Type.VOID, paybleEntryArgs,
					Const.INVOKESPECIAL));
		}
		else
			il.insert(where, factory.createInvoke(className, ENTRY, Type.VOID, ENTRY_ARGS, Const.INVOKESPECIAL));
	}

	/**
	 * Entries call {@link io.takamaka.code.lang.Contract#entry(Contract)} or
	 * {@link io.takamaka.code.lang.Contract#payableEntry(Contract,BigInteger)} at their
	 * beginning, to set the caller and the balance of the called entry. In general,
	 * such call can be placed at the very beginning of the code. The only problem
	 * is related to constructors, that require their code to start with a call to a
	 * constructor of their superclass. In that case, this method finds the place
	 * where that contractor of the superclass is called: after which, we can add
	 * the call that sets caller and balance.
	 * 
	 * @param il the list of instructions of the entry
	 * @param method the entry
	 * @param slotForCaller the local where the caller contract is passed to the entry
	 * @return the instruction before which the code that sets caller and balance can be placed
	 */
	private InstructionHandle determineWhereToSetCallerAndBalance(InstructionList il, MethodGen method, int slotForCaller) {
		InstructionHandle start = il.getStart();

		if (method.getName().equals(Const.CONSTRUCTOR_NAME)) {
			// we have to identify the call to the constructor of the superclass:
			// the code of a constructor normally starts with an aload_0 whose value is consumed
			// by a call to a constructor of the superclass. In the middle, slotForCaller is not expected
			// to be modified. Note that this is the normal situation, as results from a normal
			// Java compiler. In principle, the Java bytecode might instead do very weird things,
			// including calling two constructors of the superclass at different places. In all such cases
			// this method fails and rejects the code: such non-standard code is not supported by Takamaka
			Instruction startInstruction = start.getInstruction();
			if (startInstruction.getOpcode() == Const.ALOAD_0 || (startInstruction.getOpcode() == Const.ALOAD
					&& ((LoadInstruction) startInstruction).getIndex() == 0)) {
				Set<InstructionHandle> callsToConstructorsOfSuperclass = new HashSet<>();

				HeightAtBytecode seed = new HeightAtBytecode(start.getNext(), 1);
				Set<HeightAtBytecode> seen = new HashSet<>();
				seen.add(seed);
				List<HeightAtBytecode> workingSet = new ArrayList<>();
				workingSet.add(seed);

				do {
					HeightAtBytecode current = workingSet.remove(workingSet.size() - 1);
					int stackHeightAfterBytecode = current.stackHeightBeforeBytecode;
					Instruction bytecode = current.ih.getInstruction();

					if (bytecode instanceof StoreInstruction) {
						int modifiedLocal = ((StoreInstruction) bytecode).getIndex();
						int size = ((StoreInstruction) bytecode).getType(cpg).getSize();
						if (modifiedLocal == slotForCaller || (size == 2 && modifiedLocal == slotForCaller - 1))
							throw new IllegalStateException("Unexpected modification of local " + slotForCaller
									+ " before initialization of " + className);
					}

					if (bytecode instanceof StackProducer)
						stackHeightAfterBytecode += ((StackProducer) bytecode).produceStack(cpg);
					if (bytecode instanceof StackConsumer)
						stackHeightAfterBytecode -= ((StackConsumer) bytecode).consumeStack(cpg);

					if (stackHeightAfterBytecode == 0) {
						// found a consumer of the aload_0: is it really a call to a constructor of the superclass?
						if (bytecode instanceof INVOKESPECIAL
								&& ((INVOKESPECIAL) bytecode).getClassName(cpg).equals(verifiedClass.getSuperclassName())
								&& ((INVOKESPECIAL) bytecode).getMethodName(cpg).equals(Const.CONSTRUCTOR_NAME))
							callsToConstructorsOfSuperclass.add(current.ih);
						else
							throw new IllegalStateException("Unexpected consumer of local 0 " + bytecode + " before initialization of " + className);
					}
					else if (bytecode instanceof GotoInstruction) {
						HeightAtBytecode added = new HeightAtBytecode(((GotoInstruction) bytecode).getTarget(),
								stackHeightAfterBytecode);
						if (seen.add(added))
							workingSet.add(added);
					}
					else if (bytecode instanceof IfInstruction) {
						HeightAtBytecode added = new HeightAtBytecode(current.ih.getNext(), stackHeightAfterBytecode);
						if (seen.add(added))
							workingSet.add(added);
						added = new HeightAtBytecode(((IfInstruction) bytecode).getTarget(), stackHeightAfterBytecode);
						if (seen.add(added))
							workingSet.add(added);
					}
					else if (bytecode instanceof BranchInstruction || bytecode instanceof ATHROW
							|| bytecode instanceof RETURN || bytecode instanceof RET)
						throw new IllegalStateException("Unexpected instruction " + bytecode + " before initialization of " + className);
					else {
						HeightAtBytecode added = new HeightAtBytecode(current.ih.getNext(),
								stackHeightAfterBytecode);
						if (seen.add(added))
							workingSet.add(added);
					}
				}
				while (!workingSet.isEmpty());

				if (callsToConstructorsOfSuperclass.size() == 1)
					return callsToConstructorsOfSuperclass.iterator().next().getNext();
				else
					throw new IllegalStateException("Cannot identify single call to constructor of superclass inside a constructor ot " + className);
			}
			else
				throw new IllegalStateException("Constructor of " + className + " does not start with aload 0");
		}
		else
			return start;
	}

	/**
	 * Adds an extra caller parameter to the given entry.
	 * 
	 * @param method the entry
	 * @return the local variable used for the extra parameter
	 */
	private int addExtraParameters(MethodGen method) {
		List<Type> args = new ArrayList<>();
		int slotsForParameters = 0;
		for (Type arg : method.getArgumentTypes()) {
			args.add(arg);
			slotsForParameters += arg.getSize();
		}
		args.add(CONTRACT_OT);
		args.add(DUMMY_OT); // to avoid name clashes after the addition
		method.setArgumentTypes(args.toArray(Type.NO_ARGS));

		String[] names = method.getArgumentNames();
		if (names != null) {
			List<String> namesAsList = new ArrayList<>();
			for (String name : names)
				namesAsList.add(name);
			namesAsList.add("caller");
			namesAsList.add("unused");
			method.setArgumentNames(namesAsList.toArray(new String[namesAsList.size()]));
		}

		return slotsForParameters + 1;
	}
}