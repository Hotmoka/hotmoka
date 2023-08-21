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
 * Methods that check an unchecked exception thrown by a runnable.
 */
public abstract class CheckRunnable {

	private CheckRunnable() {}

	/**
	 * Runs a runnable and makes an unchecked exception type into checked.
	 * 
	 * @param <T> the type of the exception
	 * @param exception the class of the exception
	 * @param runnable the runnable
	 * @throws T if the runnable throws an unchecked exception with this cause
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Throwable> void check(Class<T> exception, Runnable runnable) throws T {
		try {
			runnable.run();
		}
		catch (UncheckedException e) {
			var cause = e.getCause();
			if (exception.isInstance(cause))
				throw (T) cause;
			else
				throw e;
		}
	}

	/**
	 * Runs a runnable and makes two unchecked exception types into checked.
	 * 
	 * @param <T1> the first type of the exception
	 * @param <T2> the second type of the exception
	 * @param exception1 the class of the first exception
	 * @param exception2 the class of the second exception
	 * @param runnable the runnable
	 * @throws T1 if the runnable throws an unchecked exception with this cause
	 * @throws T2 if the runnable throws an unchecked exception with this cause
	 */
	@SuppressWarnings("unchecked")
	public static <T1 extends Throwable, T2 extends Throwable> void check(Class<T1> exception1, Class<T2> exception2, Runnable runnable) throws T1, T2 {
		try {
			runnable.run();
		}
		catch (UncheckedException e) {
			var cause = e.getCause();
			if (exception1.isInstance(cause))
				throw (T1) cause;
			else if (exception2.isInstance(cause))
				throw (T2) cause;
			else
				throw e;
		}
	}

	/**
	 * Runs a runnable and makes three unchecked exception types into checked.
	 * 
	 * @param <T1> the first type of the exception
	 * @param <T2> the second type of the exception
	 * @param <T3> the third type of the exception
	 * @param exception1 the class of the first exception
	 * @param exception2 the class of the second exception
	 * @param exception3 the class of the third exception
	 * @param runnable the runnable
	 * @throws T1 if the runnable throws an unchecked exception with this cause
	 * @throws T2 if the runnable throws an unchecked exception with this cause
	 * @throws T3 if the runnable throws an unchecked exception with this cause
	 */
	@SuppressWarnings("unchecked")
	public static <T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> void check(Class<T1> exception1, Class<T2> exception2, Class<T3> exception3, Runnable runnable) throws T1, T2, T3 {
		try {
			runnable.run();
		}
		catch (UncheckedException e) {
			var cause = e.getCause();
			if (exception1.isInstance(cause))
				throw (T1) cause;
			else if (exception2.isInstance(cause))
				throw (T2) cause;
			else if (exception3.isInstance(cause))
				throw (T3) cause;
			else
				throw e;
		}
	}

	/**
	 * Runs a runnable and makes four unchecked exception types into checked.
	 * 
	 * @param <T1> the first type of the exception
	 * @param <T2> the second type of the exception
	 * @param <T3> the third type of the exception
	 * @param <T4> the fourth type of the exception
	 * @param exception1 the class of the first exception
	 * @param exception2 the class of the second exception
	 * @param exception3 the class of the third exception
	 * @param exception4 the class of the fourth exception
	 * @param runnable the runnable
	 * @throws T1 if the runnable throws an unchecked exception with this cause
	 * @throws T2 if the runnable throws an unchecked exception with this cause
	 * @throws T3 if the runnable throws an unchecked exception with this cause
	 * @throws T4 if the runnable throws an unchecked exception with this cause
	 */
	@SuppressWarnings("unchecked")
	public static <T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, T4 extends Throwable>
			void check(Class<T1> exception1, Class<T2> exception2, Class<T3> exception3, Class<T4> exception4, Runnable runnable) throws T1, T2, T3, T4 {

		try {
			runnable.run();
		}
		catch (UncheckedException e) {
			var cause = e.getCause();
			if (exception1.isInstance(cause))
				throw (T1) cause;
			else if (exception2.isInstance(cause))
				throw (T2) cause;
			else if (exception3.isInstance(cause))
				throw (T3) cause;
			else if (exception4.isInstance(cause))
				throw (T4) cause;
			else
				throw e;
		}
	}

	/**
	 * Runs a runnable and makes five unchecked exception types into checked.
	 * 
	 * @param <T1> the first type of the exception
	 * @param <T2> the second type of the exception
	 * @param <T3> the third type of the exception
	 * @param <T4> the fourth type of the exception
	 * @param <T5> the fifth type of the exception
	 * @param exception1 the class of the first exception
	 * @param exception2 the class of the second exception
	 * @param exception3 the class of the third exception
	 * @param exception4 the class of the fourth exception
	 * @param exception5 the class of the fifth exception
	 * @param runnable the runnable
	 * @throws T1 if the runnable throws an unchecked exception with this cause
	 * @throws T2 if the runnable throws an unchecked exception with this cause
	 * @throws T3 if the runnable throws an unchecked exception with this cause
	 * @throws T4 if the runnable throws an unchecked exception with this cause
	 * @throws T5 if the runnable throws an unchecked exception with this cause
	 */
	@SuppressWarnings("unchecked")
	public static <T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, T4 extends Throwable, T5 extends Throwable>
			void check(Class<T1> exception1, Class<T2> exception2, Class<T3> exception3, Class<T4> exception4, Class<T5> exception5, Runnable runnable) throws T1, T2, T3, T4, T5 {

		try {
			runnable.run();
		}
		catch (UncheckedException e) {
			var cause = e.getCause();
			if (exception1.isInstance(cause))
				throw (T1) cause;
			else if (exception2.isInstance(cause))
				throw (T2) cause;
			else if (exception3.isInstance(cause))
				throw (T3) cause;
			else if (exception4.isInstance(cause))
				throw (T4) cause;
			else if (exception5.isInstance(cause))
				throw (T5) cause;
			else
				throw e;
		}
	}

	/**
	 * Runs a runnable and makes six unchecked exception types into checked.
	 * 
	 * @param <T1> the first type of the exception
	 * @param <T2> the second type of the exception
	 * @param <T3> the third type of the exception
	 * @param <T4> the fourth type of the exception
	 * @param <T5> the fifth type of the exception
	 * @param <T6> the sixth type of the exception
	 * @param exception1 the class of the first exception
	 * @param exception2 the class of the second exception
	 * @param exception3 the class of the third exception
	 * @param exception4 the class of the fourth exception
	 * @param exception5 the class of the fifth exception
	 * @param exception6 the class of the sixth exception
	 * @param runnable the runnable
	 * @throws T1 if the runnable throws an unchecked exception with this cause
	 * @throws T2 if the runnable throws an unchecked exception with this cause
	 * @throws T3 if the runnable throws an unchecked exception with this cause
	 * @throws T4 if the runnable throws an unchecked exception with this cause
	 * @throws T5 if the runnable throws an unchecked exception with this cause
	 * @throws T6 if the runnable throws an unchecked exception with this cause
	 */
	@SuppressWarnings("unchecked")
	public static <T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, T4 extends Throwable, T5 extends Throwable, T6 extends Throwable>
			void check(Class<T1> exception1, Class<T2> exception2, Class<T3> exception3, Class<T4> exception4, Class<T5> exception5, Class<T6> exception6, Runnable runnable) throws T1, T2, T3, T4, T5, T6 {

		try {
			runnable.run();
		}
		catch (UncheckedException e) {
			var cause = e.getCause();
			if (exception1.isInstance(cause))
				throw (T1) cause;
			else if (exception2.isInstance(cause))
				throw (T2) cause;
			else if (exception3.isInstance(cause))
				throw (T3) cause;
			else if (exception4.isInstance(cause))
				throw (T4) cause;
			else if (exception5.isInstance(cause))
				throw (T5) cause;
			else if (exception6.isInstance(cause))
				throw (T6) cause;
			else
				throw e;
		}
	}

	/**
	 * Runs a runnable and makes seven unchecked exception types into checked.
	 * 
	 * @param <T1> the first type of the exception
	 * @param <T2> the second type of the exception
	 * @param <T3> the third type of the exception
	 * @param <T4> the fourth type of the exception
	 * @param <T5> the fifth type of the exception
	 * @param <T6> the sixth type of the exception
	 * @param <T7> the seventh type of the exception
	 * @param exception1 the class of the first exception
	 * @param exception2 the class of the second exception
	 * @param exception3 the class of the third exception
	 * @param exception4 the class of the fourth exception
	 * @param exception5 the class of the fifth exception
	 * @param exception6 the class of the sixth exception
	 * @param exception7 the class of the seventh exception
	 * @param runnable the runnable
	 * @throws T1 if the runnable throws an unchecked exception with this cause
	 * @throws T2 if the runnable throws an unchecked exception with this cause
	 * @throws T3 if the runnable throws an unchecked exception with this cause
	 * @throws T4 if the runnable throws an unchecked exception with this cause
	 * @throws T5 if the runnable throws an unchecked exception with this cause
	 * @throws T6 if the runnable throws an unchecked exception with this cause
	 * @throws T7 if the runnable throws an unchecked exception with this cause
	 */
	@SuppressWarnings("unchecked")
	public static <T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, T4 extends Throwable, T5 extends Throwable, T6 extends Throwable, T7 extends Throwable>
			void check(Class<T1> exception1, Class<T2> exception2, Class<T3> exception3, Class<T4> exception4, Class<T5> exception5, Class<T6> exception6, Class<T7> exception7, Runnable runnable) throws T1, T2, T3, T4, T5, T6, T7 {

		try {
			runnable.run();
		}
		catch (UncheckedException e) {
			var cause = e.getCause();
			if (exception1.isInstance(cause))
				throw (T1) cause;
			else if (exception2.isInstance(cause))
				throw (T2) cause;
			else if (exception3.isInstance(cause))
				throw (T3) cause;
			else if (exception4.isInstance(cause))
				throw (T4) cause;
			else if (exception5.isInstance(cause))
				throw (T5) cause;
			else if (exception6.isInstance(cause))
				throw (T6) cause;
			else if (exception7.isInstance(cause))
				throw (T7) cause;
			else
				throw e;
		}
	}
}