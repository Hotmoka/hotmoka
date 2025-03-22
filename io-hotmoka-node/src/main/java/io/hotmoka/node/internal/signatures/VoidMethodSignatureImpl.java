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

package io.hotmoka.node.internal.signatures;

import java.io.IOException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.internal.gson.MethodSignatureJson;
import io.hotmoka.node.internal.types.ClassTypeImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The signature of a method of a class, that does not return any value.
 */
@Immutable
public final class VoidMethodSignatureImpl extends AbstractMethodSignature implements VoidMethodSignature {

	/**
	 * Builds the signature of a method, that returns no value.
	 * 
	 * @param definingClass the class of the method
	 * @param name the name of the method
	 * @param formals the formal arguments of the method
	 */
	public VoidMethodSignatureImpl(ClassType definingClass, String name, StorageType[] formals) {
		super(definingClass, name, formals, IllegalArgumentException::new);
	}

	/**
	 * Builds the signature of a method, that returns no value.
	 * 
	 * @param <E> the type of the exception thrown if some arguments is illegal
	 * @param definingClass the class of the method
	 * @param name the name of the method
	 * @param formals the formal arguments of the method
	 * @param onIllegalArgs the generator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	<E extends Exception> VoidMethodSignatureImpl(ClassType definingClass, String name, StorageType[] formals, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		super(definingClass, name, formals, onIllegalArgs);
	}

	/**
	 * Creates a method signature from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public VoidMethodSignatureImpl(MethodSignatureJson json) throws InconsistentJsonException {
		this(
			ClassTypeImpl.named(json.getDefiningClass(), InconsistentJsonException::new),
			json.getName(),
			formalsAsTypes(json),
			InconsistentJsonException::new
		);
	}

	@Override
	public String toString() {
		return "void " + super.toString();
	}

    @Override
	public boolean equals(Object other) {
		return other instanceof VoidMethodSignature && super.equals(other);
	}

    @Override
    public void into(MarshallingContext context) throws IOException {
    	getDefiningClass().into(context);
    	context.writeStringUnshared(getName());

    	var formals = getFormals().toArray(StorageType[]::new);
    	context.writeCompactInt(formals.length * 2); // this signals that the method is void (see from() inside AbstractMethodSignature)
    	for (var formal: formals)
    		formal.into(context);
    }
}