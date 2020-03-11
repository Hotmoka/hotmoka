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

import org.apache.bcel.Const;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;

import it.univr.bcel.UninitializedObjectType;

/**
 * An uninitialized object type created by a {@code new} instruction
 * at a given offset inside the code, or passed as receiver to a constructor.
 */
class UninitializedObjectTypeImpl extends ReferenceType implements UninitializedObjectType {

    /**
     * The type of the object, once it will be initialized.
     */
    private final ObjectType onceInitialized;

    /**
     * The offset of the {@code new} instruction that created the object,
     * inside the code of the same method or constructor
     * whose type are being inferred. This may be -1, meaning that this
     * is the uninitialized object passed as receiver to a constructor.
     */
    private final int offset;

    /**
     * Creates an uninitialized object created by the {@code new} instruction at the
     * given offset inside the code of the same method or constructor whose types
     * are being inferred.
     * 
     * @param onceInitialized the type of the object once it will be initialized
     * @param offset the offset of the {@code new} instruction
     */
    UninitializedObjectTypeImpl(ObjectType onceInitialized, int offset) {
        super(Const.T_UNKNOWN, onceInitialized.getClassName() + " [uninitialized]");

        this.onceInitialized = onceInitialized;
        this.offset = offset;
    }

    /**
     * Creates an uninitialized object created by the {@code new} instruction at the
     * given offset inside the code of the same method or constructor whose types
     * are being inferred.
     * 
     * @param onceInitialized the type of the object once it will be initialized
     */
    UninitializedObjectTypeImpl(ObjectType onceInitialized) {
        super(Const.T_UNKNOWN, "this [uninitialized]");

        this.onceInitialized = onceInitialized;
        this.offset = -1;
    }

    @Override
    public ObjectType onceInitialized() {
        return onceInitialized;
    }

    @Override
    public int getOffset() {
    	return offset;
    }

    @Override
    public int hashCode() {
    	return onceInitialized.hashCode() ^ offset;
    }

    @Override
    public boolean equals(Object other) {
    	return other instanceof UninitializedObjectTypeImpl &&
    		offset == ((UninitializedObjectTypeImpl) other).offset &&
    		onceInitialized.equals(((UninitializedObjectTypeImpl) other).onceInitialized);
    }
}