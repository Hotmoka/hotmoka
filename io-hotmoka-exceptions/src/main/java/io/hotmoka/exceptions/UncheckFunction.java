/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.exceptions;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Function;

/**
 */
public abstract class UncheckFunction {

	public static <T, R> Function<T, R> uncheck(FunctionWithExceptions<T, R> wrapped) {
		return new Function<>() {

			@Override
			public R apply(T t) {
				try {
					return wrapped.apply(t);
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				catch (NoSuchAlgorithmException e) {
					throw new UncheckedNoSuchAlgorithmException(e);
				}
				catch (InterruptedException e) {
					throw new UncheckedInterruptedException(e);
				}
				catch (URISyntaxException e) {
					throw new UncheckedURISyntaxException(e);
				}
				catch (ClassNotFoundException e) {
					throw new UncheckedClassNotFoundException(e);
				}
			}
		};
	}
}