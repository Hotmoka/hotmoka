package io.hotmoka.tools.internal.cli;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.util.List;
import java.util.stream.Stream;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.ByteValue;
import io.hotmoka.beans.values.CharValue;
import io.hotmoka.beans.values.DoubleValue;
import io.hotmoka.beans.values.FloatValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.ShortValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.nodes.GasHelper;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.NonceHelper;
import io.hotmoka.remote.RemoteNode;
import io.hotmoka.remote.RemoteNodeConfig;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.whitelisting.WhiteListingWizard;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "call",
	description = "calls a method of an object or class in the store of a node",
	showDefaultValues = true)
public class Call extends AbstractCommand {

	@Parameters(arity = "1", description = "the reference to the account that pays for the call")
    private String payer;

	@Parameters(arity = "1", description = "the receiver of the call (class name or reference to object)")
    private String receiver;

	@Option(arity ="0..", names = { "--args" }, description = "the actual arguments passed to the method")
    private List<String> args;

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Option(names = "--classpath", description = "the classpath used to interpret arguments, payer and receiver", defaultValue = "takamakaCode")
    private String classpath;

	@Option(names = { "--non-interactive" }, description = "runs in non-interactive mode") 
	private boolean nonInteractive;

	@Option(names = { "--gas-limit" }, description = "the gas limit used for the call", defaultValue = "10000") 
	private BigInteger gasLimit;

	@Override
	public void run() {
		try {
			new Run();
		}
		catch (Exception e) {
			throw new CommandException(e);
		}
	}

	private class Run {
		private final Class<?> clazz;
		private final WhiteListingWizard whiteListingWizard;
		private final Constructor<?> constructor;

		private Run() throws Exception {
			RemoteNodeConfig remoteNodeConfig = new RemoteNodeConfig.Builder().setURL(url).build();

			try (Node node = RemoteNode.of(remoteNodeConfig)) {
				TransactionReference takamakaCode = node.getTakamakaCode();
				StorageReference manifest = node.getManifest();
				StorageReference payer = new StorageReference(Call.this.payer);
				String chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
				GasHelper gasHelper = new GasHelper(node);
				NonceHelper nonceHelper = new NonceHelper(node);
				KeyPair keys = readKeys(payer);

				TransactionReference classpath = "takamakaCode".equals(Call.this.classpath) ? takamakaCode : new LocalTransactionReference(Call.this.classpath);
				TakamakaClassLoader classloader = new ClassLoaderHelper(node).classloaderFor(classpath);
				this.clazz = classloader.loadClass(receiver);
				this.whiteListingWizard = classloader.getWhiteListingWizard();
				this.constructor = askForConstructor();
				askForConfirmation();
				ConstructorSignature signatureOfConstructor = signatureOfConstructor();
				StorageValue[] actuals = actualsAsStorageValues(signatureOfConstructor);

				StorageReference object = node.addConstructorCallTransaction(new ConstructorCallTransactionRequest(
						Signer.with(node.getSignatureAlgorithmForRequests(), keys),
						payer,
						nonceHelper.getNonceOf(payer),
						chainId,
						gasLimit,
						gasHelper.getSafeGasPrice(),
						classpath,
						signatureOfConstructor,
						actuals));

				System.out.println("The new object has been allocated at " + object);
			}
		}

		private StorageValue[] actualsAsStorageValues(ConstructorSignature constructor) {
			StorageType[] formals = constructor.formals().toArray(StorageType[]::new);
			StorageValue[] result = new StorageValue[formals.length];

			int pos = 0;
			for (String actualAsString: args)
				result[pos] = intoStorageValue(actualAsString, formals[pos++]);

			return result;
		}

		private StorageValue intoStorageValue(String actualAsString, StorageType type) {
			if (type instanceof BasicTypes)
				switch ((BasicTypes) type) {
				case BOOLEAN: return new BooleanValue(Boolean.valueOf(actualAsString));
				case BYTE: return new ByteValue(Byte.valueOf(actualAsString));
				case CHAR: {
					if (actualAsString.length() != 1)
						throw new IllegalArgumentException("the value is not a character");
					else
						return new CharValue(Character.valueOf(actualAsString.charAt(0)));
				}
				case SHORT: return new ShortValue(Short.valueOf(actualAsString));
				case INT: return new IntValue(Integer.valueOf(actualAsString));
				case LONG: return new LongValue(Long.valueOf(actualAsString));
				case FLOAT: return new FloatValue(Float.valueOf(actualAsString));
				default: return new DoubleValue(Double.valueOf(actualAsString));
				}
			else
				return new StorageReference(actualAsString);
		}

		private ConstructorSignature signatureOfConstructor() {
			StorageType[] formals = Stream.of(constructor.getParameters())
				.map(Parameter::getType)
				.map(StorageType::of)
				.toArray(StorageType[]::new);

			return new ConstructorSignature(receiver, formals);
		}

		private Constructor<?> askForConstructor() throws ClassNotFoundException {
			int argCount = args == null ? 0 : args.size();
			Constructor<?>[] alternatives = Stream.of(clazz.getConstructors())
				.filter(constructor -> constructor.getParameterCount() == argCount)
				.toArray(Constructor<?>[]::new);

			if (alternatives.length == 0)
				throw new IllegalArgumentException("Cannot find any constructor with " + argCount + " formal arguments in class " + receiver);

			if (alternatives.length == 1)
				return alternatives[0];

			System.out.println("Which constructor do you want to call?");
			int pos = 0;
			for (Constructor<?> constructor: alternatives) {
				System.out.printf(AbstractCommand.ANSI_RED + "%2d) ", pos++);
				printConstructor(constructor);
			}

			while (true) {
				try {
					String answer = System.console().readLine();
					int num = Integer.parseInt(answer);
					if (num >= 0 && num < alternatives.length)
						return alternatives[num];
				}
				catch (NumberFormatException e) {
				}
			}
		}

		private void printConstructor(Constructor<?> constructor) throws ClassNotFoundException {
			Class<?> clazz = constructor.getDeclaringClass();
			System.out.print(AbstractCommand.ANSI_GREEN
				+ constructor.toString().replace(clazz.getName() + "(", clazz.getSimpleName() + "(")
				+ (whiteListingWizard.whiteListingModelOf(constructor).isEmpty() ? (AbstractCommand.ANSI_RED + " \u274c" + AbstractCommand.ANSI_RESET) : AbstractCommand.ANSI_RESET));
		}

		private void askForConfirmation() throws ClassNotFoundException {
			if (!nonInteractive) {
				System.out.print("Do you really want to spend up to " + gasLimit + " gas units to call ");
				printConstructor(constructor);
				System.out.print(" ? [Y/N] ");
				String answer = System.console().readLine();
				if (!"Y".equals(answer))
					System.exit(0);
			}
		}
	}
}