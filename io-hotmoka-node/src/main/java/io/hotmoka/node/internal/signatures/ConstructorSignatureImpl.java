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
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.internal.json.ConstructorSignatureJson;
import io.hotmoka.node.internal.types.AbstractStorageType;
import io.hotmoka.node.internal.types.ClassTypeImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The signature of a constructor of a class.
 */
@Immutable
public final class ConstructorSignatureImpl extends AbstractCodeSignature implements ConstructorSignature {

	/**
	 * Builds the signature of a constructor.
	 * 
	 * @param definingClass the class defining the constructor
	 * @param formals the formal arguments of the constructor
	 */
	public ConstructorSignatureImpl(ClassType definingClass, StorageType[] formals) {
		super(definingClass, formals, IllegalArgumentException::new);
	}

	/**
	 * Yields a constructor signature from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public ConstructorSignatureImpl(ConstructorSignatureJson json) throws InconsistentJsonException {
		this(
			ClassTypeImpl.named(json.getDefiningClass(), InconsistentJsonException::new),
			getFormalsAsTypes(json),
			InconsistentJsonException::new
		);
	}

	/**
	 * Unmarshals a constructor signature from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @throws IOException if the constructor signature cannot be unmarshalled
	 */
	public ConstructorSignatureImpl(UnmarshallingContext context) throws IOException {
		this(
			unmarshalDefiningClass(context),
			context.readLengthAndArray(StorageTypes::from, StorageType[]::new),
			IOException::new
		);
	}

	/**
	 * Builds the signature of a constructor.
	 * 
	 * @param <E> the type of the exception thrown if some arguments is illegal
	 * @param definingClass the class defining the constructor
	 * @param formals the formal arguments of the constructor
	 * @param onIllegalArgs the generator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> ConstructorSignatureImpl(ClassType definingClass, StorageType[] formals, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(definingClass, formals, onIllegalArgs);
	}

	private static StorageType[] getFormalsAsTypes(ConstructorSignatureJson json) throws InconsistentJsonException {
		var formals = json.getFormals().toArray(String[]::new);
		var formalsAsTypes = new StorageType[formals.length];
		int pos = 0;
		for (var formal: formals)
			formalsAsTypes[pos++] = AbstractStorageType.named(formal, InconsistentJsonException::new);

		return formalsAsTypes;
	}

	@Override
	public String toString() {
		return getDefiningClass() + commaSeparatedFormals();
	}

    @Override
	public boolean equals(Object other) {
		return other instanceof ConstructorSignature && super.equals(other);
	}

    @Override
    public void into(MarshallingContext context) throws IOException {
    	getDefiningClass().into(context);
		context.writeLengthAndArray(getFormals().toArray(StorageType[]::new));
    }

    /**
	 * The constructor of an externally owned account with a big integer amount.
	 */
	public final static ConstructorSignature EOA_CONSTRUCTOR = ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.BIG_INTEGER, StorageTypes.STRING);
}