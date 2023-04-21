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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An object that can be marshalled into a stream, in a way
 * more compact than standard Java serialization. TYpically,
 * this works because of context information about the structure
 * of the object.
 */
public abstract class Marshallable<C extends MarshallingContext> {

	/**
	 * Marshals this object into a given stream. This method in general
	 * performs better than standard Java serialization, wrt the size of the marshalled data.
	 * 
	 * @param context the context holding the stream
	 * @throws IOException if this object cannot be marshalled
	 */
	public abstract void into(C context) throws IOException;

	/**
	 * Marshals an array of marshallables into a given stream.
	 * 
	 * @param marshallables the array of marshallables
	 * @param context the context holding the stream
	 * @throws IOException if some element could not be marshalled
	 */
	public static <C extends MarshallingContext> void intoArray(Marshallable<C>[] marshallables, C context) throws IOException {
		context.writeCompactInt(marshallables.length);

		for (Marshallable<C> marshallable: marshallables)
			marshallable.into(context);
	}

	/**
	 * Creates a marshalling context for this object.
	 * 
	 * @param os the output stream of the context
	 * @return the marshalling context
	 * @throws IOException if the marshalling context cannot be created
	 */
	protected abstract C createMarshallingContext(OutputStream os) throws IOException;

	/**
	 * Marshals this object into a byte array.
	 * 
	 * @return the byte array resulting from marshalling this object
	 * @throws IOException if this object cannot be marshalled
	 */
	public final byte[] toByteArray() throws IOException {
		try (var baos = new ByteArrayOutputStream(); var context = createMarshallingContext(baos)) {
			into(context);
			context.flush();
			return baos.toByteArray();
		}
	}

	/**
	 * A provider of a marshalling context from its output stream.
	 */
	public interface MarshallingContextProvider<C extends MarshallingContext> {
		C apply(OutputStream os) throws IOException;
	}

	/**
	 * Marshals an array of marshallables into a byte array.
	 * 
	 * @return the byte array resulting from marshalling the array of marshallables
	 * @throws IOException if some marshallable could not be marshalled
	 */
	public static <C extends MarshallingContext> byte[] toByteArray(Marshallable<C>[] marshallables, MarshallingContextProvider<C> contextProvider) throws IOException {
		try (var baos = new ByteArrayOutputStream(); var context = contextProvider.apply(baos)) {
			intoArray(marshallables, context);
			context.flush();
			return baos.toByteArray();
		}
	}

	/**
	 * A function that unmarshals a single marshallable.
	 *
	 * @param <T> the type of the marshallable
	 */
	public interface Unmarshaller<T extends Marshallable<?>> {
		default UnmarshallingContext mkContext(InputStream is) throws IOException {
			return new UnmarshallingContext(is);
		}

		T from(UnmarshallingContext context) throws IOException, ClassNotFoundException;
	}	
}