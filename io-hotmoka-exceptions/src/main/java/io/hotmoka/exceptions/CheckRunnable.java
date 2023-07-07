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

/**
 */
public abstract class CheckRunnable {

	@SuppressWarnings("unchecked")
	public static <E extends Exception> void check2(Class<E> exception, Runnable runnable) throws E {
		try {
			runnable.run();
		}
		catch (UncheckedException2 e) {
			var cause = e.getCause();
			if (exception.isInstance(cause))
				throw (E) cause;
			else
				throw e;
		}
	}

	@SuppressWarnings("unchecked")
	public static <E1 extends Exception, E2 extends Exception> void check2(Class<E1> exception1, Class<E2> exception2, Runnable runnable) throws E1, E2 {
		try {
			runnable.run();
		}
		catch (UncheckedException2 e) {
			var cause = e.getCause();
			if (exception1.isInstance(cause))
				throw (E1) cause;
			else if (exception2.isInstance(cause))
				throw (E2) cause;
			else
				throw e;
		}
	}

	@SuppressWarnings("unchecked")
	public static <E1 extends Exception, E2 extends Exception, E3 extends Exception> void check2(Class<E1> exception1, Class<E2> exception2, Class<E3> exception3, Runnable runnable) throws E1, E2, E3 {
		try {
			runnable.run();
		}
		catch (UncheckedException2 e) {
			var cause = e.getCause();
			if (exception1.isInstance(cause))
				throw (E1) cause;
			else if (exception2.isInstance(cause))
				throw (E2) cause;
			else if (exception3.isInstance(cause))
				throw (E3) cause;
			else
				throw e;
		}
	}

	@SuppressWarnings("unchecked")
	public static <CX extends Exception, X extends UncheckedException<CX>> void check(Class<X> exception1, Runnable runnable) throws CX {

		try {
			runnable.run();
		}
		catch (Throwable t) {
			if (exception1.isInstance(t))
				throw (CX) t.getCause();
			else
				throw t;
		}
	}

	@SuppressWarnings("unchecked")
	public static <CX extends Exception, X extends UncheckedException<CX>, CY extends Exception, Y extends UncheckedException<CY>> void check
		(Class<X> exception1, Class<Y> exception2, Runnable runnable) throws CX, CY {

		try {
			runnable.run();
		}
		catch (Throwable t) {
			if (exception1.isInstance(t))
				throw (CX) t.getCause();
			else if (exception2.isInstance(t))
				throw (CY) t.getCause();
			else
				throw t;
		}
	}

	@SuppressWarnings("unchecked")
	public static <CX extends Exception, X extends UncheckedException<CX>, CY extends Exception, Y extends UncheckedException<CY>, CZ extends Exception, Z extends UncheckedException<CZ>> void check
		(Class<X> exception1, Class<Y> exception2, Class<Z> exception3, Runnable runnable) throws CX, CY, CZ {

		try {
			runnable.run();
		}
		catch (Throwable t) {
			if (exception1.isInstance(t))
				throw (CX) t.getCause();
			else if (exception2.isInstance(t))
				throw (CY) t.getCause();
			else if (exception3.isInstance(t))
				throw (CZ) t.getCause();
			else
				throw t;
		}
	}
}