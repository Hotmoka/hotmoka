/*
Copyright 2023 Fausto Spoto

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

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import io.hotmoka.cli.AbstractRpcCommand;
import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.Entropy;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.helpers.api.MisbehavingNodeException;
import io.hotmoka.helpers.api.UnexpectedCodeException;
import io.hotmoka.moka.api.GasCost;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.Account;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.CodeExecutionTransactionRequest;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.RemoteNodes;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.MappedEncoder;
import io.hotmoka.websockets.beans.api.JsonRepresentation;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;
import jakarta.websocket.EncodeException;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Shared code among the commands that connect to a remote Hotmoka node and perform Rpc calls
 * to the public API of the remote.
 */
public abstract class AbstractMokaRpcCommand extends AbstractRpcCommand<RemoteNode> {
	protected final static BigInteger _500_000 = BigInteger.valueOf(500_000L);

	protected AbstractMokaRpcCommand() {
	}

	@Option(names = "--uri", description = "the network URI where the API of the Hotmoka node service is published", defaultValue = "ws://localhost:8001")
	private URI uri;

	/**
	 * Yields the URI of the public API of the remote service.
	 * 
	 * @return the URI
	 */
	protected final URI uri() {
		return uri;
	}

	@Override
	protected final void execute() throws CommandException {
		execute(RemoteNodes::of, this::bodyWrapper, uri);
	}

	/**
	 * Runs the main body of the command, with a remote connected to the uri of a remote Hotmoka node service,
	 * specified through the {@code --uri} option. It takes care of transforming {@link ClosedNodeException}
	 * and {@link UninitializedNodeException} into {@link CommandException}.
	 * 
	 * @param remote the remote Hotmoka node service
	 * @throws TimeoutException if the execution times out
	 * @throws InterruptedException if the execution gets interrupted before completion
	 * @throws CommandException if something erroneous must be logged and the user must be informed
	 */
	private void bodyWrapper(RemoteNode remote) throws TimeoutException, InterruptedException, CommandException {
		try {
			body(remote);
		}
		catch (ClosedNodeException e) {
			throw new CommandException("The node is already closed!", e);
		}
		catch (UninitializedNodeException e) {
			throw new CommandException("The node is not initialized yet!", e);
		}
		catch (MisbehavingNodeException e) {
			throw new CommandException("The node is misbehaving", e);
		}
		catch (UnexpectedCodeException e) {
			throw new CommandException("The node contains an unexpected Takamaka runtime", e);
		}
	}

	/**
	 * Runs the main body of the command, with a remote connected to the uri of a remote Hotmoka node service,
	 * specified through the {@code --uri} option.
	 * 
	 * @param remote the remote Hotmoka node service
	 * @throws TimeoutException if the execution times out
	 * @throws InterruptedException if the execution gets interrupted before completion
	 * @throws CommandException if something erroneous must be logged and the user must be informed
	 * @throws ClosedNodeException if {@code remote} is already closed
	 * @throws UninitializedNodeException if {@code remote} is not initialized yet
	 * @throws MisbehavingNodeException if {@code remote} is performing in a buggy way
	 * @throws UnexpectedCodeException if {@code remote} contains an unexpected Takamaka runtime
	 */
	protected abstract void body(RemoteNode remote) throws TimeoutException, InterruptedException, CommandException, ClosedNodeException, UninitializedNodeException, MisbehavingNodeException, UnexpectedCodeException;

	/**
	 * Reports on the standard output the given output of a command.
	 * 
	 * @param <O> the type of the output to report
	 * @param json true if and only if the output must be reported in JSON format
	 * @param output the output to report
	 * @param encoder a supplier of a converter of the output into JSON representation; this will
	 *                be used only if {@code json} is true
	 * @throws CommandException if reporting failed
	 */
	protected <O> void report(boolean json, O output, Supplier<MappedEncoder<O, ? extends JsonRepresentation<O>>> encoder) throws CommandException {
		if (json) {
			try {
				System.out.println(encoder.get().encode(output));
			}
			catch (EncodeException e) {
				throw new CommandException("Cannot encode the output of the command in JSON format", e);
			}
		}
		else
			System.out.print(output);
	}

	protected TransactionReference computeTransaction(TransactionRequest<?> request) throws CommandException {
		try {
			Hasher<TransactionRequest<?>> hasher = HashingAlgorithms.sha256().getHasher(TransactionRequest::toByteArray);
			return TransactionReferences.of(hasher.hash(request));
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("The sha256 hashing algorithm is not available");
		}
	}

	protected GasCost computeIncurredGasCost(RemoteNode remote, BigInteger gasPrice, TransactionReference reference) throws CommandException, InterruptedException, ClosedNodeException, TimeoutException {
		try {
			return new GasCostImpl(remote, gasPrice, reference);
		}
		catch (UnknownReferenceException e) {
			throw new CommandException("Cannot find the transaction request in the store of the node, maybe a sudden history change has occurred?", e);
		}
	}

	protected Account mkAccount(StorageReference reference, Path dir) throws CommandException {
		try {
			return Accounts.of(reference, dir);
		}
		catch (IOException e) {
			throw new CommandException("Cannot read the key pair of the " + reference + " : it was expected to be in file \"" + dir.resolve(reference.toString()) + ".pem\"", e);
		}
	}

	/**
	 * Yields the signer of a transaction paid by a given account, whose key pair can be found
	 * in the given directory, uses the given signature algorithm and is unlocked by the given password.
	 * 
	 * @param account the storage reference of the account
	 * @param dir the directory where the key pair of {@code account} can be found
	 * @param signature the signature algorithm used by {@code account}
	 * @param password the password of the key pair of {@code account}
	 * @return the signer
	 * @throws CommandException if the signer cannot be created
	 */
	protected Signer<SignedTransactionRequest<?>> mkSigner(StorageReference account, Path dir, SignatureAlgorithm signature, String password) throws CommandException {
		return signature.getSigner(mkAccount(account, dir).keys(password, signature).getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);
	}

	/**
	 * Yields the reference to the transaction that created the given object in store.
	 * 
	 * @param object the object
	 * @param node the node where the object should be stored
	 * @return the reference to the transaction that created {@code object}
	 * @throws ClosedNodeException if the node is already closed
	 * @throws TimeoutException if no answer arrives by a given time window
	 * @throws InterruptedException if the operation gets interrupted before completion
	 * @throws CommandException if {@code object} does not exist in store, or if it has not been created with a transaction that creates object, in which case the remote node is corrupted
	 */
	// TODO: add a component to the ClassTag of the object, so that we do not need to look for the classpath of the creation transaction of the objects
	protected TransactionReference getClasspathAtCreationTimeOf(StorageReference object, RemoteNode node) throws ClosedNodeException, TimeoutException, InterruptedException, CommandException {
		TransactionRequest<?> request;

		try {
			request = node.getRequest(object.getTransaction());
		}
		catch (UnknownReferenceException e) {
			throw new CommandException(object + " cannot be found in the store of the node");
		}

		if (request instanceof CodeExecutionTransactionRequest<?> cetr)
			return cetr.getClasspath();
		else if (request instanceof GameteCreationTransactionRequest gctr)
			return gctr.getClasspath();
		else
			throw new CommandException("Object " + object + " has been unexpectedly created with a " + request.getClass().getName());
	}

	protected BigInteger gasForTransactionWhosePayerHasSignature(SignatureAlgorithm signature) {
		switch (signature.getName()) {
		case "qtesla1":
			return BigInteger.valueOf(3_000_000L);
		case "qtesla3":
			return BigInteger.valueOf(6_000_000L);
		default:
			return _500_000;
		}
	}

	/**
	 * Yields the signature algorithm of the account with the given storage reference.
	 * 
	 * @param account the storage reference of the account
	 * @param remote the remote node whose store will be used
	 * @return the signature algorithm for {@code account}
	 * @throws CommandException if {@code account} cannot be found in the store of the node or if its signature algorithm is not available
	 * @throws InterruptedException if the operation gets interrupted while waiting
	 * @throws TimeoutException if the operation times out
	 * @throws ClosedNodeException if the node is already closed
	 * @throws MisbehavingNodeException if the node is behaving in a buggy way
	 */
	protected SignatureAlgorithm determineSignatureOf(StorageReference account, RemoteNode remote) throws CommandException, InterruptedException, TimeoutException, MisbehavingNodeException, ClosedNodeException {
		try {
			return SignatureHelpers.of(remote).signatureAlgorithmFor(account);
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException(account + " uses a non-available signature algorithm", e);
		}
		catch (UnknownReferenceException e) {
			throw new CommandException(account + " cannot be found in the store of the node");
		}
		catch (UnsupportedVerificationVersionException e) {
			throw new CommandException("The node uses a verification version that is not available");
		}
	}

	/**
	 * Yields the nonce of the account with the given storage reference.
	 * 
	 * @param account the storage reference of the account
	 * @param remote the remote node whose store will be used
	 * @return the nonce of {@code account}
	 * @throws CommandException if {@code account} cannot be found in the store of the node
	 * @throws InterruptedException if the operation gets interrupted while waiting
	 * @throws TimeoutException if the operation times out
	 * @throws UnexpectedCodeException if the Takamaka code in the store of the node is unexpected
	 * @throws ClosedNodeException if the node is already closed
	 */
	protected BigInteger determineNonceOf(StorageReference account, RemoteNode remote) throws CommandException, InterruptedException, TimeoutException, ClosedNodeException, UnexpectedCodeException {
		try {
			return NonceHelpers.of(remote).getNonceOf(account);
		}
		catch (UnknownReferenceException e) {
			throw new CommandException(account + " cannot be found in the store of the node");
		}
		catch (CodeExecutionException | TransactionRejectedException | TransactionException e) {
			throw new CommandException("Cannot determine the nonce of " + account + "!", e);
		}
	}

	/**
	 * Asks the user about the real intention to spend some gas.
	 * 
	 * @param goal the goal why confirmation is being asked
	 * @param gasLimit the amount of gas
	 * @param gasPrice the proposed price for a unit of gas
	 * @param skipped true if the question must be skipped
	 * @throws CommandException if the user replies negatively
	 */
	protected void askForConfirmation(String goal, BigInteger gasLimit, BigInteger gasPrice, boolean skipped) throws CommandException {
		if (!skipped && !answerIsYes(asInteraction("Do you really want to " + goal + " spending up to "
				+ gasUnits(gasLimit) + " at the price of " + panas(gasPrice) + " per unit (that is, up to " + panas(gasLimit.multiply(gasPrice)) + ") [Y/N] ")))
			throw new CommandException("Stopped");
	}

	/**
	 * Binds the key pair specified by the given identifier, if it exists, to the
	 * given account, and saves the result inside the given directory.
	 * 
	 * @param publicKeyOrKeyPair the key identifier
	 * @param account the account
	 * @param dir the directory where the key pair must be saved, for the given {@code account}
	 * @return the path where the key pair of {@code account} has been saved, if any; this is empty
	 *         if {@code publicKeyIdentifier} used the --key option rather than the --keys option,
	 *         because this method performs nothing in that case
	 * @throws CommandException if the operation fails
	 */
	protected Optional<Path> bindKeysToAccount(PublicKeyOrKeyPair publicKeyOrKeyPair, StorageReference account, Path dir) throws CommandException {
		Optional<Path> maybeKeys = publicKeyOrKeyPair.getPathOfKeyPair();

		if (maybeKeys.isPresent())
			return Optional.of(bindKeysToAccount(maybeKeys.get(), account, dir));
		else
			return Optional.empty();
	}

	protected Path bindKeysToAccount(Path keys, StorageReference account, Path dir) throws CommandException {
		Entropy entropy;

		try {
			entropy = Entropies.load(keys);
		}
		catch (IOException e) {
			throw new CommandException("Cannot access file \"" + keys + "\"!", e);
		}

		var newAccount = Accounts.of(entropy, account);
		Path file = dir.resolve(newAccount + ".pem");
		try {
			newAccount.dump(file);
		}
		catch (IOException e) {
			throw new CommandException("Cannot save the key pair of " + account + " in file \"" + newAccount + ".pem\"!");
		}

		return file;
	}

	protected static String panas(BigInteger cost) {
		return cost.equals(BigInteger.ONE) ? "1 pana" : (cost + " panas");
	}

	protected String gasUnits(BigInteger units) {
		return units.equals(BigInteger.ONE) ? "1 gas unit" : (units + " gas units");
	}

	/**
	 * Styles the given string in red.
	 * 
	 * @param text the text to style
	 * @return the colored text
	 */
	protected static String red(String text) {
		return Ansi.AUTO.string("@|red " + text + "|@");
	}

	/**
	 * Styles the given string in green.
	 * 
	 * @param text the text to style
	 * @return the colored text
	 */
	protected static String green(String text) {
		return Ansi.AUTO.string("@|green " + text + "|@");
	}

	/**
	 * Styles the given string in cyan.
	 * 
	 * @param text the text to style
	 * @return the colored text
	 */
	protected static String cyan(String text) {
		return Ansi.AUTO.string("@|cyan " + text + "|@");
	}

	/**
	 * Styles the given string as for the command style.
	 * 
	 * @param text the text to style
	 * @return the styled text
	 */
	protected static String asCommand(String text) {
		return Ansi.AUTO.string("@|bold " + text + "|@");
	}

	/**
	 * Styles the given URI in URI style.
	 * 
	 * @param uri the URI to style
	 * @return the styled URI text
	 */
	protected static String asUri(URI uri) {
		return Ansi.AUTO.string("@|blue " + uri + "|@");
	}

	/**
	 * Styles the given path in path style.
	 * 
	 * @param path the path
	 * @return the styled path
	 */
	protected static String asPath(Path path) {
		return Ansi.AUTO.string("@|bold,fg(1;3;1) \"" + path + "\"|@");
	}

	/**
	 * Styles the given transaction reference in transaction reference style.
	 * 
	 * @param reference the reference
	 * @return the styled text
	 */
	protected static String asTransactionReference(TransactionReference reference) {
		return Ansi.AUTO.string("@|fg(5;3;2) " + reference + "|@");
	}

	/**
	 * Styles the given string as for the user interaction style.
	 * 
	 * @param text the text to style
	 * @return the styled text
	 */
	protected static String asInteraction(String text) {
		return Ansi.AUTO.string("@|bold,red " + text + "|@");
	}
}