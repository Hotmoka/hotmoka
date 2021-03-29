package io.hotmoka.tools.internal.cli;

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

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.MethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
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

@Command(name = "call",
	description = "Calls a method of an object or class",
	showDefaultValues = true)
public class Call extends AbstractCommand {

	@Parameters(index = "0", description = "the reference to the account that pays for the call")
    private String payer;

	@Parameters(index = "1", description = "the receiver of the call (class name or reference to object)")
    private String receiver;

	@Parameters(index = "2", description = "the name of the method to call")
    private String methodName;

	@Parameters(index ="3..*", description = "the actual arguments passed to the method")
    private List<String> args;

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Option(names = "--classpath", description = "the classpath used to interpret arguments, payer and receiver", defaultValue = "the classpath of the receiver")
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
		private final StorageReference receiver;
		private final WhiteListingWizard whiteListingWizard;
		private final Method method;
		private final boolean isView;
		private final Node node;
		private final StorageReference payer;
		private final TransactionReference classpath;
		private final MethodCallTransactionRequest request;
		private final TakamakaClassLoader classloader;

		private Run() throws Exception {
			try (Node node = this.node = RemoteNode.of(remoteNodeConfig(url))) {
				this.payer = new StorageReference(Call.this.payer);

				if ("the classpath of the receiver".equals(Call.this.classpath)) {
					ClassTag tag = node.getClassTag(new StorageReference(Call.this.receiver));
					this.classpath = tag.jar;
				}
				else
					this.classpath = new LocalTransactionReference(Call.this.classpath);

				System.out.println(this.classpath);
				this.classloader = new ClassLoaderHelper(node).classloaderFor(classpath);
				this.receiver = computeReceiver();
				this.clazz = getClassOfReceiver();
				this.whiteListingWizard = classloader.getWhiteListingWizard();
				this.method = askForMethod();
				this.isView = methodIsView();
				askForConfirmation();
				this.request = createRequest();

				try {
					callMethod();
				}
				finally {
					if (isView)
						System.out.println("calls to @View methods consume no gas");
					else
						printCosts(node, request);
				}
			}
		}

		private boolean methodIsView() {
			return Stream.of(method.getAnnotations())
				.anyMatch(annotation -> annotation.annotationType().getName().equals(Constants.VIEW_NAME));
		}

		private Class<?> getClassOfReceiver() throws ClassNotFoundException, NoSuchElementException {
			try {
				return classloader.loadClass(Call.this.receiver);
			}
			catch (ClassNotFoundException e) {
				// receiver is not a class name, let's try as a storage reference
				return classloader.loadClass(node.getClassTag(receiver).clazz.name);
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
			GasHelper gasHelper = new GasHelper(node);
			NonceHelper nonceHelper = new NonceHelper(node);
			KeyPair keys = readKeys(payer);
			StorageReference manifest = node.getManifest();
			String chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, node.getTakamakaCode(), CodeSignature.GET_CHAIN_ID, manifest))).value;
			MethodSignature signatureOfMethod = signatureOfMethod();
			StorageValue[] actuals = actualsAsStorageValues(signatureOfMethod);

			if (receiver == null)
				return new StaticMethodCallTransactionRequest(
						Signer.with(node.getSignatureAlgorithmForRequests(), keys),
						payer,
						nonceHelper.getNonceOf(payer),
						chainId,
						gasLimit,
						gasHelper.getGasPrice(),
						classpath,
						signatureOfMethod,
						actuals);
			else
				return new InstanceMethodCallTransactionRequest(
						Signer.with(node.getSignatureAlgorithmForRequests(), keys),
						payer,
						nonceHelper.getNonceOf(payer),
						chainId,
						gasLimit,
						gasHelper.getGasPrice(),
						classpath,
						signatureOfMethod,
						receiver,
						actuals);
		}

		private void callMethod() throws Exception {
			StorageValue result;

			if (receiver == null)
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
				System.out.println(ANSI_YELLOW + result + ANSI_RESET);
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

		private MethodSignature signatureOfMethod() {
			StorageType[] formals = Stream.of(method.getParameters())
				.map(Parameter::getType)
				.map(StorageType::of)
				.toArray(StorageType[]::new);

			Class<?> returnType = method.getReturnType();
			if (returnType == void.class)
				return new VoidMethodSignature(clazz.getName(), methodName, formals);
			else
				return new NonVoidMethodSignature(clazz.getName(), methodName, StorageType.of(returnType), formals);
		}

		private Method askForMethod() throws ClassNotFoundException {
			int argCount = args == null ? 0 : args.size();
			Method[] alternatives = Stream.of(clazz.getMethods())
				.filter(method -> method.getName().equals(methodName) && method.getParameterCount() == argCount)
				.toArray(Method[]::new);

			if (alternatives.length == 0)
				throw new CommandException("cannot find any method called " + methodName + " and with " + argCount + " formal arguments in class " + clazz.getName());

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

		private void printMethod(Method method) throws ClassNotFoundException {
			System.out.print(annotationsAsString(method) + AbstractCommand.ANSI_GREEN
				+ method.toString().replace(method.getDeclaringClass().getName() + "." + method.getName(), method.getName())
				+ (whiteListingWizard.whiteListingModelOf(method).isEmpty() ? (AbstractCommand.ANSI_RED + " \u274c" + AbstractCommand.ANSI_RESET) : AbstractCommand.ANSI_RESET));
		}

		private void askForConfirmation() throws ClassNotFoundException {
			if (!nonInteractive && !isView) {
				System.out.print("do you really want to spend up to " + gasLimit + " gas units to call ");
				printMethod(method);
				System.out.print(" ? [Y/N] ");
				String answer = System.console().readLine();
				if (!"Y".equals(answer))
					throw new CommandException("stopped");
			}
		}
	}
}