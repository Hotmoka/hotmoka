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

package io.hotmoka.tools.internal.moka;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.constants.Constants;
import io.hotmoka.nodes.Node;
import io.hotmoka.verification.TakamakaClassLoader;
import io.hotmoka.whitelisting.WhiteListingWizard;

class PrintAPI {
	private final Class<?> clazz;
	private final WhiteListingWizard whiteListingWizard;

	PrintAPI(Node node, TransactionReference jar, String className) throws ClassNotFoundException, TransactionRejectedException, TransactionException, CodeExecutionException {
		TakamakaClassLoader classloader = new ClassLoaderHelper(node).classloaderFor(jar);
		this.clazz = classloader.loadClass(className);
		this.whiteListingWizard = classloader.getWhiteListingWizard();
		printConstructors();
		printMethods();
	}

	private void printMethods() throws ClassNotFoundException {
		Comparator<Method> comparator = Comparator.comparing(Method::getName)
			.thenComparing(Method::toString);

		Method[] methods = clazz.getMethods();
		List<Method> defined = Stream.of(methods)
			.sorted(comparator)
			.filter(method -> method.getDeclaringClass() == clazz)
			.collect(Collectors.toList());

		for (Method method: defined)
			printMethod(method);

		List<Method> inherited = Stream.of(methods)
			.sorted(comparator)
			.filter(method -> method.getDeclaringClass() != clazz)
			.collect(Collectors.toList());

		for (Method method: inherited)
			printInheritedMethod(method);

		if (methods.length > 0)
			System.out.println();
	}

	private void printConstructors() throws ClassNotFoundException {
		Constructor<?>[] constructors = clazz.getConstructors();
		for (Constructor<?> constructor: constructors)
			printConstructor(constructor);

		if (constructors.length > 0)
			System.out.println();
	}

	private void printConstructor(Constructor<?> constructor) {
		Class<?> clazz = constructor.getDeclaringClass();
		System.out.println(AbstractCommand.ANSI_RESET + "  "
			+ annotationsAsString(constructor)
			+ constructor.toString().replace(clazz.getName() + "(", clazz.getSimpleName() + "(")
			+ (whiteListingWizard.whiteListingModelOf(constructor).isEmpty() ? (AbstractCommand.ANSI_RED + " \u274c") : ""));
	}

	private String annotationsAsString(Executable executable) {
		String prefix = Constants.IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME;
		String result = Stream.of(executable.getAnnotations())
			.filter(annotation -> annotation.annotationType().getName().startsWith(prefix))
			.map(Annotation::toString)
			.collect(Collectors.joining(" "))
			.replace(prefix, "")
			.replace("()", "")
			.replace("(Contract.class)", "");

		if (result.isEmpty())
			return "";
		else
			return AbstractCommand.ANSI_RED + result + AbstractCommand.ANSI_RESET + ' ';
	}

	private void printMethod(Method method) {
		System.out.println(AbstractCommand.ANSI_RESET + "  "
			+ annotationsAsString(method)
			+ method.toString().replace(method.getDeclaringClass().getName() + "." + method.getName(), method.getName())
			+ (whiteListingWizard.whiteListingModelOf(method).isEmpty() ? (AbstractCommand.ANSI_RED + " \u274c") : ""));
	}

	private void printInheritedMethod(Method method) {
		Class<?> definingClass = method.getDeclaringClass();
		System.out.println(AbstractCommand.ANSI_CYAN + "\u25b2 "
			+ annotationsAsString(method) + AbstractCommand.ANSI_CYAN
			+ method.toString().replace(method.getDeclaringClass().getName() + "." + method.getName(), method.getName())
			+ AbstractCommand.ANSI_GREEN + " (inherited from " + definingClass.getName() + ")"
			+ (whiteListingWizard.whiteListingModelOf(method).isEmpty() ? (AbstractCommand.ANSI_RED + " \u274c") : ""));
	}
}