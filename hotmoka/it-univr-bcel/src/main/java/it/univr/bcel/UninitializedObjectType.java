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

package it.univr.bcel;

import org.apache.bcel.generic.ObjectType;

/**
 * An uninitialized object type created by a {@code new} instruction
 * at a given offset inside the code, or passed as receiver to a constructor.
 */
public interface UninitializedObjectType {

    /**
     * Returns the type of the variable, as it will be after initialization.
     */
	ObjectType onceInitialized();

    /**
     * Yields the offset of the {@code new} instruction that created the object,
     * inside the code of the same method or constructor
     * whose type are being inferred. This may be -1, meaning that this
     * is the uninitialized object passed as receiver to a constructor.
     * 
     * @return the offset
     */
	int getOffset();
}