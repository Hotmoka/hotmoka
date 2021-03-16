package io.hotmoka.tools.internal.cli;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.AbstractJarStoreTransactionRequest;
import io.hotmoka.nodes.Node;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.whitelisting.WhiteListingWizard;

class PrintAPI {
	private final Class<?> clazz;
	private final WhiteListingWizard whiteListingWizard;

	PrintAPI(Node node, TransactionReference jar, String className) throws ClassNotFoundException {
		AbstractJarStoreTransactionRequest request = (AbstractJarStoreTransactionRequest) node.getRequest(jar);
		TakamakaClassLoader classloader = TakamakaClassLoader.of(Stream.of(request.getJar()), 0);
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

	private void printConstructor(Constructor<?> constructor) throws ClassNotFoundException {
		Class<?> clazz = constructor.getDeclaringClass();
		System.out.println(AbstractCommand.ANSI_RESET + "  "
			+ constructor.toString().replace(clazz.getName() + "(", clazz.getSimpleName() + "(")
			+ (whiteListingWizard.whiteListingModelOf(constructor).isEmpty() ? (AbstractCommand.ANSI_RED + " \u274c") : ""));
	}

	private void printMethod(Method method) throws ClassNotFoundException {
		System.out.println(AbstractCommand.ANSI_RESET + "  "
			+ method.toString().replace(method.getDeclaringClass().getName() + "." + method.getName(), method.getName())
			+ (whiteListingWizard.whiteListingModelOf(method).isEmpty() ? (AbstractCommand.ANSI_RED + " \u274c") : ""));
	}

	private void printInheritedMethod(Method method) throws ClassNotFoundException {
		Class<?> definingClass = method.getDeclaringClass();
		System.out.println(AbstractCommand.ANSI_CYAN + "\u25b2 "
			+ method.toString().replace(method.getDeclaringClass().getName() + "." + method.getName(), method.getName())
			+ AbstractCommand.ANSI_GREEN + " (inherited from " + definingClass.getName()
			+ (whiteListingWizard.whiteListingModelOf(method).isEmpty() ? (AbstractCommand.ANSI_RED + " \u274c") : ""));
	}
}