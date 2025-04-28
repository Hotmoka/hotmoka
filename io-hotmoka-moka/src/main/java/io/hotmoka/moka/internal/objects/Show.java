/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.moka.internal.objects;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.helpers.ClassLoaderHelpers;
import io.hotmoka.moka.api.accounts.AccountsShowOutput;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.converters.StorageReferenceOptionConverter;
import io.hotmoka.moka.internal.json.AccountsShowOutputJson;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.updates.UpdateOfString;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.whitelisting.api.WhiteListingWizard;
import io.takamaka.code.constants.Constants;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "show", description = "Show the state of a storage object.")
public class Show extends AbstractMokaRpcCommand {

	@Parameters(index = "0", description = "the storage reference of the object", converter = StorageReferenceOptionConverter.class)
    private StorageReference object;

	@Option(names = "--api", description = "print the public API of the object")
    private boolean api;

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		new Run(remote);
	}

	private class Run {
		private final Update[] updates;
		private final ClassTag tag;

		private Run(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			try {
				this.updates = remote.getState(object).sorted().toArray(Update[]::new);
			}
			catch (UnknownReferenceException e) {
				throw new CommandException("The object " + object + " cannot be found in the store of the node.");
			}

			this.tag = getClassTag();

			printHeader();
			printFieldsInClass();
			printFieldsInherited();
			printAPI(remote);
		}

		private ClassTag getClassTag() throws CommandException {
			return Stream.of(updates)
					.filter(update -> update instanceof ClassTag)
					.map(update -> (ClassTag) update)
					.findFirst().orElseThrow(() -> new CommandException("Object " + object + " has no class tag in the store of the node!"));
		}

		private void printHeader() {
			System.out.println(green("class " + tag.getClazz() + " (from jar installed at " + tag.getJar() + ")"));
		}

		private void printFieldsInClass() {
			Stream.of(updates)
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> update.getField().getDefiningClass().equals(tag.getClazz()))
				.forEachOrdered(this::printUpdateInClass);
		}

		private void printFieldsInherited() {
			Stream.of(updates)
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> !update.getField().getDefiningClass().equals(tag.getClazz()))
				.forEachOrdered(this::printUpdateInherited);
		}

		private void printUpdateInClass(UpdateOfField update) {
			FieldSignature field = update.getField();
			System.out.println("  " + field.getName() + ":" + field.getType() + " = " + valueToPrint(update));
		}

		private void printUpdateInherited(UpdateOfField update) {
			FieldSignature field = update.getField();
			System.out.println("  " + field.getName() + ":" + field.getType() + " = " + valueToPrint(update) + green(" (inherited from " + field.getDefiningClass() + ")"));
		}

		private String valueToPrint(UpdateOfField update) {
			if (update instanceof UpdateOfString)
				return cyan('\"' + update.getValue().toString() + '\"');
			else
				return cyan(update.getValue().toString());
		}

		private void printAPI(Node node) throws NodeException, TimeoutException, InterruptedException, CommandException {
			if (api) {
				System.out.println();
				new PrintAPI(node);
			}
		}

		private class PrintAPI {
			private final Class<?> clazz;
			private final WhiteListingWizard whiteListingWizard;

			private PrintAPI(Node node) throws NodeException, TimeoutException, InterruptedException, CommandException {
				TakamakaClassLoader classLoader;

				try {
					classLoader = ClassLoaderHelpers.of(node).classloaderFor(tag.getJar());
				}
				catch (UnknownReferenceException e) {
					throw new CommandException("The object " + object + " has been installed by transaction " + tag.getJar() + " but the latter cannot be found in the node!");
				}

				try {
					this.clazz = classLoader.loadClass(tag.getClazz().getName());
				}
				catch (ClassNotFoundException e) {
					throw new CommandException("Cannot find the class of object " + object + " although it is in store!");
				}

				this.whiteListingWizard = classLoader.getWhiteListingWizard();

				printConstructors();
				printMethods();
			}

			private void printConstructors() {
				Constructor<?>[] constructors = clazz.getConstructors();

				Stream.of(constructors)
					.filter(constructor -> whiteListingWizard.whiteListingModelOf(constructor).isPresent())
					.forEachOrdered(this::printConstructor);
			
				if (constructors.length > 0)
					System.out.println();
			}

			private void printMethods() {
				Comparator<Method> comparator = Comparator.comparing(Method::getName)
					.thenComparing(Method::toString);

				Method[] methods = clazz.getMethods();

				Stream.of(methods)
					.sorted(comparator)
					.filter(method -> method.getDeclaringClass() == clazz)
					.filter(method -> whiteListingWizard.whiteListingModelOf(method).isPresent())
					.forEachOrdered(this::printMethod);

				Stream.of(methods)
					.sorted(comparator)
					.filter(method -> method.getDeclaringClass() != clazz)
					.filter(method -> whiteListingWizard.whiteListingModelOf(method).isPresent())
					.forEachOrdered(this::printInheritedMethod);
			}

			private void printConstructor(Constructor<?> constructor) {
				System.out.println("  " + annotationsAsString(constructor) + constructor.toString().replace(clazz.getName() + "(", clazz.getSimpleName() + "("));
			}

			private void printMethod(Method method) {
				System.out.println("  " + annotationsAsString(method) + method.toString().replace(method.getDeclaringClass().getName() + "." + method.getName(), method.getName()));
			}

			private void printInheritedMethod(Method method) {
				Class<?> definingClass = method.getDeclaringClass();
				System.out.println("  " + annotationsAsString(method) + method.toString().replace(method.getDeclaringClass().getName() + "." + method.getName(), method.getName())
						+ green(" (inherited from " + definingClass.getName() + ")"));
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

				return result.isEmpty() ? "" : (red(result) + ' ');
			}
		}
	}

	/**
	 * The output of this command.
	 */
	@Immutable
	public static class Output implements AccountsShowOutput {
		private final BigInteger balance;
		private final SignatureAlgorithm signature;
		private final String publicKeyBase58;
		private final String publicKeyBase64;

		/**
		 * The maximal length for the printed keys. After this length, the printout of the key gets truncated.
		 */
		public final static int MAX_PRINTED_KEY = 200;

		private Output(BigInteger balance, SignatureAlgorithm signature, String publicKeyBase64) throws Base64ConversionException {
			this.balance = balance;
			this.signature = signature;
			this.publicKeyBase64 = publicKeyBase64;
			this.publicKeyBase58 = Base58.toBase58String(Base64.fromBase64String(publicKeyBase64));
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 * @throws NoSuchAlgorithmException if {@code json} refers to a non-available cryptographic algorithm
		 */
		public Output(AccountsShowOutputJson json) throws InconsistentJsonException, NoSuchAlgorithmException {
			ExceptionSupplier<InconsistentJsonException> exp = InconsistentJsonException::new;

			this.balance = Objects.requireNonNull(json.getBalance(), "balance cannot be null", exp);
			if (balance.signum() < 0)
				throw new InconsistentJsonException("The balance of the account cannot be negative");

			this.signature = SignatureAlgorithms.of(Objects.requireNonNull(json.getSignature(), "signature cannot be null", exp));
			this.publicKeyBase58 = Base58.requireBase58(Objects.requireNonNull(json.getPublicKeyBase58(), "publicKeyBase58 cannot be null", exp), exp);
			this.publicKeyBase64 = Base64.requireBase64(Objects.requireNonNull(json.getPublicKeyBase64(), "publicKeyBase64 cannot be null", exp), exp);
		}

		@Override
		public BigInteger getBalance() {
			return balance;
		}

		@Override
		public SignatureAlgorithm getSignature() {
			return signature;
		}

		@Override
		public String getPublicKeyBase58() {
			return publicKeyBase58;
		}

		@Override
		public String getPublicKeyBase64() {
			return publicKeyBase64;
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();
	
			sb.append("* balance: " + balance + "\n");

			if (publicKeyBase58.length() > MAX_PRINTED_KEY)
				sb.append("* public key: " + publicKeyBase58.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base58)\n");
			else
				sb.append("* public key: " + publicKeyBase58 + " (" + signature + ", base58)\n");

			if (publicKeyBase64.length() > MAX_PRINTED_KEY)
				sb.append("* public key: " + publicKeyBase64.substring(0, MAX_PRINTED_KEY) + "..." + " (" + signature + ", base64)\n");
			else
				sb.append("* public key: " + publicKeyBase64 + " (" + signature + ", base64)\n");

			return sb.toString();
		}
	}
}