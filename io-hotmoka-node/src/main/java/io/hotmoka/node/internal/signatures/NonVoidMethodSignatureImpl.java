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
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.internal.json.MethodSignatureJson;
import io.hotmoka.node.internal.types.AbstractStorageType;
import io.hotmoka.node.internal.types.ClassTypeImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

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
	 * @param name the name of the method
	 * @param returnType the type of the returned value
	 * @param formals the formal arguments of the method
	 */
	public NonVoidMethodSignatureImpl(ClassType definingClass, String name, StorageType returnType, StorageType[] formals) {
		this(formals, definingClass, name, returnType, IllegalArgumentException::new);
	}

	/**
	 * Unmarshals a method signature from the given context. The number of formals has already been read.
	 * 
	 * @param context the unmarshalling context
	 * @param numberOfFoirmals the number of formal arguments
	 * @throws IOException if the unmarshalling failed
	 */
	public NonVoidMethodSignatureImpl(UnmarshallingContext context, int numberOfFoirmals) throws IOException {
		this(
			unmarshalFormals(context, numberOfFoirmals),
			unmarshalDefiningClass(context),
			context.readStringUnshared(),
			StorageTypes.from(context),
			IOException::new
		);
	}

	/**
	 * Creates a method signature from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public NonVoidMethodSignatureImpl(MethodSignatureJson json) throws InconsistentJsonException {
		this(
			formalsAsTypes(json),
			ClassTypeImpl.named(json.getDefiningClass(), InconsistentJsonException::new),
			json.getName(),
			AbstractStorageType.named(json.getReturnType().orElseThrow(() -> new InconsistentJsonException("Missing returnType")), InconsistentJsonException::new),
			InconsistentJsonException::new
		);
	}

	/**
	 * Builds the signature of a method, that returns a value.
	 * 
	 * @param <E> the type of the exception thrown if some arguments is illegal
	 * @param formals the formal arguments of the method
	 * @param definingClass the class of the method
	 * @param name the name of the method
	 * @param returnType the type of the returned value
	 * @param onIllegalArgs the generator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> NonVoidMethodSignatureImpl(StorageType[] formals, ClassType definingClass, String name, StorageType returnType, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(definingClass, name, formals, onIllegalArgs);
	
		this.returnType = Objects.requireNonNull(returnType, "returnType cannot be null", onIllegalArgs);
	}

	@Override
	public StorageType getReturnType() {
		return returnType;
	}

	@Override
	public String toString() {
		return returnType + " " + super.toString();
	}

    @Override
	public boolean equals(Object other) {
		return other instanceof NonVoidMethodSignature nvms && returnType.equals(nvms.getReturnType()) && super.equals(other);
	}

    @Override
    public void into(MarshallingContext context) throws IOException {
    	if (MethodSignatures.MOKAMINT_VALIDATORS_REWARD.equals(this))
    		context.writeCompactInt(SELECTOR_VALIDATORS_REWARD_MOKAMINT);
    	else {
    		var formals = getFormals().toArray(StorageType[]::new);
    		context.writeCompactInt(formals.length * 2 + 1 + SELECTOR_VALIDATORS_REWARD_MOKAMINT_MINER + 1); // this signals that the method is non-void (see from() inside AbstractMethodSignature)
    		for (var formal: formals)
    			formal.into(context);

    		getDefiningClass().into(context);
    		context.writeStringUnshared(getName());
    		returnType.into(context);
    	}
    }
}