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

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.api.signatures.ConstructorSignature;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * The signature of a constructor of a class.
 */
@Immutable
public final class ConstructorSignatureImpl extends AbstractCodeSignature implements ConstructorSignature {

	/**
	 * Builds the signature of a constructor.
	 * 
	 * @param definingClass the class of the constructor
	 * @param formals the formal arguments of the constructor
	 */
	public ConstructorSignatureImpl(ClassType definingClass, StorageType... formals) {
		super(definingClass, formals);
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
		ClassType definingClass;

		try {
			definingClass = (ClassType) StorageTypes.from(context);
		}
		catch (ClassCastException e) {
			throw new IOException("Failed to unmarshal a code signature", e);
		}

		var formals = context.readLengthAndArray(StorageTypes::from, StorageType[]::new);
		return ConstructorSignatures.of(definingClass, formals);
	}

	/**
	 * The constructor of an externally owned account with a big integer amount.
	 */
	public final static ConstructorSignature EOA_CONSTRUCTOR = ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.BIG_INTEGER, StorageTypes.STRING);
}