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

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;
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
import io.hotmoka.moka.api.accounts.AccountsShowOutput;
import io.hotmoka.moka.internal.AbstractCommand;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.converters.StorageReferenceOptionConverter;
import io.hotmoka.moka.internal.json.AccountsShowOutputJson;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.updates.UpdateOfString;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
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
		private final Node node;
		private final Update[] updates;
		private final ClassTag tag;

		private Run(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			this.node = remote;
			try {
				this.updates = node.getState(object).sorted().toArray(Update[]::new);
			}
			catch (UnknownReferenceException e) {
				throw new CommandException("The object " + object + " cannot be found in the store of the node.");
			}

			this.tag = getClassTag();

			printHeader();
			printFieldsInClass();
			printFieldsInherited();
			try {
				printAPI();
			}
			catch (ClassNotFoundException | TransactionRejectedException | TransactionException | CodeExecutionException | UnknownReferenceException e) {
				e.printStackTrace();
			}
		}

		private void printAPI() throws ClassNotFoundException, TransactionRejectedException, TransactionException, CodeExecutionException, UnknownReferenceException, NodeException, TimeoutException, InterruptedException {
			System.out.println();
			if (api)
				new PrintAPI(node, tag);
		}

		private void printFieldsInherited() {
			Stream.of(updates)
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> !update.getField().getDefiningClass().equals(tag.getClazz()))
				.forEachOrdered(this::printUpdate);
		}

		private void printFieldsInClass() {
			Stream.of(updates)
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> update.getField().getDefiningClass().equals(tag.getClazz()))
				.forEachOrdered(this::printUpdate);
		}

		private void printHeader() {
			ClassType clazz = tag.getClazz();
			System.out.println(AbstractCommand.ANSI_RED + "\nThis is the state of object " + object + "@" + uri() + "\n");
			System.out.println(AbstractCommand.ANSI_RESET + "class " + clazz + " (from jar installed at " + tag.getJar() + ")");
		}

		private ClassTag getClassTag() {
			return Stream.of(updates)
					.filter(update -> update instanceof ClassTag)
					.map(update -> (ClassTag) update)
					.findFirst().get();
		}

		private void printUpdate(UpdateOfField update) {
			FieldSignature field = update.getField();
			if (tag.getClazz().equals(field.getDefiningClass()))
				System.out.println(AbstractCommand.ANSI_RESET + "  " + field.getName() + ":" + field.getType() + " = " + valueToPrint(update));
			else
				System.out.println(AbstractCommand.ANSI_CYAN + "\u25b2 " + field.getName() + ":" + field.getType() + " = " + valueToPrint(update) + AbstractCommand.ANSI_GREEN + " (inherited from " + field.getDefiningClass() + ")");
		}

		private String valueToPrint(UpdateOfField update) {
			if (update instanceof UpdateOfString)
				return '\"' + update.getValue().toString() + '\"';
			else
				return update.getValue().toString();
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