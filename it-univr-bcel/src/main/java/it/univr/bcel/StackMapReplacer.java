/*
Copyright 2019 Fausto Spoto (fausto.spoto@univr.it)

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

package it.univr.bcel;

import org.apache.bcel.generic.MethodGen;

import it.univr.bcel.internal.StackMapReplacerImpl;

/**
 * An inference algorithm that recomputes the stack map for a method or constructor
 * and replaces its old stack map (if any) with the recomputed one.
 * 
 * @author Fausto Spoto <fausto.spoto@univr.it>
 */
public interface StackMapReplacer {

	/**
	 * Removes the old stack map of the given method or constructor (if any)
	 * and replaces it with a new, inferred stack map (if needed).
	 * 
	 * @param methodOrConstructor the method or constructor
	 */
	static StackMapReplacer of(MethodGen methodOrConstructor) {
		return new StackMapReplacerImpl(methodOrConstructor);
	}
}