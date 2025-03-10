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
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.internal.gson.ConstructorSignatureJson;
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
	public ConstructorSignatureImpl(ClassType definingClass, StorageType... formals) {
		super(definingClass, formals);
	}

	/**
	 * Yields a constructor signature from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public ConstructorSignatureImpl(ConstructorSignatureJson json) throws InconsistentJsonException {
		super(getDefiningClassAsType(json), getFormalsAsTypes(json));
	}

	private static ClassType getDefiningClassAsType(ConstructorSignatureJson json) throws InconsistentJsonException {
		String className = json.getDefiningClass();
		if (className == null)
			throw new InconsistentJsonException("definingClass cannot be null");

		return StorageTypes.classNamed(className, InconsistentJsonException::new);
	}

	private static StorageType[] getFormalsAsTypes(ConstructorSignatureJson json) throws InconsistentJsonException {
		var formals = json.getFormals().toArray(String[]::new);
		var formalsAsTypes = new StorageType[formals.length];
		int pos = 0;
		for (var formal: formals)
			formalsAsTypes[pos++] = StorageTypes.named(formal, InconsistentJsonException::new);

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
	 * Factory method that unmarshals a constructor signature from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the constructor signature
	 * @throws IOException if the constructor signature cannot be unmarshalled
	 */
	public static ConstructorSignature from(UnmarshallingContext context) throws IOException {
		if (!(StorageTypes.from(context) instanceof ClassType definingClass))
			throw new IOException("The type defining a constructor must be a class type");

		var formals = context.readLengthAndArray(StorageTypes::from, StorageType[]::new);

		return ConstructorSignatures.of(definingClass, formals);
	}

	/**
	 * The constructor of an externally owned account with a big integer amount.
	 */
	public final static ConstructorSignature EOA_CONSTRUCTOR = ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.BIG_INTEGER, StorageTypes.STRING);
}