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

package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.marshalling.BeanMarshallingContext;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * A request of a transaction.
 * 
 * @param <R> the type of the response expected for this request
 */
@Immutable
public abstract class TransactionRequest<R extends TransactionResponse> extends AbstractMarshallable {

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the request cannot be unmarshalled
	 */
	public static TransactionRequest<?> from(UnmarshallingContext context) throws IOException {
		byte selector = context.readByte();
		switch (selector) {
		case ConstructorCallTransactionRequest.SELECTOR: return ConstructorCallTransactionRequest.from(context);
		case InitializationTransactionRequest.SELECTOR: return InitializationTransactionRequest.from(context);
		case InstanceMethodCallTransactionRequest.SELECTOR:
		case InstanceMethodCallTransactionRequest.SELECTOR_TRANSFER_INT:
		case InstanceMethodCallTransactionRequest.SELECTOR_TRANSFER_LONG:
		case InstanceMethodCallTransactionRequest.SELECTOR_TRANSFER_BIG_INTEGER:
			return InstanceMethodCallTransactionRequest.from(context, selector);
		case JarStoreInitialTransactionRequest.SELECTOR: return JarStoreInitialTransactionRequest.from(context);
		case JarStoreTransactionRequest.SELECTOR: return JarStoreTransactionRequest.from(context);
		case GameteCreationTransactionRequest.SELECTOR: return GameteCreationTransactionRequest.from(context);
		case StaticMethodCallTransactionRequest.SELECTOR: return StaticMethodCallTransactionRequest.from(context);
		case InstanceSystemMethodCallTransactionRequest.SELECTOR: return InstanceSystemMethodCallTransactionRequest.from(context);
		default: throw new IOException("Unexpected request selector: " + selector);
		}
	}

	/**
	 * Translates an array of bytes into a hexadecimal string.
	 * 
	 * @param bytes the bytes
	 * @return the string
	 */
	protected static String bytesToHex(byte[] bytes) {
	    var hexChars = new byte[bytes.length * 2];
	    int pos = 0;
	    for (byte b: bytes) {
	        int v = b & 0xFF;
	        hexChars[pos++] = HEX_ARRAY[v >>> 4];
	        hexChars[pos++] = HEX_ARRAY[v & 0x0F];
	    }
	
	    return new String(hexChars, StandardCharsets.UTF_8);
	}

	/**
	 * The array of hexadecimal digits.
	 */
	private final static byte[] HEX_ARRAY = "0123456789abcdef".getBytes();

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new BeanMarshallingContext(os);
	}
}