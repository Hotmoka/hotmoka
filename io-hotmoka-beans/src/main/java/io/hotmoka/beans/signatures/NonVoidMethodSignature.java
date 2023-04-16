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

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;

/**
 * The signature of a method of a class, that returns a value.
 */
@Immutable
public final class NonVoidMethodSignature extends MethodSignature {
	final static byte SELECTOR = 1;

	/**
	 * The type of the returned type;
	 */
	public final StorageType returnType;

	/**
	 * Builds the signature of a method, that returns a value.
	 * 
	 * @param definingClass the class of the method
	 * @param methodName the name of the method
	 * @param returnType the type of the returned value
	 * @param formals the formal arguments of the method
	 */
	public NonVoidMethodSignature(ClassType definingClass, String methodName, StorageType returnType, StorageType... formals) {
		super(definingClass, methodName, formals);

		if (returnType == null)
			throw new IllegalArgumentException("returnType cannot be null");

		this.returnType = returnType;
	}

	/**
	 * Builds the signature of a method, that returns no value.
	 * 
	 * @param definingClass the name of the class of the method
	 * @param methodName the name of the method
	 * @param returnType the type of the returned value
	 * @param formals the formal arguments of the method
	 */
	public NonVoidMethodSignature(String definingClass, String methodName, StorageType returnType, StorageType... formals) {
		this(new ClassType(definingClass), methodName, returnType, formals);
	}

	@Override
	public String toString() {
		return returnType + " " + definingClass + "." + methodName + commaSeparatedFormals();
	}

    @Override
	public boolean equals(Object other) {
		return other instanceof NonVoidMethodSignature && returnType.equals(((NonVoidMethodSignature) other).returnType) && super.equals(other);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(returnType.size(gasCostModel));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		super.into(context);
		returnType.into(context);
	}
}