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
import java.util.List;
import java.util.Optional;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.StackMap;
import org.apache.bcel.classfile.StackMapEntry;
import org.apache.bcel.classfile.StackMapType;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import it.univr.bcel.StackMapInferrer;
import it.univr.bcel.TypeInferenceException;
import it.univr.bcel.UninitializedObjectType;

/**
 * An inference algorithm that recomputes the stack map for a method or constructor.
 * 
 * @author Fausto Spoto <fausto.spoto@univr.it>
 */
public class StackMapInferrerImpl implements StackMapInferrer {

	/**
	 * The inferred stack map.
	 */
	private final Optional<StackMap> stackMap;

	/**
	 * Infers the stack map entries for the given method or constructor.
	 * 
	 * @param methodOrConstructor the method or constructor
	 */
	public StackMapInferrerImpl(MethodGen methodOrConstructor) {
		stackMap = new Builder(methodOrConstructor).newStackMap;
	}

	@Override
	public Optional<StackMap> getStackMap() {
		return stackMap;
	}

	private static class Builder {
		private final TypeInferrerImpl inferrer;
		private final List<StackMapEntry> newStackMapEntries = new ArrayList<>();
		private final Optional<StackMap> newStackMap;
		private int sizeOfNewStackMap = 2; // increased whenever a new entry is added
		private InstructionHandle previous;
		private TypesImpl typesAtPrevious;
		private ConstantPoolGen cpg;

		private Builder(MethodGen methodOrConstructor) {
			this.cpg = methodOrConstructor.getConstantPool();
			this.inferrer = new TypeInferrerImpl(methodOrConstructor);
			this.previous = methodOrConstructor.isAbstract() ? null : methodOrConstructor.getInstructionList().getStart();
			this.typesAtPrevious = new TypesImpl(methodOrConstructor);
		
			newStackMap = inferStackMap();
		}

		private Optional<StackMap> inferStackMap() {
			inferrer.getInstructionHandlesRequiringStackmapEntry().forEach(this::process);

			if (newStackMapEntries.isEmpty())
				return Optional.empty();
			else
				return Optional.of(new StackMap(cpg.addUtf8("StackMapTable"), sizeOfNewStackMap, newStackMapEntries.toArray(new StackMapEntry[newStackMapEntries.size()]), cpg.getConstantPool()));
		}

		private void process(InstructionHandle ih) {
			InstructionHandle previous = this.previous;
			TypesImpl typesAtPrevious = this.typesAtPrevious;
			this.previous = ih;
			this.typesAtPrevious = inferrer.getTypesAt(ih);

			addStackMapEntry(previous, typesAtPrevious, ih, this.typesAtPrevious);
		}

		private void addStackMapEntry(InstructionHandle previous, TypesImpl typesAtPrevious, InstructionHandle next, TypesImpl typesAtNext) {
			int offset;
			if (newStackMapEntries.isEmpty())
				offset = next.getPosition();
			else
				offset = next.getPosition() - previous.getPosition() - 1;

			if (typesAtNext.same(typesAtPrevious)) {
				if (offset <= Const.SAME_FRAME_MAX) {
					sizeOfNewStackMap++;
					newStackMapEntries.add(new StackMapEntry(Const.SAME_FRAME + offset, offset, null, null, cpg.getConstantPool()));
				}
				else {
					sizeOfNewStackMap += 3;
					newStackMapEntries.add(new StackMapEntry(Const.SAME_FRAME_EXTENDED, offset, null, null, cpg.getConstantPool()));
				}
			}
			else if (typesAtNext.sameLocalsOneStackItem(typesAtPrevious)) {
				if (Const.SAME_LOCALS_1_STACK_ITEM_FRAME + offset <= Const.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
					sizeOfNewStackMap++;
					newStackMapEntries.add(new StackMapEntry
						(Const.SAME_LOCALS_1_STACK_ITEM_FRAME + offset, offset, null,
						new StackMapType[] { toStackMapType(typesAtNext.getStack(0))},
						cpg.getConstantPool()));
				}
				else {
					sizeOfNewStackMap += 3;
					newStackMapEntries.add(new StackMapEntry
						(Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED, offset, null,
						new StackMapType[] { toStackMapType(typesAtNext.getStack(0))},
						cpg.getConstantPool()));
				}
			}
			else if (typesAtNext.append1(typesAtPrevious)) {
				int previousLocals = typesAtPrevious.localsCount();
				sizeOfNewStackMap += 3;
				newStackMapEntries.add(new StackMapEntry
					(Const.APPEND_FRAME, offset,
					new StackMapType[] { toStackMapType(typesAtNext.getLocal(previousLocals))},
					null,
					cpg.getConstantPool()));
			}
			else if (typesAtNext.append2(typesAtPrevious)) {
				int previousLocals = typesAtPrevious.localsCount();
				Type additional1 = typesAtNext.getLocal(previousLocals);
				Type additional2 = typesAtNext.getLocal(previousLocals + additional1.getSize());
				sizeOfNewStackMap += 3;
				newStackMapEntries.add(new StackMapEntry
					(Const.APPEND_FRAME + 1, offset,
					new StackMapType[] { toStackMapType(additional1), toStackMapType(additional2) },
					null,
					cpg.getConstantPool()));
			}
			else if (typesAtNext.append3(typesAtPrevious)) {
				int previousLocals = typesAtPrevious.localsCount();
				Type additional1 = typesAtNext.getLocal(previousLocals);
				Type additional2 = typesAtNext.getLocal(previousLocals + additional1.getSize());
				Type additional3 = typesAtNext.getLocal(previousLocals + additional1.getSize() + additional2.getSize());
				sizeOfNewStackMap += 3;
				newStackMapEntries.add(new StackMapEntry
					(Const.APPEND_FRAME + 2, offset,
					new StackMapType[] { toStackMapType(additional1), toStackMapType(additional2), toStackMapType(additional3) },
					null,
					cpg.getConstantPool()));
			}
			else if (typesAtNext.chop1(typesAtPrevious)) {
				sizeOfNewStackMap += 3;
				newStackMapEntries.add(new StackMapEntry(Const.CHOP_FRAME + 2, offset, null, null, cpg.getConstantPool()));
			}
			else if (typesAtNext.chop2(typesAtPrevious)) {
				sizeOfNewStackMap += 3;
				newStackMapEntries.add(new StackMapEntry(Const.CHOP_FRAME + 1, offset, null, null, cpg.getConstantPool()));
			}
			else if (typesAtNext.chop3(typesAtPrevious)) {
				sizeOfNewStackMap += 3;
				newStackMapEntries.add(new StackMapEntry(Const.CHOP_FRAME, offset, null, null, cpg.getConstantPool()));
			}
			else {
				sizeOfNewStackMap += 7;
				StackMapType[] locals = inferrer.getTypesAt(next).getLocalsOnlyFirstSlots().map(this::toStackMapType).toArray(StackMapType[]::new);
				StackMapType[] stack = inferrer.getTypesAt(next).getStackOnlyFirstSlots().map(this::toStackMapType).toArray(StackMapType[]::new);
				newStackMapEntries.add(new StackMapEntry(Const.FULL_FRAME, offset, locals, stack, cpg.getConstantPool()));
			}
		}

		private StackMapType toStackMapType(Type type) {
			if (type == Type.NULL) {
				sizeOfNewStackMap++;
				return new StackMapType(Const.ITEM_Null, 0, cpg.getConstantPool());
			}
			else if (type instanceof UninitializedObjectType) {
				int offset = ((UninitializedObjectType) type).getOffset();
				if (offset < 0) {
					sizeOfNewStackMap++;
					return new StackMapType(Const.ITEM_InitObject, 0, cpg.getConstantPool());
				}
				else {
					sizeOfNewStackMap += 3;
					return new StackMapType(Const.ITEM_NewObject, offset, cpg.getConstantPool());
				}
			}

			switch (type.getType()) {
			case Const.T_INT:
				sizeOfNewStackMap++;
				return new StackMapType(Const.ITEM_Integer, 0, cpg.getConstantPool());
			case Const.T_FLOAT:
				sizeOfNewStackMap++;
				return new StackMapType(Const.ITEM_Float, 0, cpg.getConstantPool());
			case Const.T_DOUBLE:
				sizeOfNewStackMap++;
				return new StackMapType(Const.ITEM_Double, 0, cpg.getConstantPool());
			case Const.T_LONG:
				sizeOfNewStackMap++;
				return new StackMapType(Const.ITEM_Long, 0, cpg.getConstantPool());
			case Const.T_UNKNOWN:
				sizeOfNewStackMap++;
				return new StackMapType(Const.ITEM_Bogus, 0, cpg.getConstantPool());
			case Const.T_ARRAY:
				sizeOfNewStackMap += 3;
				return new StackMapType(Const.ITEM_Object, cpg.addArrayClass((ArrayType) type), cpg.getConstantPool());
			case Const.T_REFERENCE:
				sizeOfNewStackMap += 3;
				return new StackMapType(Const.ITEM_Object, cpg.addClass((ObjectType) type), cpg.getConstantPool());
			default:
				throw new TypeInferenceException("Unexpected type " + type + " in type frame");
			}
		}
	}
}