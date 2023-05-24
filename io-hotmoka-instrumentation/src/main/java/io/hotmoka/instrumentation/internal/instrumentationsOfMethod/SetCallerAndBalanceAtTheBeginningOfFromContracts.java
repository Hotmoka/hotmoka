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

package io.hotmoka.instrumentation.internal.instrumentationsOfMethod;

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
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.RET;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.Type;

import io.hotmoka.constants.Constants;
import io.hotmoka.instrumentation.InstrumentationConstants;
import io.hotmoka.instrumentation.internal.HeightAtBytecode;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl.Builder.MethodLevelInstrumentation;
import io.hotmoka.instrumentation.internal.instrumentationsOfMethod.AddExtraArgsToCallsToFromContract.LoadCaller;
import io.hotmoka.verification.Dummy;

/**
 * Sets the caller at the beginning of {@code @@FromContract} and updates the balance
 * at the beginning of payable {@code @@FromContract}.
 */
public class SetCallerAndBalanceAtTheBeginningOfFromContracts extends MethodLevelInstrumentation {
	private final static ObjectType CONTRACT_OT = new ObjectType(io.hotmoka.constants.Constants.CONTRACT_NAME);
	private final static ObjectType OBJECT_OT = new ObjectType(Object.class.getName());
	private final static ObjectType DUMMY_OT = new ObjectType(Dummy.class.getName());
	private final static Type[] FROM_CONTRACT_ARGS = { OBJECT_OT, OBJECT_OT };

	public SetCallerAndBalanceAtTheBeginningOfFromContracts(InstrumentedClassImpl.Builder builder, MethodGen method) throws ClassNotFoundException {
		builder.super(method);

		if (isStorage || classLoader.isInterface(className)) {
			String name = method.getName();
			Type[] args = method.getArgumentTypes();
			Type returnType = method.getReturnType();
			Optional<Class<?>> ann = annotations.getFromContractArgument(className, name, args, returnType);
			if (ann.isPresent()) {
				boolean isPayable = annotations.isPayable(className, name, args, returnType);
				boolean isRedPayable = annotations.isRedPayable(className, name, args, returnType);
				instrumentFromContract(method, ann.get(), isPayable, isRedPayable);
			};
		}
	}

	/**
	 * Instruments an entry, by setting the caller and transferring funds for payable entries.
	 * 
	 * @param method the entry
	 * @param callerContract the class of the caller contract
	 * @param isPayable true if and only if the entry is payable
	 * @param isRedPayable true if and only if the entry is red payable
	 * @throws ClassNotFoundException if some class of the Takamaka program could not be found
	 */
	private void instrumentFromContract(MethodGen method, Class<?> callerContract, boolean isPayable, boolean isRedPayable) throws ClassNotFoundException {
		// slotForCaller is the local variable used for the extra "caller" parameter;
		int slotForCaller = addExtraParameters(method);
		if (!method.isAbstract()) {
			// we shift one slot upwards all local variables from slotForCaller, inclusive
			shiftUp(method, slotForCaller);
			setCallerAndBalance(method, callerContract, slotForCaller, isPayable, isRedPayable);
		}
	}

	private void shiftUp(MethodGen method, int slotForCaller) {
		InstructionList il = method.getInstructionList();
		for (InstructionHandle ih: il) {
			Instruction ins = ih.getInstruction();
			if (ins instanceof LocalVariableInstruction && !(ins instanceof LoadCaller)) {
				int local = ((LocalVariableInstruction) ins).getIndex();
				if (local >= slotForCaller) {
					if (ins instanceof IINC)
						ih.setInstruction(new IINC(local + 1, ((IINC) ins).getIncrement()));
					else if (ins instanceof LoadInstruction)
						ih.setInstruction(InstructionFactory.createLoad(((LoadInstruction) ins).getType(cpg), local + 1));
					else if (ins instanceof StoreInstruction)
						ih.setInstruction(InstructionFactory.createStore(((StoreInstruction) ins).getType(cpg), local + 1));
				}
			}
		}
	}

	/**
	 * Instruments an entry by calling the runtime method that sets caller and balance.
	 * 
	 * @param method the entry
	 * @param callerContract the class of the caller contract
	 * @param slotForCaller the local variable for the caller implicit argument
	 * @param isPayable true if and only if the entry is payable
	 * @param isRedPayable true if and only if the entry is red payable
	 * @throws ClassNotFoundException 
	 */
	private void setCallerAndBalance(MethodGen method, Class<?> callerContract, int slotForCaller, boolean isPayable, boolean isRedPayable) throws ClassNotFoundException {
		InstructionList il = method.getInstructionList();
		InstructionHandle start = il.getStart();

		// the call to the method that sets caller and balance cannot be put at the
		// beginning of the method, always: for constructors, Java bytecode requires
		// that their code starts with a call to a constructor of the superclass
		InstructionHandle where;
		boolean isConstructorOfInstanceInnerClass;
		boolean superconstructorIsFromContract;
		boolean superconstructorIsPayable;
		boolean superconstructorIsRedPayable;

		if (method.getName().equals(Const.CONSTRUCTOR_NAME)) {
			isConstructorOfInstanceInnerClass = isConstructorOfInstanceInnerClass();
			InstructionHandle callToSuperConstructor = callToSuperConstructor(il, method, slotForCaller, isConstructorOfInstanceInnerClass);
			INVOKESPECIAL invokespecial = (INVOKESPECIAL) callToSuperConstructor.getInstruction();
			// if the superconstructor is @FromContract, then it will take care of setting the caller for us
			String classNameOfSuperConstructor = invokespecial.getClassName(cpg);
			Type[] argumentTypes = invokespecial.getArgumentTypes(cpg);
			if (argumentTypes.length > 0 && argumentTypes[argumentTypes.length - 1].equals(DUMMY_OT)) {
				// the target has been already instrumented, we removed the extra arguments
				Type[] copy = new Type[argumentTypes.length - 2];
				System.arraycopy(argumentTypes, 0, copy, 0, copy.length);
				argumentTypes = copy;
			}
			superconstructorIsFromContract = annotations.isFromContract(classNameOfSuperConstructor, Const.CONSTRUCTOR_NAME, argumentTypes, Type.VOID);
			superconstructorIsPayable = annotations.isPayable(classNameOfSuperConstructor, Const.CONSTRUCTOR_NAME, argumentTypes, Type.VOID);
			superconstructorIsRedPayable = annotations.isRedPayable(classNameOfSuperConstructor, Const.CONSTRUCTOR_NAME, argumentTypes, Type.VOID);
			where = callToSuperConstructor.getNext();
		}
		else {
			isConstructorOfInstanceInnerClass = false;
			superconstructorIsFromContract = false;
			superconstructorIsPayable = false;
			superconstructorIsRedPayable = false;
			where = start;
		}

		if (isPayable || isRedPayable) {
			if (superconstructorIsPayable || superconstructorIsRedPayable) {
				if (callerContract != classLoader.getContract()) {
					il.insert(start, InstructionFactory.createLoad(CONTRACT_OT, slotForCaller));
					il.insert(start, factory.createCast(CONTRACT_OT, Type.getType(callerContract)));
					il.insert(InstructionConst.POP);
				}
			}
			else {
				il.insert(start, InstructionFactory.createThis());
				il.insert(start, InstructionFactory.createLoad(CONTRACT_OT, slotForCaller));

				if (callerContract != classLoader.getContract())
					il.insert(start, factory.createCast(CONTRACT_OT, Type.getType(callerContract)));

				il.insert(start, InstructionFactory.createLoad(DUMMY_OT, slotForCaller + 1));

				// a payable from contract method can have a first argument of type int/long/BigInteger
				Type amountType = method.getArgumentType(isConstructorOfInstanceInnerClass ? 1 : 0);
				il.insert(start, InstructionFactory.createLoad(amountType, isConstructorOfInstanceInnerClass ? 2 : 1));
				Type[] payableFromContractArgs = new Type[] { OBJECT_OT, OBJECT_OT, DUMMY_OT, amountType };
				il.insert(where, factory.createInvoke(Constants.RUNTIME_NAME,
					isPayable ? InstrumentationConstants.PAYABLE_FROM_CONTRACT : InstrumentationConstants.RED_PAYABLE_FROM_CONTRACT,
					Type.VOID, payableFromContractArgs, Const.INVOKESTATIC));
			}
		}
		else if (!superconstructorIsFromContract) {
			il.insert(start, InstructionFactory.createThis());
			il.insert(start, InstructionFactory.createLoad(CONTRACT_OT, slotForCaller));

			if (callerContract != classLoader.getContract())
				il.insert(start, factory.createCast(CONTRACT_OT, Type.getType(callerContract)));

			il.insert(where, factory.createInvoke(Constants.RUNTIME_NAME, InstrumentationConstants.FROM_CONTRACT, Type.VOID, FROM_CONTRACT_ARGS, Const.INVOKESTATIC));
		}
		else if (callerContract != classLoader.getContract()) {
			il.insert(start, InstructionFactory.createLoad(CONTRACT_OT, slotForCaller));
			il.insert(start, factory.createCast(CONTRACT_OT, Type.getType(callerContract)));
			il.insert(InstructionConst.POP);
		}
	}

	private boolean isConstructorOfInstanceInnerClass() {
		int dollarPos = className.lastIndexOf('$');
		Type[] methodArgs;
		ObjectType t;

		// constructors of inner classes c have a first implicit parameter whose type t is the parent class
		// and they start with aload_0 aload_1 putfield c.f:t
		if (dollarPos > 0
			&& (methodArgs = method.getArgumentTypes()).length > 0 && methodArgs[0] instanceof ObjectType
			&& (t = (ObjectType) methodArgs[0]).getClassName().equals(className.substring(0, dollarPos))) {

			InstructionList il = method.getInstructionList();
			if (il != null && il.getLength() >= 3) {
				Instruction[] instructions = il.getInstructions();
				ReferenceType c;
				PUTFIELD putfield;

				return instructions[0] instanceof LoadInstruction && ((LoadInstruction) instructions[0]).getIndex() == 0
					&& instructions[1] instanceof LoadInstruction && ((LoadInstruction) instructions[1]).getIndex() == 1
					&& instructions[2] instanceof PUTFIELD && (putfield = (PUTFIELD) instructions[2]).getFieldType(cpg).equals(t)
					&& (c = putfield.getReferenceType(cpg)) instanceof ObjectType && ((ObjectType) c).getClassName().equals(className);
			}
		}

		return false;
	}

	/**
	 * From contract constructors {@link io.takamaka.code.lang.Storage#fromContract(io.takamaka.code.lang.Contract)} or
	 * {@link io.takamaka.code.lang.Contract#payableFromContract(io.takamaka.code.lang.Contract, BigInteger)} at their
	 * beginning, to set the caller and the balance of the called entry. In general,
	 * such call can be placed at the very beginning of the code. The only problem
	 * is related to constructors, that require (by JVM constraints)
	 * their code to start with a call to a
	 * constructor of their superclass. In that case, this method finds that call:
	 * after that, we can add the call that sets caller and balance.
	 * 
	 * @param il the list of instructions of the entry
	 * @param constructor the from contract constructor
	 * @param slotForCaller the local where the caller contract is passed to the entry
	 * @param isConstructorOfInstanceInnerClass true if and only if the {@code constructor} belongs to an instance inner class
	 * @return the instruction before which the code that sets caller and balance can be placed
	 */
	private InstructionHandle callToSuperConstructor(InstructionList il, MethodGen constructor, int slotForCaller, boolean isConstructorOfInstanceInnerClass) {
		InstructionHandle start = il.getStart();

		// we skip the initial aload_0 aload_1 putfield this$0
		if (isConstructorOfInstanceInnerClass)
			start = il.getInstructionHandles()[3];

		// we have to identify the call to the constructor of the superclass:
		// the code of a constructor normally starts with an aload_0 whose value is consumed
		// by a call to a constructor of the superclass. In the middle, slotForCaller is not expected
		// to be modified. Note that this is the normal situation, as results from a normal
		// Java compiler. In principle, the Java bytecode might instead do very weird things,
		// including calling two constructors of the superclass at different places. In all such cases
		// this method fails and rejects the code: such non-standard code is not supported by Takamaka
		Instruction startInstruction = start.getInstruction();
		if (startInstruction instanceof LoadInstruction && ((LoadInstruction) startInstruction).getIndex() == 0) {
			Set<InstructionHandle> callsForConstructorChaining = new HashSet<>();
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

				stackHeightAfterBytecode += bytecode.produceStack(cpg);
				stackHeightAfterBytecode -= bytecode.consumeStack(cpg);

				if (stackHeightAfterBytecode == 0) {
					// found a consumer of the aload_0: is it really a call to a constructor of the superclass or of the same class?
					if (bytecode instanceof INVOKESPECIAL
							&& (((INVOKESPECIAL) bytecode).getClassName(cpg).equals(getSuperclassName()) ||
									((INVOKESPECIAL) bytecode).getClassName(cpg).equals(className))
							&& ((INVOKESPECIAL) bytecode).getMethodName(cpg).equals(Const.CONSTRUCTOR_NAME))
						callsForConstructorChaining.add(current.ih);
					else
						throw new IllegalStateException("Unexpected consumer of local 0 " + bytecode + " before initialization of " + className);
				}
				else if (bytecode instanceof GotoInstruction) {
					HeightAtBytecode added = new HeightAtBytecode(((GotoInstruction) bytecode).getTarget(), stackHeightAfterBytecode);
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
				else if (bytecode instanceof BranchInstruction || bytecode instanceof ATHROW || bytecode instanceof RETURN || bytecode instanceof RET)
					throw new IllegalStateException("Unexpected instruction " + bytecode + " before initialization of " + className);
				else {
					HeightAtBytecode added = new HeightAtBytecode(current.ih.getNext(), stackHeightAfterBytecode);
					if (seen.add(added))
						workingSet.add(added);
				}
			}
			while (!workingSet.isEmpty());

			if (callsForConstructorChaining.size() == 1)
				return callsForConstructorChaining.iterator().next();
			else
				throw new IllegalStateException("Cannot identify single call to constructor chaining inside a constructor ot " + className);
		}
		else
			throw new IllegalStateException("Constructor of " + className + " does not start with aload 0");
	}

	/**
	 * Adds two extra caller parameters to the given method annotated as {@code @@FromContract}.
	 * 
	 * @param method the method
	 * @return the local variable used for the first extra parameter
	 */
	private int addExtraParameters(MethodGen method) {
		List<Type> args = new ArrayList<>();
		int slotsForParameters = 0;
		for (Type arg: method.getArgumentTypes()) {
			args.add(arg);
			slotsForParameters += arg.getSize();
		}
		args.add(CONTRACT_OT);

		args.add(DUMMY_OT); // to avoid name clashes after the addition
		method.setArgumentTypes(args.toArray(Type.NO_ARGS));

		String[] names = method.getArgumentNames();
		if (names != null) {
			List<String> namesAsList = new ArrayList<>();
			for (String name: names)
				namesAsList.add(name);
			namesAsList.add("caller");
			namesAsList.add("unused");
			method.setArgumentNames(namesAsList.toArray(String[]::new));
		}

		return slotsForParameters + 1;
	}
}