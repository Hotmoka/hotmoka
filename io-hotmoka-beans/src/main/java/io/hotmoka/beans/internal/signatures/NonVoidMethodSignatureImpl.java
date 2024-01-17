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

package io.hotmoka.beans.internal.signatures;

import java.io.IOException;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * The signature of a method of a class, that returns a value.
 */
@Immutable
public final class NonVoidMethodSignatureImpl extends AbstractMethodSignature implements NonVoidMethodSignature {

	/**
	 * The type of the returned value.
	 */
	private final StorageType returnType;

	/**
	 * Builds the signature of a method, that returns a value.
	 * 
	 * @param definingClass the class of the method
	 * @param methodName the name of the method
	 * @param returnType the type of the returned value
	 * @param formals the formal arguments of the method
	 */
	public NonVoidMethodSignatureImpl(ClassType definingClass, String methodName, StorageType returnType, StorageType... formals) {
		super(definingClass, methodName, formals);

		this.returnType = Objects.requireNonNull(returnType, "returnType cannot be null");
	}

	@Override
	public StorageType getReturnType() {
		return returnType;
	}

	@Override
	public String toString() {
		return returnType + " " + getDefiningClass() + "." + getMethodName() + commaSeparatedFormals();
	}

    @Override
	public boolean equals(Object other) {
		return other instanceof NonVoidMethodSignature nvms && returnType.equals(nvms.getReturnType()) && super.equals(other);
	}

    @Override
    public void into(MarshallingContext context) throws IOException {
    	getDefiningClass().into(context);
    	context.writeStringUnshared(getMethodName());

    	var formals = getFormals().toArray(StorageType[]::new);
    	context.writeCompactInt(formals.length * 2 + 1); // this signals that the method is non-void (see from() inside AbstractMethodSignature)
    	for (var formal: formals)
    		formal.into(context);

    	returnType.into(context);
    }
}