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

package it.univr.bcel.internal;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.bcel.classfile.StackMap;
import org.apache.bcel.generic.MethodGen;

import it.univr.bcel.StackMapInferrer;
import it.univr.bcel.StackMapReplacer;

/**
 * An inference algorithm that recomputes the stack map for a method or constructor
 * and replaces its old stack map (if any) with the recomputed one.
 * 
 * @author Fausto Spoto <fausto.spoto@univr.it>
 */
public class StackMapReplacerImpl implements StackMapReplacer {

	/**
	 * Removes the old stack map of the given method or constructor (if any)
	 * and replaces it with a new, inferred stack map (if needed).
	 * 
	 * @param methodOrConstructor the method or constructor
	 */
	public StackMapReplacerImpl(MethodGen methodOrConstructor) {
		Optional<StackMap> newStackMap = StackMapInferrer.of(methodOrConstructor).getStackMap();
		// we drop the old stack map only if the new one could be computed (no exceptions)
		dropOldStackMap(methodOrConstructor);
		newStackMap.ifPresent(methodOrConstructor::addCodeAttribute);
	}

	/**
	 * Removes the old stack map of the method or constructor, if any.
	 * 
	 * @param methodOrConstructor the method or constructor
	 */
	private static void dropOldStackMap(MethodGen methodOrConstructor) {
		Stream.of(methodOrConstructor.getCodeAttributes())
			.filter(attribute -> attribute instanceof StackMap)
			.map(attribute -> (StackMap) attribute)
			.findAny()
			.ifPresent(methodOrConstructor::removeCodeAttribute);
	}
}