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
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import io.hotmoka.moka.ObjectsCallOutputs;
import io.hotmoka.moka.api.GasCost;
import io.hotmoka.moka.api.objects.ObjectsCallOutput;
import io.hotmoka.moka.internal.AbstractGasCostCommand;
import io.hotmoka.moka.internal.converters.StorageReferenceOfAccountOptionConverter;
import io.hotmoka.moka.internal.converters.StorageReferenceOptionConverter;
import io.hotmoka.moka.internal.converters.TransactionReferenceOptionConverter;
import io.hotmoka.moka.internal.json.ObjectsCallOutputJson;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.MethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.signatures.CodeSignature;
import io.hotmoka.node.api.signatures.MethodSignature;
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

@Command(name = "call",
	description = "Call a method of an object or class.",
	showDefaultValues = true)
public class Call extends AbstractGasCostCommand {

	@Parameters(index = "0", description = "the account that pays for calling the method (if it is not a @View method) and that can be referenced as caller() inside the method", converter = StorageReferenceOfAccountOptionConverter.class)
	private StorageReference payer;

	@Parameters(index = "1", description = "the name of the class whose method gets called")
    private String className;

	@Parameters(index = "2", description = "the name of the method to call")
    private String methodName;

	@Parameters(index ="3..*", description = "the actual arguments passed to the method")
    private List<String> args;

	@Option(names = "--receiver", paramLabel = "<storage reference>", description = "the receiver of the call; this is not used for calls to static methods", converter = StorageReferenceOptionConverter.class)
    private StorageReference receiver;

	@Option(names = "--selection", description = "the number (1 or greater) of the method to call if there are many methods with the given number of arguments; if missing, the method to call will be selected interactively")
	private Integer selection;

	@Option(names = "--dir", paramLabel = "<path>", description = "the directory where the key pair of the payer can be found", defaultValue = "")
	private Path dir;

	@Option(names = "--password-of-payer", description = "the password of the key pair of the payer account; it is not used for calls to @View methods", interactive = true, defaultValue = "")
    private char[] passwordOfPayer;

	@Option(names = "--classpath", paramLabel = "<transaction reference>", description = "the classpath used to interpret payer, class name, receiver and arguments; if missing, the reference to the transaction that created the receiver account will be used; if the receiver is missing as well, the reference to the transaction that created the payer account will be used", converter = TransactionReferenceOptionConverter.class)
    private TransactionReference classpath;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		new Body(remote);
	}

	private class Body {
		private final RemoteNode remote;
		private final String chainId;
		private final Signer<SignedTransactionRequest<?>> signer;
		private final BigInteger gasLimit;
		private final TransactionReference classpath;
		private final Class<?> clazz;
		private final WhiteListingWizard whiteListingWizard;
		private final Method method;
		private final boolean isView;
		private final boolean isStatic;
		private final MethodCallTransactionRequest request;
		private final TakamakaClassLoader classloader;
		private final MethodSignature signatureOfMethod;
		private final BigInteger gasPrice;
		private final BigInteger nonce;

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
				this.method = identifyMethod();
				this.isView = methodIsView();
				this.isStatic = methodIsStatic();
				this.signatureOfMethod = mkMethod();
				this.gasPrice = determineGasPrice(remote);
				askForConfirmation("call method " + method, gasLimit, gasPrice, yes || json());
				this.nonce = determineNonceOf(payer, remote);
				this.request = mkRequest();
				report(json(), executeRequest(), ObjectsCallOutputs.Encoder::new);
			}
			finally {
				passwordOfPayerAsString = null;
				Arrays.fill(passwordOfPayer, ' ');
			}
		}

		private TransactionReference mkClasspath() throws CommandException, NodeException, TimeoutException, InterruptedException {
			if (Call.this.classpath != null)
				return Call.this.classpath;
			else if (receiver != null)
				return getClasspathAtCreationTimeOf(receiver, remote);
			else
				return getClasspathAtCreationTimeOf(payer, remote);
		}

		private TakamakaClassLoader mkClassloader() throws NodeException, TimeoutException, InterruptedException, CommandException {
			try {
				return ClassLoaderHelpers.of(remote).classloaderFor(classpath);
			}
			catch (UnknownReferenceException e) {
				if (classpath != null)
					throw new CommandException("The classpath " + classpath + " does not match any transaction in the store of the node");
				else if (receiver != null)
					throw new CommandException(receiver + " exists in the store of the node but its creation transaction cannot be found");
				else
					throw new CommandException(payer + " exists in the store of the node but its creation transaction cannot be found");
			}
		}

		private BigInteger gasLimitHeuristic(SignatureAlgorithm signatureOfPayer) {
			return _100_000.add(gasForTransactionWhosePayerHasSignature(signatureOfPayer));
		}

		private boolean methodIsView() {
			return Stream.of(method.getAnnotations())
				.anyMatch(annotation -> annotation.annotationType().getName().equals(Constants.VIEW_NAME));
		}

		private boolean methodIsStatic() {
			return Modifier.isStatic(method.getModifiers());
		}

		private Class<?> mkClass() throws CommandException {
			try {
				return classloader.loadClass(className);
			}
			catch (ClassNotFoundException e) {
				throw new CommandException("Class " + className + " cannot be found; did you correctly specify the classpath with --classpath?");
			}
		}

		private MethodCallTransactionRequest mkRequest() throws CommandException {
			StorageValue[] actuals = actualsAsStorageValues(signatureOfMethod);

			if (isView) {
				if (isStatic)
					return TransactionRequests.staticViewMethodCall(
							payer,
							gasLimit,
							classpath,
							signatureOfMethod,
							actuals);
				else
					return TransactionRequests.instanceViewMethodCall(
							payer,
							gasLimit,
							classpath,
							signatureOfMethod,
							receiver,
							actuals);
			}
			else {
				try {
					if (isStatic)
						return TransactionRequests.staticMethodCall(
								signer,
								payer,
								nonce,
								chainId,
								gasLimit,
								gasPrice,
								classpath,
								signatureOfMethod,
								actuals);
					else
						return TransactionRequests.instanceMethodCall(
								signer,
								payer,
								nonce,
								chainId,
								gasLimit,
								gasPrice,
								classpath,
								signatureOfMethod,
								receiver,
								actuals);
				}
				catch (InvalidKeyException | SignatureException e) {
					throw new CommandException("The key pair of " + payer + " seems corrupted!", e);
				}
			}
		}

		private Output executeRequest() throws CommandException, NodeException, TimeoutException, InterruptedException {
			TransactionReference transaction = computeTransaction(request);
			Optional<StorageValue> result = Optional.empty();
			Optional<GasCost> gasCost = Optional.empty();
			Optional<String> errorMessage = Optional.empty();

			try {
				if (isView) {
					if (!json())
						System.out.print("Running transaction " + asTransactionReference(transaction) + "... ");

					try {
						if (isStatic)
							result = remote.runStaticMethodCallTransaction((StaticMethodCallTransactionRequest) request);
						else
							result = remote.runInstanceMethodCallTransaction((InstanceMethodCallTransactionRequest) request);

						if (!json())
							System.out.println("done.");
					}
					catch (TransactionException | CodeExecutionException e) {
						if (!json())
							System.out.println("failed.");

						errorMessage = Optional.of(e.getMessage());
					}
				}
				else {
					if (post()) {
						if (!json())
							System.out.print("Posting transaction " + asTransactionReference(transaction) + "... ");

						if (isStatic)
							remote.postStaticMethodCallTransaction((StaticMethodCallTransactionRequest) request);
						else
							remote.postInstanceMethodCallTransaction((InstanceMethodCallTransactionRequest) request);

						if (!json())
							System.out.println("done.");
					}
					else {
						if (!json())
							System.out.print("Adding transaction " + asTransactionReference(transaction) + "... ");

						try {
							if (isStatic)
								result = remote.addStaticMethodCallTransaction((StaticMethodCallTransactionRequest) request);
							else
								result = remote.addInstanceMethodCallTransaction((InstanceMethodCallTransactionRequest) request);

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
			}
			catch (TransactionRejectedException e) {
				throw new CommandException("Transaction " + transaction + " has been rejected!", e);
			}

			return new Output(transaction, result, gasCost, errorMessage);
		}

		private StorageValue[] actualsAsStorageValues(CodeSignature signature) throws CommandException {
			try {
				var formals = signature.getFormals().toArray(StorageType[]::new);
				var result = new StorageValue[formals.length];

				if (result.length > 0) {
					int pos = 0;
					for (String actualAsString: args)
						result[pos] = StorageValues.of(actualAsString, formals[pos++]);
				}

				return result;
			}
			catch (IllegalArgumentException e) {
				throw new CommandException("Cannot compute the types of the actual arguments of the " + signature);
			}
		}

		private MethodSignature mkMethod() throws CommandException {
			if (!isStatic && receiver == null)
				throw new CommandException("The target method is an instance method: please specify --receiver");

			try {
				Parameter[] parameters = method.getParameters();
				var formals = new StorageType[parameters.length];
				int pos = 0;
				for (var parameter: parameters)
					formals[pos++] = StorageTypes.fromClass(parameter.getType());

				Class<?> returnType = method.getReturnType();
				if (returnType == void.class)
					return MethodSignatures.ofVoid(StorageTypes.classFromClass(clazz), methodName, formals);
				else
					return MethodSignatures.ofNonVoid(StorageTypes.classFromClass(clazz), methodName, StorageTypes.fromClass(returnType), formals);
			}
			catch (IllegalArgumentException e) {
				throw new CommandException("Cannot build the signature of the method", e);
			}
		}

		private Method identifyMethod() throws CommandException {
			int argCount = args == null ? 0 : args.size();

			// by using a comparator we fix the ordering, which is relevant if --selection is used
			Comparator<Method> comparator = Comparator.comparing(Method::toString);

			Method[] alternatives = Stream.of(clazz.getMethods())
				.filter(method -> method.getName().equals(methodName))
				.filter(method -> method.getParameterCount() == argCount)
				.filter(method -> whiteListingWizard.whiteListingModelOf(method).isPresent())
				.sorted(comparator)
				.toArray(Method[]::new);

			int alternativesCount = alternatives.length;

			if (alternativesCount == 0)
				throw new CommandException("Cannot find any white-listed method with name " + methodName + " and with " + argCount + " formal arguments in class " + className);
			else if (selection != null && selection >= 1 && selection <= alternativesCount)
				return alternatives[selection - 1];
			else if (selection != null)
				throw new CommandException("--selection " + selection + " has been specified, but alternatives are between 1 and " + alternativesCount);
			else if (alternativesCount == 1)
				return alternatives[0];
			else if (json())
				throw new CommandException("There are " + alternativesCount + " white-listed methods in class " + className + " named " + methodName + " and with " + argCount + " formal arguments: use --selection to specify which one must be called");
			else {
				System.out.println(asInteraction("Which method do you want to call [" + 1 + "-" + alternativesCount + "] ?"));

				int pos = 1;
				for (Method method: alternatives)
					System.out.printf("%2d) %s\n", pos++, toString(method));

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

		private String toString(Method method) {
			String annotations = annotationsAsString(method);
			String methodAsString = method.toString().replace(method.getDeclaringClass().getName() + "." + method.getName(), method.getName());
			if (annotations.isEmpty())
				return methodAsString;
			else
				return red(annotations) + " " + methodAsString;
		}
	}

	/**
	 * The output of this command.
	 */
	@Immutable
	public static class Output extends AbstractGasCostCommandOutput implements ObjectsCallOutput {

		/**
		 * The result of the method call, if any.
		 */
		private final Optional<StorageValue> result;

		/**
		 * Builds the output of the command.
		 * 
		 * @param transaction the creation transaction
		 * @param result the result of the method call, if any
		 * @param gasCost the gas cost of the transaction, if any
		 * @param errorMessage the error message of the transaction, if any
		 */
		private Output(TransactionReference transaction, Optional<StorageValue> result, Optional<GasCost> gasCost, Optional<String> errorMessage) {
			super(transaction, gasCost, errorMessage);

			this.result = result;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(ObjectsCallOutputJson json) throws InconsistentJsonException {
			super(json);

			var result = json.getResult();
			if (result.isEmpty())
				this.result = Optional.empty();
			else
				this.result = Optional.of(result.get().unmap());
		}

		@Override
		public Optional<StorageValue> getResult() {
			return result;
		}

		@Override
		protected void toString(StringBuilder sb) {
			result.ifPresent(o -> sb.append("The method returned:\n" + o + "\n"));
		}
	}
}