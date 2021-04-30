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

package io.hotmoka.tools.internal.cli;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests;
import io.hotmoka.nodes.GasHelper;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.NonceHelper;
import io.hotmoka.remote.RemoteNode;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.whitelisting.WhiteListingWizard;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create",
	description = "Creates an object in the store of a node",
	showDefaultValues = true)
public class Create extends AbstractCommand {

	@Parameters(index = "0", description = "the reference to the account that pays for the creation")
    private String payer;

	@Parameters(index = "1", description = "the name of the class that gets instantiated")
    private String className;

	@Parameters(index ="2..*", description = "the actual arguments passed to the constructor")
    private List<String> args;

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Option(names = "--classpath", description = "the classpath used to interpret arguments, payer and className", defaultValue = "takamakaCode")
    private String classpath;

	@Option(names = { "--non-interactive" }, description = "runs in non-interactive mode") 
	private boolean nonInteractive;

	@Option(names = { "--gas-limit" }, description = "the gas limit used for the call", defaultValue = "500000") 
	private BigInteger gasLimit;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Class<?> clazz;
		private final WhiteListingWizard whiteListingWizard;
		private final Constructor<?> constructor;

		private Run() throws Exception {
			try (Node node = RemoteNode.of(remoteNodeConfig(url))) {
				TransactionReference takamakaCode = node.getTakamakaCode();
				StorageReference manifest = node.getManifest();
				StorageReference payer = new StorageReference(Create.this.payer);
				String chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
				GasHelper gasHelper = new GasHelper(node);
				NonceHelper nonceHelper = new NonceHelper(node);
				KeyPair keys = readKeys(payer);

				TransactionReference classpath = "takamakaCode".equals(Create.this.classpath) ? takamakaCode : new LocalTransactionReference(Create.this.classpath);
				TakamakaClassLoader classloader = new ClassLoaderHelper(node).classloaderFor(classpath);
				this.clazz = classloader.loadClass(className);
				this.whiteListingWizard = classloader.getWhiteListingWizard();
				this.constructor = askForConstructor();
				askForConfirmation();
				ConstructorSignature signatureOfConstructor = signatureOfConstructor();
				SignatureAlgorithm<SignedTransactionRequest> signature = SignatureAlgorithmForTransactionRequests.mk(node.getNameOfSignatureAlgorithmForRequests());

				ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest(
						Signer.with(signature, keys),
						payer,
						nonceHelper.getNonceOf(payer),
						chainId,
						gasLimit,
						gasHelper.getGasPrice(),
						classpath,
						signatureOfConstructor,
						actualsAsStorageValues(signatureOfConstructor));

				try {
					StorageReference object = node.addConstructorCallTransaction(request);
					System.out.println("the new object has been allocated at " + object);
				}
				finally {
					printCosts(node, request);
				}
			}
		}

		private StorageValue[] actualsAsStorageValues(CodeSignature signature) {
			StorageType[] formals = signature.formals().toArray(StorageType[]::new);
			StorageValue[] result = new StorageValue[formals.length];

			if (result.length > 0) {
				int pos = 0;
				for (String actualAsString: args)
					result[pos] = StorageValue.of(actualAsString, formals[pos++]);
			}

			return result;
		}

		private ConstructorSignature signatureOfConstructor() {
			StorageType[] formals = Stream.of(constructor.getParameters())
				.map(Parameter::getType)
				.map(StorageType::of)
				.toArray(StorageType[]::new);

			return new ConstructorSignature(className, formals);
		}

		private Constructor<?> askForConstructor() throws ClassNotFoundException {
			int argCount = args == null ? 0 : args.size();
			Constructor<?>[] alternatives = Stream.of(clazz.getConstructors())
				.filter(constructor -> constructor.getParameterCount() == argCount)
				.toArray(Constructor<?>[]::new);

			if (alternatives.length == 0)
				throw new CommandException("cannot find any constructor with " + argCount + " formal arguments in class " + className);

			if (alternatives.length == 1)
				return alternatives[0];

			System.out.println("which constructor do you want to call?");
			int pos = 1;
			for (Constructor<?> constructor: alternatives) {
				System.out.printf(AbstractCommand.ANSI_RESET + "%2d) ", pos++);
				printConstructor(constructor);
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

				System.out.println("the answer must be between 1 and " + alternatives.length);
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

		private void printConstructor(Constructor<?> constructor) throws ClassNotFoundException {
			System.out.print(constructorAsString(constructor));
		}

		private String constructorAsString(Constructor<?> constructor) {
			Class<?> clazz = constructor.getDeclaringClass();
			return annotationsAsString(constructor) + AbstractCommand.ANSI_GREEN
				+ constructor.toString().replace(clazz.getName() + "(", clazz.getSimpleName() + "(")
				+ (whiteListingWizard.whiteListingModelOf(constructor).isEmpty() ? (AbstractCommand.ANSI_RED + " \u274c" + AbstractCommand.ANSI_RESET) : AbstractCommand.ANSI_RESET);
		}

		private void askForConfirmation() throws ClassNotFoundException {
			if (!nonInteractive)
				yesNo("Do you really want to spend up to " + gasLimit + " gas units to call " + constructorAsString(constructor) + " ? [Y/N] ");
		}
	}
}