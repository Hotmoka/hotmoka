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

package io.hotmoka.instrumentation.internal;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import io.hotmoka.instrumentation.InstrumentedClasses;
import io.hotmoka.instrumentation.api.GasCostModel;
import io.hotmoka.instrumentation.api.InstrumentedClass;
import io.hotmoka.instrumentation.api.InstrumentedJar;
import io.hotmoka.verification.VerificationException;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.VerifiedClass;
import io.hotmoka.verification.api.VerifiedJar;

/**
 * An instrumented jar file, built from another, verified jar file. This means
 * for instance that storage classes get modified to account for persistence and
 * contracts get modified to implement entries.
 */
public class InstrumentedJarImpl implements InstrumentedJar {

	/**
	 * The instrumented classes of the jar.
	 */
	private final SortedSet<InstrumentedClass> classes;

	/**
	 * Instruments the given jar file into another jar file. This instrumentation
	 * might fail if at least a class did not verify.
	 * 
	 * @param verifiedJar the jar that contains the classes already verified
	 * @param gasCostModel the gas cost model used for the instrumentation
	 * @throws IllegalJarException if {@code verifiedJar} is illegal
	 * @throws VerificationException if {@code verifiedJar} has some error
	 */
	public InstrumentedJarImpl(VerifiedJar verifiedJar, GasCostModel gasCostModel) throws IllegalJarException, VerificationException {
		var firstError = verifiedJar.getErrors().findFirst();
		if (firstError.isPresent())
			throw new VerificationException(firstError.get());

		this.classes = new TreeSet<>();
		// we cannot proceed in parallel since the BCEL library is not thread-safe
		for (var verifiedClass: verifiedJar.getClasses().toArray(VerifiedClass[]::new))
			this.classes.add(InstrumentedClasses.of(verifiedClass, gasCostModel));
	}

	@Override
	public void dump(Path destination) throws IOException {
		try (var instrumentedJar = new JarOutputStream(new FileOutputStream(destination.toFile()))) {
			for (var clazz: classes)
				dumpInstrumentedClass(clazz, instrumentedJar);
		}
	}

	@Override
	public byte[] toBytes() {
		var byteArray = new ByteArrayOutputStream();
		try (var instrumentedJar = new JarOutputStream(byteArray)) {
			for (var clazz: classes)
				dumpInstrumentedClass(clazz, instrumentedJar);
		}
		catch (IOException e) {
			// this should not happen with a ByteArrayOutputStream
			throw new RuntimeException(e);
		}

		return byteArray.toByteArray();
	}

	@Override
	public Stream<InstrumentedClass> classes() {
		return classes.stream();
	}

	/**
	 * Dumps the given class into a jar file.
	 * 
	 * @param instrumentedClass the class
	 * @param instrumentedJar the jar where the instrumented class must be dumped
	 * @throws IOException if an I/O error occurs
	 */
	private static void dumpInstrumentedClass(InstrumentedClass instrumentedClass, JarOutputStream instrumentedJar) throws IOException {
		// add the same entry to the resulting jar
		var entry = new JarEntry(instrumentedClass.getClassName().replace('.', '/') + ".class");
		entry.setTime(0L); // we set the timestamp to 0, so that the result is deterministic
		instrumentedJar.putNextEntry(entry);

		// dumps the class into the jar file
		instrumentedClass.toJavaClass().dump(instrumentedJar);
	}
}