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

package io.hotmoka.moka.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyPair;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.cli.CommandException;
import io.hotmoka.helpers.ClassLoaderHelpers;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.signatures.CodeSignature;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.remote.RemoteNodes;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.whitelisting.api.WhiteListingWizard;
import io.takamaka.code.constants.Constants;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create",
	description = "Create an object in the store of a node",
	showDefaultValues = true)
public class CreateOld extends AbstractCommand {

	@Parameters(index = "0", description = "the name of the class that gets instantiated")
    private String className;

	@Parameters(index ="1..*", description = "the actual arguments passed to the constructor")
    private List<String> args;

	@Option(names = { "--payer" }, description = "the reference to the account that pays for the call and becomes caller inside the method; it can be left blank for @View calls, in which case the manifest is used as caller")
	private String payer;

	@Option(names = { "--password-of-payer" }, description = "the password of the payer account; if not specified, it will be asked interactively")
	private String passwordOfPayer;

	@Option(names = { "--uri" }, description = "the URI of the node", defaultValue = "ws://localhost:8001")
    private URI uri;

	@Option(names = "--classpath", description = "the classpath used to interpret arguments, payer and className", defaultValue = "takamakaCode")
    private String classpath;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true")
	private boolean interactive;

	@Option(names = { "--gas-limit" }, description = "the gas limit used for the call", defaultValue = "500000") 
	private BigInteger gasLimit;

	@Option(names = { "--print-costs" }, description = "print the incurred gas costs", defaultValue = "true") 
	private boolean printCosts;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Class<?> clazz;
		private final WhiteListingWizard whiteListingWizard;
		private final Constructor<?> constructor;

		private Run() throws Exception {
			passwordOfPayer = ensurePassword(passwordOfPayer, "the payer account", interactive, false);

			try (Node node = RemoteNodes.of(uri, 10_000)) {
				var takamakaCode = node.getTakamakaCode();
				var manifest = node.getManifest();
				var payer = StorageValues.reference(CreateOld.this.payer);
				String chainId = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_CHAIN_ID, manifest))
					.orElseThrow(() -> new CommandException(MethodSignatures.GET_CHAIN_ID + " should not return void"))).getValue();
				var gasHelper = GasHelpers.of(node);
				var nonceHelper = NonceHelpers.of(node);
				KeyPair keys = readKeys(Accounts.of(payer), node, passwordOfPayer);

				TransactionReference classpath = "takamakaCode".equals(CreateOld.this.classpath) ? takamakaCode :
					TransactionReferences.of(CreateOld.this.classpath);
				TakamakaClassLoader classloader = ClassLoaderHelpers.of(node).classloaderFor(classpath);
				this.clazz = classloader.loadClass(className);
				this.whiteListingWizard = classloader.getWhiteListingWizard();
				this.constructor = askForConstructor();
				askForConfirmation();
				ConstructorSignature signatureOfConstructor = signatureOfConstructor();
				var signature = SignatureHelpers.of(node).signatureAlgorithmFor(payer);

				var request = TransactionRequests.constructorCall(
						signature.getSigner(keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
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
					System.out.println("The new object has been allocated at " + object);
				}
				finally {
					if (printCosts)
						printCosts(node, request);
				}
			}
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

		private ConstructorSignature signatureOfConstructor() throws CommandException {
			Parameter[] parameters = constructor.getParameters();
			var formals = new StorageType[parameters.length];
			int pos = 0;
			for (var parameter: parameters)
				formals[pos++] = StorageTypes.fromClass(parameter.getType());

			return ConstructorSignatures.of(StorageTypes.classNamed(className), formals);
		}

		private Constructor<?> askForConstructor() throws ClassNotFoundException, CommandException {
			int argCount = args == null ? 0 : args.size();
			Constructor<?>[] alternatives = Stream.of(clazz.getConstructors())
				.filter(constructor -> constructor.getParameterCount() == argCount)
				.toArray(Constructor<?>[]::new);

			if (alternatives.length == 0)
				throw new CommandException("Cannot find any constructor with " + argCount + " formal arguments in class " + className);

			if (alternatives.length == 1)
				return alternatives[0];

			System.out.println("Which constructor do you want to call?");
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
			if (interactive)
				yesNo("Do you really want to spend up to " + gasLimit + " gas units to call " + constructorAsString(constructor) + " ? [Y/N] ");
		}
	}
}