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

package io.hotmoka.beans.responses;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.hotmoka.beans.BeanMarshallable;
import io.hotmoka.beans.BeanMarshallingContext;
import io.hotmoka.beans.UnmarshallingContext;

/**
 * The response of a transaction.
 */
public abstract class TransactionResponse extends BeanMarshallable {

	/**
	 * Used to marshal requests that are specific to a node.
	 * After this selector, the qualified name of the request must be follow.
	 */
	protected final static byte EXPANSION_SELECTOR = 15;

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the response could not be unmarshalled
	 * @throws ClassNotFoundException if the response could not be unmarshalled
	 */
	public static TransactionResponse from(UnmarshallingContext context) throws IOException, ClassNotFoundException {
		byte selector = context.readByte();

		switch (selector) {
		case GameteCreationTransactionResponse.SELECTOR: return GameteCreationTransactionResponse.from(context);
		case JarStoreInitialTransactionResponse.SELECTOR: return JarStoreInitialTransactionResponse.from(context);
		case InitializationTransactionResponse.SELECTOR: return InitializationTransactionResponse.from(context);
		case JarStoreTransactionFailedResponse.SELECTOR: return JarStoreTransactionFailedResponse.from(context);
		case JarStoreTransactionSuccessfulResponse.SELECTOR: return JarStoreTransactionSuccessfulResponse.from(context);
		case ConstructorCallTransactionExceptionResponse.SELECTOR: return ConstructorCallTransactionExceptionResponse.from(context);
		case ConstructorCallTransactionFailedResponse.SELECTOR: return ConstructorCallTransactionFailedResponse.from(context);
		case ConstructorCallTransactionSuccessfulResponse.SELECTOR:
		case ConstructorCallTransactionSuccessfulResponse.SELECTOR_NO_EVENTS: return ConstructorCallTransactionSuccessfulResponse.from(context, selector);
		case MethodCallTransactionExceptionResponse.SELECTOR: return MethodCallTransactionExceptionResponse.from(context);
		case MethodCallTransactionFailedResponse.SELECTOR: return MethodCallTransactionFailedResponse.from(context);
		case MethodCallTransactionSuccessfulResponse.SELECTOR:
		case MethodCallTransactionSuccessfulResponse.SELECTOR_NO_EVENTS_NO_SELF_CHARGED:
		case MethodCallTransactionSuccessfulResponse.SELECTOR_ONE_EVENT_NO_SELF_CHARGED: return MethodCallTransactionSuccessfulResponse.from(context, selector);
		case VoidMethodCallTransactionSuccessfulResponse.SELECTOR:
		case VoidMethodCallTransactionSuccessfulResponse.SELECTOR_NO_EVENTS_NO_SELF_CHARGED: return VoidMethodCallTransactionSuccessfulResponse.from(context, selector);
		case EXPANSION_SELECTOR: {
			// this case deals with responses that only exist in a specific type of node;
			// hence their fully-qualified name must be available after the expansion selector

			String className = context.readUTF();
			Class<?> clazz = Class.forName(className, false, ClassLoader.getSystemClassLoader());

			// only subclass of TransactionResponse are considered, to block potential call injections
			if (!TransactionResponse.class.isAssignableFrom(clazz))
				throw new IOException("unkown response class " + className);

			Method from;
			try {
				from = clazz.getMethod("from", UnmarshallingContext.class);
			}
			catch (NoSuchMethodException | SecurityException e) {
				throw new IOException("cannot find method " + className + ".from(UnmarshallingContext)");
			}

			try {
				return (TransactionResponse) from.invoke(null, context);
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new IOException("cannot call method " + className + ".from(UnmarshallingContext)");
			}
		}
		default: throw new IOException("unexpected response selector: " + selector);
		}
	}

	protected static byte[] instrumentedJarFrom(UnmarshallingContext context) throws IOException {
		int instrumentedJarLength = context.readInt();
		return context.readBytes(instrumentedJarLength, "jar length mismatch in response");
	}

	@Override
	protected BeanMarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new BeanMarshallingContext(os);
	}
}