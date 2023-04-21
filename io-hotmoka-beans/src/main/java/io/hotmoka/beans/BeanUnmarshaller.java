package io.hotmoka.beans;

import java.io.IOException;
import java.io.InputStream;

import io.hotmoka.beans.Marshallable.Unmarshaller;

/**
 * A function that unmarshals a single marshallable bean.
 *
 * @param <T> the type of the marshallable bean
 */
public interface BeanUnmarshaller<T extends Marshallable<?>> extends Unmarshaller<T> {

	@Override
	default BeanUnmarshallingContext mkContext(InputStream is) throws IOException {
		return new BeanUnmarshallingContext(is);
	}
}