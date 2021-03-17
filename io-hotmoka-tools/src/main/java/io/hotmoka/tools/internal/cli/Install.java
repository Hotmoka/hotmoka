package io.hotmoka.tools.internal.cli;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.stream.Stream;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.nodes.GasHelper;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.NonceHelper;
import io.hotmoka.remote.RemoteNode;
import io.hotmoka.remote.RemoteNodeConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "install",
	description = "Installs a jar in a node",
	showDefaultValues = true)
public class Install extends AbstractCommand {

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Parameters(description = "the jar to install")
	private Path jar;

	@Parameters(description = "the reference to the account that pays for the installation")
    private String payer;

	@Option(names = { "--libs" }, description = "the references of the dependencies of the jar, already installed in the node (takamakaCode is automatically added)")
	private List<String> libs;

	@Option(names = "classpath", description = "the classpath used to interpret the payer", defaultValue = "takamakaCode")
    private String classpath;

	@Option(names = { "--non-interactive" }, description = "runs in non-interactive mode") 
	private boolean nonInteractive;

	@Option(names = { "--gas-limit" }, description = "the gas limit used for the installation", defaultValue = "heuristic") 
	private String gasLimit;

	@Override
	public void run() {
		RemoteNodeConfig remoteNodeConfig = new RemoteNodeConfig.Builder().setURL(url).build();

		try (Node node = RemoteNode.of(remoteNodeConfig)) {
			TransactionReference takamakaCode = node.getTakamakaCode();
			StorageReference manifest = node.getManifest();
			StorageReference payer = new StorageReference(this.payer);
			String chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
			GasHelper gasHelper = new GasHelper(node);
			NonceHelper nonceHelper = new NonceHelper(node);
			byte[] bytes = Files.readAllBytes(jar);
			KeyPair keys = readKeys(payer);
			TransactionReference[] dependencies;
			if (libs != null)
				dependencies = Stream.concat(libs.stream().map(LocalTransactionReference::new), Stream.of(takamakaCode))
					.distinct().toArray(TransactionReference[]::new);
			else
				dependencies = new TransactionReference[] { takamakaCode };

			BigInteger gas = "heuristic".equals(gasLimit) ? _10_000.add(BigInteger.valueOf(4).multiply(BigInteger.valueOf(bytes.length))) : new BigInteger(gasLimit);
			TransactionReference classpath = "takamakaCode".equals(this.classpath) ? takamakaCode : new LocalTransactionReference(this.classpath);

			askForConfirmation(gas);

			TransactionReference response = node.addJarStoreTransaction(new JarStoreTransactionRequest(
				Signer.with(node.getSignatureAlgorithmForRequests(), keys),
				payer,
				nonceHelper.getNonceOf(payer),
				chainId,
				gas,
				gasHelper.getSafeGasPrice(),
				classpath,
				bytes,
				dependencies));

			System.out.println(jar + " has been installed at " + response);
		}
		catch (Exception e) {
			throw new CommandException(e);
		}
	}

	private void askForConfirmation(BigInteger gas) {
		if (!nonInteractive) {
			System.out.print("Do you really want to spend up to " + gas + " gas units to install the jar [Y/N] ");
			String answer = System.console().readLine();
			if (!"Y".equals(answer))
				System.exit(0);
		}
	}
}