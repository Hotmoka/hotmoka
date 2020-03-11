/*
Copyright 2019 Fausto Spoto (fausto.spoto@univr.it)

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

package it.univr.bcel.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ExceptionThrower;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.Select;

import it.univr.bcel.TypeInferrer;
import it.univr.bcel.Types;

/**
 * An inference algorithm for the static types at the instructions of a method or constructor.
 * 
 * @author Fausto Spoto <fausto.spoto@univr.it>
 */
public class TypeInferrerImpl implements TypeInferrer {

	/**
	 * A map from instruction handles to the types inferred there.
	 */
	private final SortedMap<InstructionHandle, TypesImpl> types = new TreeMap<>(Comparator.comparing(InstructionHandle::getPosition));

	/**
	 * The instruction handles where a stack map must be computed.
	 */
	private final SortedSet<InstructionHandle> instructionsRequiringStackmapEntry = new TreeSet<>(Comparator.comparing(InstructionHandle::getPosition));

	/**
	 * Performs type inference for the given method or constructor.
	 * The types at each of its instruction handles can be later accessed
	 * through {@link it.univr.bcel.TypeInferrerImpl#getTypesAt(InstructionHandle)}.
	 * 
	 * @param methodOrConstructor the method or constructor
	 */
	public TypeInferrerImpl(MethodGen methodOrConstructor) {
		if (!methodOrConstructor.isAbstract()) {
			InstructionList il = methodOrConstructor.getInstructionList();
			ConstantPoolGen cpg = methodOrConstructor.getConstantPool();
			CodeExceptionGen[] handlers = methodOrConstructor.getExceptionHandlers();

			List<InstructionHandle> ws = new ArrayList<>();
			updateAt(il.getStart(), new TypesImpl(methodOrConstructor), ws);

			do {
				InstructionHandle current = ws.remove(ws.size() - 1);
				Instruction currentIns = current.getInstruction();
				if (currentIns instanceof ReturnInstruction || currentIns instanceof ATHROW)
					continue;

				TypesImpl currentTypes = types.get(current);
				TypesImpl nextTypes = currentTypes.after(current, cpg);
				propagateAtNormalFollowers(current, nextTypes, ws);
				if (currentIns instanceof ExceptionThrower)
					propagateAtExceptionalFollowers(current, handlers, currentTypes, ws);
			}
			while (!ws.isEmpty());
		}
	}

	@Override
	public TypesImpl getTypesAt(InstructionHandle where) {
		return types.get(where);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Entry<InstructionHandle, TypesImpl> entry: types.entrySet())
			sb.append(String.format("%c %30s: %s\n",
				// an asterisk means that the position requires a stack map (explicit or implicit)
				instructionsRequiringStackmapEntry.contains(entry.getKey()) ? '*' : ' ',
				entry.getKey(), entry.getValue()));
	
		return sb.toString();
	}

	/**
	 * Yields the ordered stream of the instruction handles of the constructor or method whose
	 * types have been inferred, that need a stack map entry, since they are the target of
	 * a jump (or exception handler). The stream is ordered in increasing
	 * offset inside the code of the constructor or method. The first instruction of the constructor
	 * or method is normally not in this stream, unless it is a target of a jump or exception handler.
	 * 
	 * @return the ordered stream
	 */
	Stream<InstructionHandle> getInstructionHandlesRequiringStackmapEntry() {
		return instructionsRequiringStackmapEntry.stream();
	}

	private void propagateAtExceptionalFollowers(InstructionHandle ih, CodeExceptionGen[] handlers, TypesImpl initialTypes, List<InstructionHandle> ws) {
		int pos = ih.getPosition();
		Stream.of(handlers)
			.filter(handler -> handler.getStartPC().getPosition() <= pos && pos <= handler.getEndPC().getPosition())
			.forEach(handler -> updateAtStackMapPosition(handler.getHandlerPC(),
					initialTypes.afterException(handler.getCatchType() == null ? ObjectType.THROWABLE : handler.getCatchType()), ws));
	}

	private void propagateAtNormalFollowers(InstructionHandle ih, TypesImpl nextTypes, List<InstructionHandle> ws) {
		Instruction ins = ih.getInstruction();

		switch (ins.getOpcode()) {
		case Const.GOTO:
		case Const.GOTO_W:
			updateAtStackMapPosition(((GOTO) ins).getTarget(), nextTypes, ws);
			return;
		case Const.IF_ACMPEQ:
		case Const.IF_ACMPNE:
		case Const.IF_ICMPEQ:
		case Const.IF_ICMPGE:
		case Const.IF_ICMPGT:
		case Const.IF_ICMPLE:
		case Const.IF_ICMPLT:
		case Const.IF_ICMPNE:
		case Const.IFEQ:
		case Const.IFGE:
		case Const.IFGT:
		case Const.IFLE:
		case Const.IFLT:
		case Const.IFNE:
		case Const.IFNONNULL:
		case Const.IFNULL:
			updateAtStackMapPosition(((IfInstruction) ins).getTarget(), nextTypes, ws);
			updateAt(ih.getNext(), nextTypes, ws);
			return;
		case Const.LOOKUPSWITCH:
		case Const.TABLESWITCH:
			updateAtStackMapPosition(((Select) ins).getTarget(), nextTypes, ws);
			for (InstructionHandle target: ((Select) ins).getTargets())
				updateAtStackMapPosition(target, nextTypes, ws);
			return;
		default:
			updateAt(ih.getNext(), nextTypes, ws);
			return;
		}
	}

	private void updateAt(InstructionHandle target, TypesImpl newTypes, List<InstructionHandle> ws) {
		TypesImpl oldTypes = types.get(target);
		if (oldTypes == null) {
			types.put(target, newTypes);
			if (!ws.contains(target))
				ws.add(target);
		}
		else {
			TypesImpl merged = oldTypes.merge(newTypes);
			if (!merged.equals(oldTypes)) {
				types.put(target, merged);
				if (!ws.contains(target))
					ws.add(target);
			}
		}
	}

	/**
	 * Like {@link it.univr.bcel.TypeInferrerImpl#updateAt(InstructionHandle, Types, List)},
	 * but takes note that the target requires a stack map.
	 * 
	 * @param target the target
	 * @param newTypes the new types at the target
	 * @param ws the working set, that might get expanded
	 */
	private void updateAtStackMapPosition(InstructionHandle target, TypesImpl newTypes, List<InstructionHandle> ws) {
		updateAt(target, newTypes, ws);
		instructionsRequiringStackmapEntry.add(target);
	}
}