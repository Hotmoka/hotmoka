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

package io.hotmoka.beans;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.StorageReference;

/**
 * A context used during bean marshalling into bytes.
 */
public class BeanMarshallingContext extends MarshallingContext {
	private final Map<StorageReference, Integer> memoryStorageReference = new HashMap<>();
	private final Map<FieldSignature, Integer> memoryFieldSignature = new HashMap<>();
	private final Map<TransactionReference, Integer> memoryTransactionReference = new HashMap<>();

	public BeanMarshallingContext(OutputStream oos) throws IOException {
		super(oos);
	}

	/**
	 * Writes the given storage reference into the output stream. It uses
	 * a memory to recycle storage references already written with this context
	 * and compress them by using their progressive number instead.
	 * 
	 * @param reference the storage reference to write
	 * @throws IOException if the storage reference could not be written
	 */
	public void writeStorageReference(StorageReference reference) throws IOException {
		Integer index = memoryStorageReference.get(reference);
		if (index != null) {
			if (index < 254)
				writeByte(index);
			else {
				writeByte(254);
				writeInt(index);
			}
		}
		else {
			int next = memoryStorageReference.size();
			if (next == Integer.MAX_VALUE) // irrealistic
				throw new IllegalStateException("too many storage references in the same context");

			memoryStorageReference.put(reference, next);

			writeByte(255);
			reference.transaction.into(this);
			writeBigInteger(reference.progressive);
		}
	}

	/**
	 * Writes the given field signature into the output stream. It uses
	 * a memory to recycle field signatures already written with this context
	 * and compress them by using their progressive number instead.
	 * 
	 * @param field the field signature to write
	 * @throws IOException if the field signature could not be written
	 */
	public void writeFieldSignature(FieldSignature field) throws IOException {
		Integer index = memoryFieldSignature.get(field);
		if (index != null) {
			if (index < 254)
				writeByte(index);
			else {
				writeByte(254);
				writeInt(index);
			}
		}
		else {
			int next = memoryFieldSignature.size();
			if (next == Integer.MAX_VALUE) // irrealistic
				throw new IllegalStateException("too many field signatures in the same context");

			memoryFieldSignature.put(field, next);

			writeByte(255);
			field.definingClass.into(this);
			writeUTF(field.name);
			field.type.into(this);
		}
	}

	/**
	 * Writes the given transaction reference into the output stream. It uses
	 * a memory to recycle transaction references already written with this context
	 * and compress them by using their progressive number instead.
	 * 
	 * @param transaction the transaction reference to write
	 * @throws IOException IOException if the transaction reference could not be written
	 */
	public void writeTransactionReference(TransactionReference transaction) throws IOException {
		Integer index = memoryTransactionReference.get(transaction);
		if (index != null) {
			if (index < 254)
				writeByte(index);
			else {
				writeByte(254);
				writeInt(index);
			}
		}
		else {
			int next = memoryTransactionReference.size();
			if (next == Integer.MAX_VALUE) // irrealistic
				throw new IllegalStateException("too many transaction references in the same context");

			memoryTransactionReference.put(transaction, next);

			writeByte(255);
			write(transaction.getHashAsBytes());
		}
	}
}