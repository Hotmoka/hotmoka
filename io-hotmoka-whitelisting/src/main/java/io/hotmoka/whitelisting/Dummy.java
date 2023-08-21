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

package io.hotmoka.whitelisting;

/**
 * This class is used only in the instrumentation of an entry method or constructor,
 * as an extra type added at the end of its signature: {@code m(formals)} becomes
 * {@code m(formals, Contract, Dummy)}, where the {@code io.takamaka.code.lang.Contract}
 * is the caller of the entry. The goal is to avoid signature clashes
 * because of the instrumentation: since this class is not white-listed, it cannot
 * be used by the programmer and the instrumentation cannot lead to signature clashes.
 * Moreover, the value passed for this extra parameter can be used to signal something to the callee.
 */
public final class Dummy {
	
	/**
	 * This value is passed to a from contract method to signal that it has
	 * been called on this in the caller.
	 */
	public final static Dummy METHOD_ON_THIS = new Dummy();

	private Dummy() {}
}