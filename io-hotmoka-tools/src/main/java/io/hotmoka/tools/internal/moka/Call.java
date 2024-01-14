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
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.MethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.constants.Constants;
import io.hotmoka.helpers.ClassLoaderHelpers;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.remote.RemoteNodes;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.whitelisting.api.WhiteListingWizard;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "call",
	description = "Call a method of an object or class",
	showDefaultValues = true)
public class Call extends AbstractCommand {

	@Parameters(index = "0", description = "the receiver of the call (class name or object reference)")
    private String receiver;

	@Parameters(index = "1", description = "the name of the method to call")
    private String methodName;

	@Parameters(index ="2..*", description = "the actual arguments passed to the method")
    private List<String> args;

	@Option(names = { "--payer" }, description = "the reference to the account that pays for the call and becomes caller inside the method; it can be left blank for @View calls, in which case the manifest is used as caller")
	private String payer;

	@Option(names = { "--password-of-payer" }, description = "the password of the payer account; if not specified, it will be asked interactively for non-@View calls")
	private String passwordOfPayer;

	@Option(names = { "--class-of-receiver" }, description = "the class of the receiver of the call; this is normally inferred from the run-time type of the receiver but can be specified in case of visibility problems. This is used only for calling instance methods")
    private String classOfReceiver;

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Option(names = "--classpath", description = "the classpath used to interpret arguments, payer and receiver", defaultValue = "the classpath of the receiver")
    private String classpath;

	@Option(names = { "--gas-price" }, description = "the gas price offered for the call", defaultValue = "the current price")
	private String gasPrice;

	@Option(names = { "--gas-limit" }, description = "the gas limit used for the call", defaultValue = "500000") 
	private BigInteger gasLimit;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true") 
	private boolean interactive;

	@Option(names = { "--print-costs" }, description = "print the incurred gas costs", defaultValue = "true") 
	private boolean printCosts;

	@Option(names = { "--use-colors" }, description = "use colors in the output", defaultValue = "true") 
	private boolean useColors;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Class<?> clazz;
		private final StorageReference receiver;
		private final WhiteListingWizard whiteListingWizard;
		private final Method method;
		private final boolean isView;
		private final boolean isStatic;
		private final Node node;
		private final StorageReference payer;
		private final TransactionReference classpath;
		private final MethodCallTransactionRequest request;
		private final TakamakaClassLoader classloader;

		private Run() throws Exception {
			try (Node node = this.node = RemoteNodes.of(remoteNodeConfig(url))) {
				if ("the classpath of the receiver".equals(Call.this.classpath))
					this.classpath = node.getClassTag(new StorageReference(Call.this.receiver)).jar;
				else
					this.classpath = new LocalTransactionReference(Call.this.classpath);

				this.classloader = ClassLoaderHelpers.of(node).classloaderFor(classpath);
				this.receiver = computeReceiver();
				this.clazz = getClassOfReceiver();
				this.whiteListingWizard = classloader.getWhiteListingWizard();
				this.method = askForMethod();
				this.isView = methodIsView();
				this.isStatic = methodIsStatic();

				if (!isView) {
					passwordOfPayer = ensurePassword(passwordOfPayer, "the payer account", interactive, false);
					this.payer = new StorageReference(Call.this.payer);
				}
				else
					this.payer = null;

				askForConfirmation();
				this.request = createRequest();

				try {
					callMethod();
				}
				finally {
					if (printCosts)
						if (isView)
							System.out.println("No gas consumed, since the called method is @View");
						else
							printCosts(node, request);
				}
			}
		}

		private boolean methodIsView() {
			return Stream.of(method.getAnnotations())
				.anyMatch(annotation -> annotation.annotationType().getName().equals(Constants.VIEW_NAME));
		}

		private boolean methodIsStatic() {
			try {
				classloader.loadClass(Call.this.receiver);
				return true; // no exception: it looks like the name of a class
			}
			catch (ClassNotFoundException e) {
				return false; // not really a class
			}
		}

		private Class<?> getClassOfReceiver() throws ClassNotFoundException, NoSuchElementException {
			try {
				return classloader.loadClass(Call.this.receiver);
			}
			catch (ClassNotFoundException e) {
				if (classOfReceiver != null)
					return classloader.loadClass(classOfReceiver);
				else
					// receiver is not a class name, let's try as a storage reference
					return classloader.loadClass(node.getClassTag(receiver).clazz.getName());
			}
		}

		private StorageReference computeReceiver() {
			try {
				classloader.loadClass(Call.this.receiver);
				return null;
			}
			catch (ClassNotFoundException e) {
				return new StorageReference(Call.this.receiver);
			}
		}

		private MethodCallTransactionRequest createRequest() throws Exception {
			StorageReference manifest = node.getManifest();
			MethodSignature signatureOfMethod = signatureOfMethod();
			StorageValue[] actuals = actualsAsStorageValues(signatureOfMethod);

			if (isView) {
				StorageReference caller = payer != null ? payer : manifest; // we use the manifest as dummy caller when the payer is not specified;
				if (isStatic)
					return new StaticMethodCallTransactionRequest(
							caller,
							gasLimit,
							classpath,
							signatureOfMethod,
							actuals);
				else
					return new InstanceMethodCallTransactionRequest(
							caller,
							gasLimit,
							classpath,
							signatureOfMethod,
							receiver,
							actuals);
			}
			else {
				KeyPair keys = readKeys(Accounts.of(payer), node, passwordOfPayer);
				String chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, node.getTakamakaCode(), CodeSignature.GET_CHAIN_ID, manifest))).getValue();
				var signature = SignatureHelpers.of(node).signatureAlgorithmFor(payer);
				BigInteger nonce = NonceHelpers.of(node).getNonceOf(payer);
				BigInteger gasPrice = getGasPrice();

				if (isStatic)
					return new StaticMethodCallTransactionRequest(
							signature.getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
							payer,
							nonce,
							chainId,
							gasLimit,
							gasPrice,
							classpath,
							signatureOfMethod,
							actuals);
				else
					return new InstanceMethodCallTransactionRequest(
							signature.getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
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
		}

		private BigInteger getGasPrice() throws Exception {
			if ("the current price".equals(Call.this.gasPrice))
				return GasHelpers.of(node).getGasPrice();
			else {
				BigInteger gasPrice;

				try {
					gasPrice = new BigInteger(Call.this.gasPrice);
				}
				catch (NumberFormatException e) {
					throw new CommandException("The gas price must be a non-negative integer");
				}

				if (gasPrice.signum() < 0)
					throw new CommandException("The gas price must be non-negative");
				
				return gasPrice;
			}
		}

		private void callMethod() throws Exception {
			StorageValue result;

			if (isStatic)
				if (isView)
					result = node.runStaticMethodCallTransaction((StaticMethodCallTransactionRequest) request);
				else
					result = node.addStaticMethodCallTransaction((StaticMethodCallTransactionRequest) request);
			else
				if (isView)
					result = node.runInstanceMethodCallTransaction((InstanceMethodCallTransactionRequest) request);
				else
					result = node.addInstanceMethodCallTransaction((InstanceMethodCallTransactionRequest) request);

			if (method.getReturnType() != void.class)
				if (useColors)
					System.out.println(ANSI_YELLOW + result + ANSI_RESET);
				else
					System.out.println(result);
		}

		private StorageValue[] actualsAsStorageValues(CodeSignature signature) {
			var formals = signature.formals().toArray(StorageType[]::new);
			var result = new StorageValue[formals.length];

			if (result.length > 0) {
				int pos = 0;
				for (String actualAsString: args)
					result[pos] = StorageValues.of(actualAsString, formals[pos++]);
			}

			return result;
		}

		private MethodSignature signatureOfMethod() {
			var formals = Stream.of(method.getParameters())
				.map(Parameter::getType)
				.map(StorageTypes::fromClass)
				.toArray(StorageType[]::new);

			Class<?> returnType = method.getReturnType();
			if (returnType == void.class)
				return new VoidMethodSignature(clazz.getName(), methodName, formals);
			else
				return new NonVoidMethodSignature(clazz.getName(), methodName, StorageTypes.fromClass(returnType), formals);
		}

		private Method askForMethod() throws ClassNotFoundException {
			int argCount = args == null ? 0 : args.size();
			Method[] alternatives = Stream.of(clazz.getMethods())
				.filter(method -> method.getName().equals(methodName) && method.getParameterCount() == argCount)
				.toArray(Method[]::new);

			if (alternatives.length == 0)
				throw new CommandException("Cannot find any method called " + methodName + " and with " + argCount + " formal arguments in class " + clazz.getName());

			if (alternatives.length == 1)
				return alternatives[0];

			System.out.println("Which method do you want to call?");
			int pos = 1;
			for (Method method: alternatives) {
				System.out.printf(AbstractCommand.ANSI_RESET + "%2d) ", pos++);
				printMethod(method);
				System.out.println();
			}

			while (true) {
				try {
					String answer = System.console().readLine();
					int num = Integer.parseInt(answer);
					if (num >= 1 && num <= alternatives.length)
						return alternatives[num - 1];
				}
				catch (NumberFormatException e) {
				}

				System.out.println("The answer must be between 1 and " + alternatives.length);
			}
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

		private void printMethod(Method method) throws ClassNotFoundException {
			System.out.print(methodAsString(method));
		}

		private String methodAsString(Method method) {
			return annotationsAsString(method) + AbstractCommand.ANSI_GREEN
				+ method.toString().replace(method.getDeclaringClass().getName() + "." + method.getName(), method.getName())
				+ (whiteListingWizard.whiteListingModelOf(method).isEmpty() ? (AbstractCommand.ANSI_RED + " \u274c" + AbstractCommand.ANSI_RESET) : AbstractCommand.ANSI_RESET);
		}

		private void askForConfirmation() throws ClassNotFoundException {
			if (interactive && !isView)
				yesNo("Do you really want to spend up to " + gasLimit + " gas units to call " + methodAsString(method) + " ? [Y/N] ");
		}
	}
}