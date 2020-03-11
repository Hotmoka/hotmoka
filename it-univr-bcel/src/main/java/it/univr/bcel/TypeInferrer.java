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

import java.io.IOException;
import java.util.stream.Stream;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import it.univr.bcel.internal.TypeInferrerImpl;

/**
 * An inference algorithm for the static types at the instructions of a method or constructor.
 * 
 * @author Fausto Spoto <fausto.spoto@univr.it>
 */
public interface TypeInferrer {

	/**
	 * Performs type inference for the given method or constructor.
	 * The types at each of its instruction handles can be later accessed
	 * through {@link it.univr.bcel.TypeInferrer#getTypesAt(InstructionHandle)}.
	 * 
	 * @param methodOrConstructor the method or constructor
	 */
	static TypeInferrer of(MethodGen methodOrConstructor) {
		return new TypeInferrerImpl(methodOrConstructor);
	}

	/**
	 * Yields the types inferred at the given instruction handle.
	 * 
	 * @param where the instruction handle
	 * @return the types
	 */
	Types getTypesAt(InstructionHandle where);

	/**
	 * Performs type inference for all constructors and methods in a given class.
	 * 
	 * @param args the first element must contain the name of a file containing the bytecode
	 *             of the class whose constructors and methods must be processed
	 * @throws ClassFormatException if the class file is corrupted
	 * @throws IOException if the file containing the bytecode cannot be accessed
	 */
	static void main(String[] args) throws ClassFormatException, IOException {
		ClassGen classGen = new ClassGen(new ClassParser(args[0]).parse());
		String className = classGen.getClassName();
		ConstantPoolGen cpg = classGen.getConstantPool();

		Stream.of(classGen.getMethods())
			.map(method -> new MethodGen(method, className, cpg))
			.peek(methodGen -> System.out.print(methodGen.getName()))
			.map(TypeInferrer::of)
			.forEach(_types -> System.out.println(" ok!"));
	}
}