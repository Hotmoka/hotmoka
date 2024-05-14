/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.local.internal.store;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.AbstractInstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.local.LRUCache;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.ResponseBuilder;
import io.hotmoka.node.local.api.StoreException;

public abstract class ExecutionEnvironment {
	private final static Logger LOGGER = Logger.getLogger(ExecutionEnvironment.class.getName());

	protected ExecutionEnvironment() {
	}

	public final Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, TransactionReference reference) throws TransactionRejectedException, TransactionException, CodeExecutionException, StoreException {
		return getOutcome(new InstanceViewMethodCallResponseBuilder(reference, request, this).getResponse());
	}

	public final Optional<StorageValue> runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, TransactionReference reference) throws TransactionRejectedException, TransactionException, CodeExecutionException, StoreException {
		return getOutcome(new StaticViewMethodCallResponseBuilder(reference, request, this).getResponse());
	}

	/**
	 * Yields the request that generated the transaction with the given reference.
	 * If this node has some form of commit, then this method is called only when
	 * the transaction has been already committed.
	 * 
	 * @param reference the reference of the transaction
	 * @return the request
	 */
	public abstract TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, StoreException;

	/**
	 * Yields the response of the transaction having the given reference.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response
	 */
	public abstract TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException, StoreException;

	/**
	 * Yields the history of the given object, that is, the references to the transactions
	 * that can be used to reconstruct the current values of its fields.
	 * 
	 * @param object the reference of the object
	 * @return the history
	 * @throws StoreException if the store is not able to perform the operation
	 */
	public abstract Stream<TransactionReference> getHistory(StorageReference object) throws UnknownReferenceException, StoreException;

	/**
	 * Yields the manifest installed when the node is initialized.
	 * 
	 * @return the manifest
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	public abstract Optional<StorageReference> getManifest() throws StoreException;

	/**
	 * Yields the current consensus configuration of the node.
	 * 
	 * @return the current consensus configuration of the node
	 */
	public abstract ConsensusConfig<?,?> getConfig() throws StoreException;

	/**
	 * Yields a class loader for the given class path, using a cache to avoid regeneration, if possible.
	 * 
	 * @param classpath the class path that must be used by the class loader
	 * @return the class loader
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	protected final EngineClassLoader getClassLoader(TransactionReference classpath, ConsensusConfig<?,?> consensus) throws StoreException {
		try {
			var classLoaders = getClassLoaders();

			var classLoader = classLoaders.get(classpath);
			if (classLoader != null)
				return classLoader;

			var classLoader2 = new EngineClassLoaderImpl(null, Stream.of(classpath), this, consensus);
			return classLoaders.computeIfAbsent(classpath, _classpath -> classLoader2);
		}
		catch (ClassNotFoundException e) {
			// since the class loader is created from transactions that are already in the store,
			// they should be consistent and never miss a dependent class
			throw new StoreException(e);
		}
	}

	protected final ClassTag getClassTag(StorageReference reference) throws UnknownReferenceException, StoreException {
		// we go straight to the transaction that created the object
		if (getResponse(reference.getTransaction()) instanceof TransactionResponseWithUpdates trwu) {
			return trwu.getUpdates().filter(update -> update instanceof ClassTag && update.getObject().equals(reference))
					.map(update -> (ClassTag) update)
					.findFirst()
					.orElseThrow(() -> new UnknownReferenceException("Object " + reference + " does not exist"));
		}
		else
			throw new UnknownReferenceException("Transaction reference " + reference + " does not contain updates");
	}

	protected final String getClassName(StorageReference reference) throws UnknownReferenceException, StoreException {
		return getClassTag(reference).getClazz().getName();
	}

	protected final StorageReference getReferenceField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		StorageValue value = getLastUpdateToField(object, field).getValue();
		if (value instanceof StorageReference reference)
			return reference;
		else
			throw new FieldNotFoundException(field);
	}

	protected final UpdateOfField getLastUpdateToField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		Stream<TransactionReference> history = getHistory(object);

		try {
			return CheckSupplier.check(StoreException.class, UnknownReferenceException.class, () -> history.map(UncheckFunction.uncheck(transaction -> getLastUpdate(object, field, transaction)))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.findFirst())
					.orElseThrow(() -> new FieldNotFoundException(field));
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("Object " + object + " has a history containing a reference not in store");
		}
	}

	protected final UpdateOfField getLastUpdateToFinalField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		// accesses directly the transaction that created the object
		return getLastUpdate(object, field, object.getTransaction()).orElseThrow(() -> new FieldNotFoundException(field));
	}

	protected final Optional<StorageReference> getGamete() throws StoreException {
		var maybeManifest = getManifest();
		if (maybeManifest.isPresent()) {
			try {
				return Optional.of(getReferenceField(maybeManifest.get(), FieldSignatures.MANIFEST_GAMETE_FIELD));
			}
			catch (FieldNotFoundException e) {
				throw new StoreException("The manifest does not contain the reference to the gamete", e);
			}
			catch (UnknownReferenceException e) {
				throw new StoreException("The manifest is set but cannot be found in store", e);
			}
		}
		else
			return Optional.empty();
	}

	protected final BigInteger getBigIntegerField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		StorageValue value = getLastUpdateToField(object, field).getValue();
		if (value instanceof BigIntegerValue biv)
			return biv.getValue();
		else
			throw new FieldNotFoundException(field);
	}

	protected final Optional<StorageReference> getValidators() throws StoreException {
		var maybeManifest = getManifest();
		if (maybeManifest.isPresent()) {
			try {
				return Optional.of(getReferenceField(maybeManifest.get(), FieldSignatures.MANIFEST_VALIDATORS_FIELD));
			}
			catch (FieldNotFoundException e) {
				throw new StoreException("The manifest does not contain the reference to the validators set", e);
			}
			catch (UnknownReferenceException e) {
				throw new StoreException("The manifest is set but cannot be found in store", e);
			}
		}
		else
			return Optional.empty();
	}

	protected final BigInteger getTotalBalance(StorageReference contract) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBalance(contract).add(getRedBalance(contract));
	}

	protected final BigInteger getNonce(StorageReference account) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBigIntegerField(account, FieldSignatures.EOA_NONCE_FIELD);
	}

	protected final Stream<UpdateOfField> getEagerFields(StorageReference object) throws UnknownReferenceException, StoreException {
		var fieldsAlreadySeen = new HashSet<FieldSignature>();

		return getHistory(object)
				.flatMap(CheckSupplier.check(StoreException.class, () -> UncheckFunction.uncheck(this::getUpdates)))
				.filter(update -> update.isEager() && update instanceof UpdateOfField uof && update.getObject().equals(object) && fieldsAlreadySeen.add(uof.getField()))
				.map(update -> (UpdateOfField) update);
	}

	protected final Stream<Update> getUpdates(TransactionReference referenceInHistory) throws StoreException {
		try {
			if (getResponse(referenceInHistory) instanceof TransactionResponseWithUpdates trwu)
				return trwu.getUpdates();
			else
				throw new StoreException("Transaction " + referenceInHistory + " belongs to the histories but does not contain updates");
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("Transaction " + referenceInHistory + " belongs to the histories but is not present in store");
		}
	}

	protected final boolean signatureIsValid(SignedTransactionRequest<?> request, SignatureAlgorithm signatureAlgorithm) throws StoreException, UnknownReferenceException, FieldNotFoundException {
		var reference = TransactionReferences.of(getHasher().hash(request));
		return CheckSupplier.check(StoreException.class, UnknownReferenceException.class, FieldNotFoundException.class, () ->
			getCheckedSignatures().computeIfAbsentNoException(reference, UncheckFunction.uncheck(_reference -> verifySignature(signatureAlgorithm, request))));
	}

	protected final String getPublicKey(StorageReference account) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getStringField(account, FieldSignatures.EOA_PUBLIC_KEY_FIELD);
	}

	protected final <X> Future<X> submit(Callable<X> task) {
		return getExecutors().submit(task);
	}

	/**
	 * Yields the builder of a response for a request of a transaction.
	 * This method can be redefined in subclasses in order to accomodate
	 * new kinds of transactions, specific to a node.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	protected final ResponseBuilder<?,?> responseBuilderFor(TransactionReference reference, TransactionRequest<?> request) throws TransactionRejectedException, StoreException {
		if (request instanceof JarStoreInitialTransactionRequest jsitr)
			return new JarStoreInitialResponseBuilder(reference, jsitr, this);
		else if (request instanceof GameteCreationTransactionRequest gctr)
			return new GameteCreationResponseBuilder(reference, gctr, this);
		else if (request instanceof JarStoreTransactionRequest jstr)
			return new JarStoreResponseBuilder(reference, jstr, this);
		else if (request instanceof ConstructorCallTransactionRequest cctr)
			return new ConstructorCallResponseBuilder(reference, cctr, this);
		else if (request instanceof AbstractInstanceMethodCallTransactionRequest aimctr)
			return new InstanceMethodCallResponseBuilder(reference, aimctr, this);
		else if (request instanceof StaticMethodCallTransactionRequest smctr)
			return new StaticMethodCallResponseBuilder(reference, smctr, this);
		else if (request instanceof InitializationTransactionRequest itr)
			return new InitializationResponseBuilder(reference, itr, this);
		else
			throw new StoreException("Unexpected transaction request of class " + request.getClass().getName());
	}

	/**
	 * Yields the time to use as current time for the requests executed inside this transaction.
	 * 
	 * @return the time, in milliseconds from the UNIX epoch time
	 */
	protected abstract long getNow();

	/**
	 * Yields the current gas price at the end of this transaction.
	 * This might be missing if the node is not initialized yet.
	 * 
	 * @return the current gas price at the end of this transaction
	 */
	protected abstract Optional<BigInteger> getGasPrice();

	protected abstract LRUCache<TransactionReference, EngineClassLoader> getClassLoaders();

	protected abstract ExecutorService getExecutors();

	protected abstract Hasher<TransactionRequest<?>> getHasher();

	protected abstract LRUCache<TransactionReference, Boolean> getCheckedSignatures();

	private boolean verifySignature(SignatureAlgorithm signature, SignedTransactionRequest<?> request) throws StoreException, UnknownReferenceException, FieldNotFoundException {
		try {
			return signature.getVerifier(getPublicKey(request.getCaller(), signature), SignedTransactionRequest<?>::toByteArrayWithoutSignature).verify(request, request.getSignature());
		}
		catch (InvalidKeyException | SignatureException | Base64ConversionException | InvalidKeySpecException e) {
			LOGGER.info("the public key of " + request.getCaller() + " could not be verified: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Yields the update to the given field of the object at the given reference, generated during a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param field the field of the object
	 * @param reference the reference to the transaction
	 * @return the update, if any. If the field of {@code object} was not modified during
	 *         the {@code transaction}, this method returns an empty optional
	 */
	private Optional<UpdateOfField> getLastUpdate(StorageReference object, FieldSignature field, TransactionReference reference) throws UnknownReferenceException, StoreException {
		if (getResponse(reference) instanceof TransactionResponseWithUpdates trwu)
			return trwu.getUpdates()
					.filter(update -> update instanceof UpdateOfField)
					.map(update -> (UpdateOfField) update)
					.filter(update -> update.getObject().equals(object) && update.getField().equals(field))
					.findFirst();
		else
			throw new StoreException("Transaction reference " + reference + " does not contain updates");
	}

	/**
	 * Yields the public key of the given externally owned account.
	 * 
	 * @param reference the account
	 * @param signatureAlgorithm the signing algorithm used for the request
	 * @return the public key
	 * @throws Base64ConversionException 
	 * @throws InvalidKeySpecException 
	 * @throws StoreException 
	 * @throws FieldNotFoundException 
	 * @throws UnknownReferenceException 
	 */
	private PublicKey getPublicKey(StorageReference reference, SignatureAlgorithm signatureAlgorithm) throws Base64ConversionException, InvalidKeySpecException, UnknownReferenceException, FieldNotFoundException, StoreException {
		String publicKeyEncodedBase64 = getPublicKey(reference);
		byte[] publicKeyEncoded = Base64.fromBase64String(publicKeyEncodedBase64);
		return signatureAlgorithm.publicKeyFromEncoding(publicKeyEncoded);
	}

	private Optional<StorageValue> getOutcome(MethodCallTransactionResponse response) throws CodeExecutionException, TransactionException {
		if (response instanceof MethodCallTransactionSuccessfulResponse mctsr)
			return Optional.of(mctsr.getResult());
		else if (response instanceof MethodCallTransactionExceptionResponse mcter)
			throw new CodeExecutionException(mcter.getClassNameOfCause(), mcter.getMessageOfCause(), mcter.getWhere());
		else if (response instanceof MethodCallTransactionFailedResponse mctfr)
			throw new TransactionException(mctfr.getClassNameOfCause(), mctfr.getMessageOfCause(), mctfr.getWhere());
		else
			return Optional.empty(); // void methods return no value
	}

	private BigInteger getBalance(StorageReference contract) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBigIntegerField(contract, FieldSignatures.BALANCE_FIELD);
	}

	private BigInteger getRedBalance(StorageReference contract) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBigIntegerField(contract, FieldSignatures.RED_BALANCE_FIELD);
	}

	private String getStringField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		StorageValue value = getLastUpdateToField(object, field).getValue();
		if (value instanceof StringValue sv)
			return sv.getValue();
		else
			throw new FieldNotFoundException(field);
	}
}