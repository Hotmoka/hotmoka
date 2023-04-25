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

package io.hotmoka.beans.signatures;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * The signature of a method of a class, that does not return any value.
 */
@Immutable
public final class VoidMethodSignature extends MethodSignature {
	final static byte SELECTOR = 2;
	final static byte SELECTOR_REWARD = 4;

	/**
	 * Builds the signature of a method, that returns no value.
	 * 
	 * @param definingClass the class of the method
	 * @param methodName the name of the method
	 * @param formals the formal arguments of the method
	 */
	public VoidMethodSignature(ClassType definingClass, String methodName, StorageType... formals) {
		super(definingClass, methodName, formals);
	}

	/**
	 * Builds the signature of a method, that returns no value.
	 * 
	 * @param definingClass the name of the class of the method
	 * @param methodName the name of the method
	 * @param formals the formal arguments of the method
	 */
	public VoidMethodSignature(String definingClass, String methodName, StorageType... formals) {
		this(new ClassType(definingClass), methodName, formals);
	}

	@Override
	public String toString() {
		return "void " + definingClass + "." + methodName + commaSeparatedFormals();
	}

    @Override
	public boolean equals(Object other) {
		return other instanceof VoidMethodSignature && super.equals(other);
	}

	@Override
	public void into(MarshallingContext context) {
		if (equals(VALIDATORS_REWARD))
			context.writeByte(SELECTOR_REWARD);
		else {
			context.writeByte(SELECTOR);
			super.into(context);
		}
	}
}