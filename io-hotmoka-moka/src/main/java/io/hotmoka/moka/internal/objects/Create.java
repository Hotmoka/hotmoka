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

package io.hotmoka.moka.internal.objects;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.ClassLoaderHelpers;
import io.hotmoka.moka.ObjectsCreateOutputs;
import io.hotmoka.moka.api.GasCost;
import io.hotmoka.moka.api.objects.ObjectsCreateOutput;
import io.hotmoka.moka.internal.AbstractGasCostCommand;
import io.hotmoka.moka.internal.converters.StorageReferenceOfAccountOptionConverter;
import io.hotmoka.moka.internal.converters.TransactionReferenceOptionConverter;
import io.hotmoka.moka.internal.json.ObjectsCreateOutputJson;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.signatures.CodeSignature;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.whitelisting.api.WhiteListingWizard;
import io.takamaka.code.constants.Constants;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create",
	description = "Create a storage object.",
	showDefaultValues = true)
public class Create extends AbstractGasCostCommand {

	@Parameters(index = "0", description = "the name of the class whose constructor gets called")
    private String className;

	@Parameters(description = "the account that pays for the creation of the object", converter = StorageReferenceOfAccountOptionConverter.class)
	private StorageReference payer;

	@Parameters(index ="2..*", description = "the actual arguments passed to the constructor; use storage references for passing objects")
    private List<String> args;

	@Option(names = "--selection", description = "the number (1 or greater) of the constructor to call if there are many constructors with the given number of arguments; if missing, the constructor to call will be selected interactively")
	private Integer selection;

	@Option(names = "--dir", paramLabel = "<path>", description = "the directory where the key pair of the payer can be found", defaultValue = "")
	private Path dir;

	@Option(names = "--password-of-payer", description = "the password of the key pair of the payer account", interactive = true, defaultValue = "")
    private char[] passwordOfPayer;

	@Option(names = "--classpath", paramLabel = "<transaction reference>", description = "the classpath used to interpret payer, class name and arguments; if missing, the reference to the transaction that created the payer account will be used", converter = TransactionReferenceOptionConverter.class)
    private TransactionReference classpath;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		new Body(remote);
	}

	private class Body {
		private final Class<?> clazz;
		private final WhiteListingWizard whiteListingWizard;
		private final Constructor<?> constructor;
		private final TransactionReference classpath;
		private final TakamakaClassLoader classloader;
		private final BigInteger gasPrice;
		private final Signer<SignedTransactionRequest<?>> signer;
		private final ConstructorSignature signatureOfConstructor;
		private final BigInteger nonce;
		private final ConstructorCallTransactionRequest request;
		private final String chainId;
		private final BigInteger gasLimit;
		private final RemoteNode remote;

		private Body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			String passwordOfPayerAsString = new String(passwordOfPayer);

			try {
				this.remote = remote;
				this.chainId = remote.getConfig().getChainId();
				var signatureOfPayer = determineSignatureOf(payer, remote);
				this.signer = mkSigner(payer, dir, signatureOfPayer, passwordOfPayerAsString);
				this.gasLimit = determineGasLimit(() -> gasLimitHeuristic(signatureOfPayer));
				this.classpath = mkClasspath();
				this.classloader = mkClassloader();
				this.clazz = mkClass();
				this.whiteListingWizard = classloader.getWhiteListingWizard();
				this.constructor = identifyConstructor();
				this.signatureOfConstructor = mkConstructor();
				this.gasPrice = determineGasPrice(remote);
				askForConfirmation("call constructor " + constructor, gasLimit, gasPrice, yes || json());
				this.nonce = determineNonceOf(payer, remote);
				this.request = mkRequest();
				report(json(), executeRequest(), ObjectsCreateOutputs.Encoder::new);
			}
			finally {
				passwordOfPayerAsString = null;
				Arrays.fill(passwordOfPayer, ' ');
			}
		}

		private Class<?> mkClass() throws CommandException {
			try {
				return classloader.loadClass(className);
			}
			catch (ClassNotFoundException e) {
				throw new CommandException("Class " + className + " cannot be found; did you correctly specify the classpath with --classpath?");
			}
		}

		private ConstructorCallTransactionRequest mkRequest() throws CommandException {
			try {
				return TransactionRequests.constructorCall(
						signer,
						payer,
						nonce,
						chainId,
						gasLimit,
						gasPrice,
						classpath,
						signatureOfConstructor,
						actualsAsStorageValues(signatureOfConstructor));
			}
			catch (InvalidKeyException | SignatureException e) {
				throw new CommandException("The key pair of " + payer + " seems corrupted!", e);
			}
		}

		private Output executeRequest() throws CommandException, NodeException, TimeoutException, InterruptedException {
			TransactionReference transaction = computeTransaction(request);
			Optional<StorageReference> object = Optional.empty();
			Optional<GasCost> gasCost = Optional.empty();
			Optional<String> errorMessage = Optional.empty();

			try {
				if (post()) {
					if (!json())
						System.out.print("Posting transaction " + asTransactionReference(transaction) + "... ");

					remote.postConstructorCallTransaction(request);

					if (!json())
						System.out.println("done.");
				}
				else {
					if (!json())
						System.out.print("Adding transaction " + asTransactionReference(transaction) + "... ");

					try {
						object = Optional.of(remote.addConstructorCallTransaction(request));
						if (!json())
							System.out.println("done.");
					}
					catch (TransactionException | CodeExecutionException e) {
						if (!json())
							System.out.println("failed.");

						errorMessage = Optional.of(e.getMessage());
					}

					gasCost = Optional.of(computeIncurredGasCost(remote, gasPrice, transaction));
				}
			}
			catch (TransactionRejectedException e) {
				throw new CommandException("Transaction " + transaction + " has been rejected!", e);
			}

			return new Output(transaction, object, gasCost, errorMessage);
		}

		private TakamakaClassLoader mkClassloader() throws NodeException, TimeoutException, InterruptedException, CommandException {
			try {
				return ClassLoaderHelpers.of(remote).classloaderFor(classpath);
			}
			catch (UnknownReferenceException e) {
				if (classpath != null)
					throw new CommandException("The classpath " + classpath + " does not match any transaction in the store of the node");
				else
					throw new CommandException(payer + " exists in the store of the node but its creation transaction cannot be found");
			}
		}

		private TransactionReference mkClasspath() throws CommandException, NodeException, TimeoutException, InterruptedException {
			return classpath != null ? classpath : getClasspathAtCreationTimeOf(payer, remote);
		}

		private BigInteger gasLimitHeuristic(SignatureAlgorithm signatureOfPayer) {
			return _100_000.add(gasForTransactionWhosePayerHasSignature(signatureOfPayer));
		}

		private StorageValue[] actualsAsStorageValues(CodeSignature signature) {
			var formals = signature.getFormals().toArray(StorageType[]::new);
			var result = new StorageValue[formals.length];

			if (result.length > 0) {
				int pos = 0;
				for (String actualAsString: args)
					result[pos] = StorageValues.of(actualAsString, formals[pos++]);
			}

			return result;
		}

		private ConstructorSignature mkConstructor() throws CommandException {
			try {
				Parameter[] parameters = constructor.getParameters();
				var formals = new StorageType[parameters.length];
				int pos = 0;
				for (var parameter: parameters)
					formals[pos++] = StorageTypes.fromClass(parameter.getType());

				return ConstructorSignatures.of(StorageTypes.classNamed(className), formals);
			}
			catch (IllegalArgumentException e) {
				throw new CommandException("Cannot build the signature of the constructor", e);
			}
		}

		private Constructor<?> identifyConstructor() throws CommandException {
			int argCount = args == null ? 0 : args.size();

			// by using a comparator we fix the ordering, which is relevant if --selection is used
			Comparator<Constructor<?>> comparator = Comparator.comparing(Constructor::toString);

			Constructor<?>[] alternatives = Stream.of(clazz.getConstructors())
				.filter(constructor -> constructor.getParameterCount() == argCount)
				.filter(constructor -> whiteListingWizard.whiteListingModelOf(constructor).isPresent())
				.sorted(comparator)
				.toArray(Constructor<?>[]::new);

			int alternativesCount = alternatives.length;

			if (alternativesCount == 0)
				throw new CommandException("Cannot find any constructor with " + argCount + " formal arguments in class " + className);
			else if (selection != null && selection >= 1 && selection <= alternativesCount)
				return alternatives[selection - 1];
			else if (selection != null)
				throw new CommandException("--selection " + selection + " has been specified, but alternatives are between 1 and " + alternativesCount);
			else if (alternativesCount == 1)
				return alternatives[0];
			else if (json())
				throw new CommandException("There are " + alternativesCount + " constructors with " + argCount + " formal arguments: use --selection to specify which one must be called");
			else {
				System.out.println(asInteraction("Which constructor do you want to call [" + 1 + "-" + alternativesCount + "] ?"));

				int pos = 1;
				for (Constructor<?> constructor: alternatives)
					System.out.printf("%2d) %s\n", pos++, toString(constructor));

				while (true) {
					try {
						String answer = System.console().readLine();
						int num = Integer.parseInt(answer);
						if (num >= 1 && num <= alternativesCount)
							return alternatives[num - 1];
					}
					catch (NumberFormatException e) {
					}

					System.out.println("Answer with a number between 1 and " + alternativesCount + " included");
				}
			}
		}

		private String annotationsAsString(Executable executable) {
			String prefix = Constants.IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME;
			return Stream.of(executable.getAnnotations())
				.filter(annotation -> annotation.annotationType().getName().startsWith(prefix))
				.map(Annotation::toString)
				.collect(Collectors.joining(" "))
				.replace(prefix, "")
				.replace("()", "")
				.replace("(Contract.class)", "");
		}

		private String toString(Constructor<?> constructor) {
			String annotations = annotationsAsString(constructor);
			if (annotations.isEmpty())
				return constructor.toString();
			else
				return red(annotations) + " " + constructor;
		}
	}

	/**
	 * The output of this command.
	 */
	@Immutable
	public static class Output extends AbstractGasCostCommandOutput implements ObjectsCreateOutput {

		/**
		 * The object that has been created, if any.
		 */
		private final Optional<StorageReference> object;

		/**
		 * Builds the output of the command.
		 * 
		 * @param transaction the creation transaction
		 * @param object the object that has been created, if any
		 * @param gasCost the gas cost of the transaction, if any
		 * @param errorMessage the error message of the transaction, if any
		 */
		private Output(TransactionReference transaction, Optional<StorageReference> object, Optional<GasCost> gasCost, Optional<String> errorMessage) {
			super(transaction, gasCost, errorMessage);

			this.object = object;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(ObjectsCreateOutputJson json) throws InconsistentJsonException {
			super(json);

			var object = json.getObject();
			if (object.isEmpty())
				this.object = Optional.empty();
			else
				this.object = Optional.of(object.get().unmap().asReference(value -> new InconsistentJsonException("The reference to the created object must be a storage reference, not a " + value.getClass().getName())));
		}

		@Override
		public Optional<StorageReference> getObject() {
			return object;
		}

		@Override
		protected void toString(StringBuilder sb) {
			object.ifPresent(o -> sb.append("A new object " + o + " has been created.\n"));
		}
	}
}