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

import static io.hotmoka.exceptions.CheckSupplier.check;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;

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
import org.apache.bcel.generic.Type;

import io.hotmoka.constants.Constants;
import io.hotmoka.instrumentation.internal.InstrumentationConstants;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl.Builder.MethodLevelInstrumentation;
import io.hotmoka.whitelisting.WhitelistingConstants;

/**
 * Passes the trailing implicit parameters to calls to methods annotated as {@code @@FromContract}.
 * They are the caller and the payer of the callee and {@code null} (as a dummy argument).
 */
public class AddExtraArgsToCallsToFromContract extends MethodLevelInstrumentation {
	private final static ObjectType CONTRACT_OT = new ObjectType(Constants.CONTRACT_NAME);
	private final static ObjectType RUNTIME_OT = new ObjectType(WhitelistingConstants.RUNTIME_NAME);
	private final static ObjectType DUMMY_OT = new ObjectType(WhitelistingConstants.DUMMY_NAME);

	/**
	 * Builds the instrumentation.
	 * 
	 * @param builder the builder of the class being instrumented
	 * @param method the method being instrumented
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
	 */
	public AddExtraArgsToCallsToFromContract(InstrumentedClassImpl.Builder builder, MethodGen method) throws ClassNotFoundException {
		builder.super(method);

		if (!method.isAbstract()) {
			InstructionList il = method.getInstructionList();
			List<InstructionHandle> callsToFromContract = check(ClassNotFoundException.class, () ->
				StreamSupport.stream(il.spliterator(), false)
					.filter(uncheck(ih -> isCallToFromContract(ih.getInstruction()))).collect(Collectors.toList())
			);

			for (InstructionHandle ih: callsToFromContract)
				passExtraArgsToCallToFromContract(il, ih, method.getName());
		}
	}

	/**
	 * Passes the trailing implicit parameters to the given call to a {@code @@FromContract}. They
	 * are the caller, the payer (if any) and {@code null} (for the dummy argument).
	 * 
	 * @param il the instructions of the method being instrumented
	 * @param ih the call to the entry
	 * @param callee the name of the method where the calls are being looked for
	 * @throws ClassNotFoundException if some class cannot be found in the Takamaka program
	 */
	private void passExtraArgsToCallToFromContract(InstructionList il, InstructionHandle ih, String callee) throws ClassNotFoundException {
		var invoke = (InvokeInstruction) ih.getInstruction();
		var args = invoke.getArgumentTypes(cpg);
		String methodName = invoke.getMethodName(cpg);
		var returnType = invoke.getReturnType(cpg);
		int slots = Stream.of(args).mapToInt(Type::getSize).sum();
		
		if (invoke instanceof INVOKEDYNAMIC) {
			var invokedynamic = (INVOKEDYNAMIC) invoke;
			var cid = (ConstantInvokeDynamic) cpg.getConstant(invokedynamic.getIndex());

			// this is an invokedynamic that calls a @FromContract: we must capture the calling contract
			var expandedArgs = new Type[args.length + 1];
			System.arraycopy(args, 0, expandedArgs, 1, args.length);
			expandedArgs[0] = new ObjectType(className);
			var expandedCid = new ConstantInvokeDynamic(cid.getBootstrapMethodAttrIndex(),
				cpg.addNameAndType(methodName, Type.getMethodSignature(returnType, expandedArgs)));
			int index = addInvokeDynamicToConstantPool(expandedCid);
			var copied = (INVOKEDYNAMIC) invokedynamic.copy();
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
			for (var arg: args) {
				il.insert(ih, InstructionFactory.createLoad(arg, usedLocals + offset));
				offset += arg.getSize();
			}
		}
		else {
			var expandedArgs = new Type[args.length + 2];
			System.arraycopy(args, 0, expandedArgs, 0, args.length);
			expandedArgs[args.length] = CONTRACT_OT;
			expandedArgs[args.length + 1] = DUMMY_OT;

			boolean onThis = pushers.getPushers(ih, slots + 1, il, cpg)
				.map(InstructionHandle::getInstruction)
				.allMatch(ins -> ins instanceof LoadInstruction && ((LoadInstruction) ins).getIndex() == 0);

			if (onThis) {
				// the call is on "this": it inherits our caller
				var ourArgs = method.getArgumentTypes();
				if (annotations.isFromContract(className, method.getName(), ourArgs, method.getReturnType())) {
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
				if (Const.CONSTRUCTOR_NAME.equals(methodName))
					il.append(ih, InstructionConst.ACONST_NULL); // we pass null as Dummy
				else
					// otherwise we pass a tag that states that it is a method called on "this"
					il.append(ih, factory.createGetStatic(WhitelistingConstants.DUMMY_NAME, "METHOD_ON_THIS", DUMMY_OT));
			}
			else {
				// the call is not on "this": it must be inside a contract "this" that becomes the caller
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
	static class LoadCaller extends ALOAD {
		private LoadCaller(int n) {
			super(n);
		}
	}

	/**
	 * Determines if the given instruction calls a method annotated as {@code @@FromContract}.
	 * 
	 * @param instruction the instruction
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if some class cannot be found in the Takamaka program
	 */
	private boolean isCallToFromContract(Instruction instruction) throws ClassNotFoundException {
		if (instruction instanceof INVOKEDYNAMIC)
			return bootstrapMethodsThatWillRequireExtraThis.contains(bootstraps.getBootstrapFor((INVOKEDYNAMIC) instruction));
		else if (instruction instanceof InvokeInstruction) {
			var invoke = (InvokeInstruction) instruction;
			var receiver = invoke.getReferenceType(cpg);
			// we do not consider calls added by instrumentation
			if (receiver instanceof ObjectType && !receiver.equals(RUNTIME_OT))
				return annotations.isFromContract(((ObjectType) receiver).getClassName(),
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