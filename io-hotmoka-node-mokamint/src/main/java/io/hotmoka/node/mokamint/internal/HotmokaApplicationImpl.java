package io.hotmoka.node.mokamint.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import io.hotmoka.annotations.GuardedBy;
import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.constants.Constants;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.NullValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.local.AbstractTrieBasedLocalNode;
import io.hotmoka.node.local.LRUCache;
import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.node.local.StateIds;
import io.hotmoka.node.local.api.StateId;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.local.api.UnknownStateIdException;
import io.hotmoka.node.mokamint.api.Application;
import io.hotmoka.node.mokamint.api.MokamintNode;
import io.hotmoka.node.mokamint.api.MokamintNodeConfig;
import io.hotmoka.xodus.env.Environment;
import io.mokamint.application.AbstractApplication;
import io.mokamint.application.api.ClosedApplicationException;
import io.mokamint.application.api.Description;
import io.mokamint.application.api.Name;
import io.mokamint.application.api.UnknownGroupIdException;
import io.mokamint.application.api.UnknownStateException;
import io.mokamint.node.Transactions;
import io.mokamint.node.api.ApplicationTimeoutException;
import io.mokamint.node.api.Block;
import io.mokamint.node.api.NonGenesisBlock;
import io.mokamint.node.api.PublicNode;
import io.mokamint.node.api.Transaction;
import io.mokamint.nonce.api.Deadline;

/**
 * Implementation of an application for the Mokamint engine, that supports a Hotmoka node.
 * Its constructor creates the Hotmoka node as well, which is later accessible
 * via the {@link #getNode()} method.
 * 
 * @param <E> the type of the underlying Mokamint engine
 */
@Name("Hotmoka")
@Description("A blockchain with Takamaka smart contracts")
public class HotmokaApplicationImpl<E extends PublicNode> extends AbstractApplication implements Application<E> {

	/**
	 * The current store transformations, for each group id of Hotmoka transactions.
	 */
	private final ConcurrentMap<Integer, MokamintStoreTransformation> transformations = new ConcurrentHashMap<>();

	/**
	 * The final state identifiers for the {@link #transformations}.
	 */
	private final ConcurrentMap<Integer, StateId> finalStateIds = new ConcurrentHashMap<>();

	/**
	 * The database transaction that begins at {@link #endBlock(int, Deadline)} for each of the {@link #transformations}.
	 */
	private final ConcurrentMap<Integer, io.hotmoka.xodus.env.Transaction> txns = new ConcurrentHashMap<>();

	/**
	 * The next group id to use for the next transformation that will be started with this application.
	 */
	private final AtomicInteger nextId = new AtomicInteger();

	/**
	 * A cache for the caches at a given state identifier. This allows to reuse
	 * a previous cache if an old state identifier is checked out. The alternative
	 * is to recompute the cache at the state identifier, which is expensive.
	 */
	private final LRUCache<StateId, StoreCache> stateIdentifiersCache = new LRUCache<>(100, 1000);

	/**
	 * The hashing algorithm to use for the bytes of a transaction, in order to get its reference.
	 */
	private final HashingAlgorithm sha256;

	/**
	 * The Hotmoka node connected to this application.
	 */
	private final MokamintNodeImpl node;

	private final static Logger LOGGER = Logger.getLogger(HotmokaApplicationImpl.class.getName());

	/**
	 * Creates a Mokamint application that supports a Hotmoka node.
	 * 
	 * @param config the configuration of the Hotmoka node
	 * @param init if true, the working directory of the node gets initialized
	 */
	public HotmokaApplicationImpl(MokamintNodeConfig config, boolean init) {
		this.node = new MokamintNodeImpl(config, init);

		try {
			this.sha256 = HashingAlgorithms.sha256();
		}
		catch (NoSuchAlgorithmException e) {
			throw new LocalNodeException(e);
		}
	}

	@Override
	public MokamintNode<E> getNode() {
		return node;
	}

	@Override
	public Optional<BigInteger> getBalance(SignatureAlgorithm signature, PublicKey publicKey) throws ClosedApplicationException, InterruptedException {
		try (var scope = mkScope()) {
			return node.getBalance(signature, publicKey);
		}
		catch (ClosedNodeException e) {
			throw new ClosedApplicationException(e);
		}
	}

	@Override
	public boolean checkPrologExtra(byte[] extra) throws ClosedApplicationException {
		try (var scope = mkScope()) {
			return extra.length == 0; // no extra used by this application
		}
	}

	@Override
	public void checkTransaction(Transaction transaction) throws ClosedApplicationException {
		try (var scope = mkScope()) {
			// nothing, there is nothing we can check for Hotmoka
		}
	}

	@Override
	public long getPriority(Transaction transaction) throws ClosedApplicationException {
		try (var scope = mkScope()) {
			return 0;
		}
	}

	@Override
	public String getRepresentation(Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException, ClosedApplicationException {
		try (var scope = mkScope()) {
			return intoHotmokaRequest(transaction).toString();
		}
	}

	@Override
	public byte[] getInitialStateId() throws ClosedApplicationException {
		try (var scope = mkScope()) {
			return new byte[MokamintNodeImpl.SIZE_OF_STATE_ID];
		}
	}

	@Override
	public int beginBlock(long height, LocalDateTime when, byte[] stateId) throws UnknownStateException, ClosedApplicationException, InterruptedException {
		var si = StateIds.of(stateId);

		MokamintStore start;

		try {
			// if we have information about the cache at the requested state id, we use it for better efficiency
			start = node.enter(si, Optional.ofNullable(stateIdentifiersCache.get(si)));
		}
		catch (UnknownStateIdException e) {
			throw new UnknownStateException(e);
		}

		int groupId = nextId.getAndIncrement();
		transformations.put(groupId, start.beginTransformation(when.toInstant(ZoneOffset.UTC).toEpochMilli()));

		return groupId;
	}

	@Override
	public void deliverTransaction(int groupId, Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException, UnknownGroupIdException, ClosedApplicationException, InterruptedException {
		try (var scope = mkScope()) {
			// no need to signalRejected() if the following fails, since it means that the transaction is not really
			// a Hotmoka request, therefore nobody is waiting for it
			TransactionRequest<?> hotmokaRequest = intoHotmokaRequest(transaction);
			MokamintStoreTransformation transformation = getTransformation(groupId);

			try {
				transformation.deliverTransaction(hotmokaRequest);
			}
			catch (TransactionRejectedException e) {
				node.signalRejected(hotmokaRequest, e);
				throw new io.mokamint.node.api.TransactionRejectedException(e.getMessage(), e);
			}
		}
	}

	@Override
	public byte[] endBlock(int groupId, Deadline deadline) throws ClosedApplicationException, UnknownGroupIdException, InterruptedException {
		try (var scope = mkScope()) {
			MokamintStoreTransformation transformation = getTransformation(groupId);
			transformation.deliverCoinbaseTransactions(deadline.getProlog());
			io.hotmoka.xodus.env.Transaction txn = node.getEnvironment().beginExclusiveTransaction();
			StateId idOfFinalStore;

			try {
				idOfFinalStore = transformation.getIdOfFinalStore(txn);
			}
			catch (RuntimeException e) {
				txn.abort();
				throw e;
			}

			finalStateIds.put(groupId, idOfFinalStore);
			stateIdentifiersCache.put(idOfFinalStore, transformation.getCache());
			txns.put(groupId, txn);

			return idOfFinalStore.getBytes();
		}
	}

	@Override
	public void commitBlock(int groupId) throws UnknownGroupIdException, ClosedApplicationException {
		try (var scope = mkScope()) {
			var transformation = getTransformation(groupId);
			var idOfFinalStore = finalStateIds.get(groupId);
			var txn = txns.remove(groupId);

			try {
				node.persist(idOfFinalStore, transformation.getNow(), txn);

				if (!txn.commit())
					// the transaction was exclusive, this should not happen then
					throw new LocalNodeException("Could not commit the block");
			}
			catch (UnknownStateIdException e) {
				// impossible, we have just computed this id inside endBlock(), which was meant to be called before this method
				txn.abort();
				throw new LocalNodeException("State id " + idOfFinalStore + " has been just computed: it must have existed", e);
			}
			finally {
				node.exit(transformation.getInitialStore());
				transformations.remove(groupId);
				finalStateIds.remove(groupId);
			}

			LOGGER.fine(() -> "persisted state " + idOfFinalStore);
		}
	}

	@Override
	public void abortBlock(int groupId) throws UnknownGroupIdException, ClosedApplicationException {
		try (var scope = mkScope()) {
			var transformation = getTransformation(groupId);
			var initialStore = transformation.getInitialStore();
			var initialStateId = initialStore.getStateId();
			var txn = txns.remove(groupId);

			try {
				// txn might be null if abortBlock() is called before previously calling endBlock()
				if (txn != null)
					txn.abort();
			}
			finally {
				node.exit(initialStore);
				transformations.remove(groupId);
				finalStateIds.remove(groupId);
			}

			LOGGER.info(() -> "aborted block creation from state " + initialStateId);
		}
	}

	@Override
	public void keepFrom(LocalDateTime start) throws ClosedApplicationException {
		try (var scope = mkScope()) {
			long limitOfTimeForGC = start.toInstant(ZoneOffset.UTC).toEpochMilli();
			node.getEnvironment().executeInTransaction(txn -> node.keepPersistedOnlyNotOlderThan(limitOfTimeForGC, txn));
		}
	}

	@Override
	public void publish(Block block) throws ClosedApplicationException, InterruptedException {
		try (var scope = mkScope()) {
			if (block instanceof NonGenesisBlock ngb) {
				var si = StateIds.of(block.getStateId());

				try {
					MokamintStore store = node.enter(si, Optional.ofNullable(stateIdentifiersCache.get(si)));

					try {
						ngb.getTransactions().forEachOrdered(tx -> publish(tx, store));
					}
					finally {
						node.exit(store);
					}
				}
				catch (UnknownStateIdException e) {
					// this might happen if publishing is very slow or garbage-collection is very aggressive
					LOGGER.warning("cannot publish the events in block " + block.getHexHash() + ": its state has been garbage-collected: " + e.getMessage());
				}
			}
		}
	}

	@Override
	protected void closeResources() {
		node.close();
		super.closeResources();
	}

	private void publish(Transaction tx, MokamintStore store) {
		try {
			var reference = TransactionReferences.of(tx.getHash(sha256));
			node.publish(reference, store.getResponse(reference), store);
		}
		catch (UnknownReferenceException e) {
			// the transactions have been delivered and the store is immutable, if they cannot be found at the end of the block then there is a problem in the database
			throw new LocalNodeException("Already delivered transactions should be in store", e);
		}
	}

	private static TransactionRequest<?> intoHotmokaRequest(Transaction transaction) throws io.mokamint.node.api.TransactionRejectedException {
		try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(transaction.getBytes()))) {
			try {
				return TransactionRequests.from(context);
			}
			catch (IOException e) {
				throw new io.mokamint.node.api.TransactionRejectedException(e.getMessage());
			}
		}
		catch (IOException e) {
			// this is thrown by implicit context.close() call; this it is not expected to happen,
			// since we are working on a ByteArrayInputStream, that is not a real file and whose close() throws no exception
			throw new LocalNodeException(e);
		}
	}

	/**
	 * Yields the transformation with the given group id, if it is currently under execution with this application.
	 * 
	 * @param groupId the group id of the transformation
	 * @return the transformation
	 * @throws UnknownGroupIdException if no transformation for the given group id is currently under execution with this application
	 */
	private MokamintStoreTransformation getTransformation(int groupId) throws UnknownGroupIdException {
		MokamintStoreTransformation transformation = transformations.get(groupId);

		if (transformation != null)
			return transformation;
		else
			throw new UnknownGroupIdException("Group id " + groupId + " is unknown");
	}

	/**
	 * An implementation of a Hotmoka node that relies on the Mokamint proof of space engine.
	 */
	@ThreadSafe
	class MokamintNodeImpl extends AbstractTrieBasedLocalNode<HotmokaApplicationImpl<?>.MokamintNodeImpl, MokamintNodeConfig, MokamintStore, MokamintStoreTransformation> implements MokamintNode<E> {

		/**
		 * The underlying Mokamint engine.
		 */
		private volatile E engine;

		/**
		 * The target block creation time (in milliseconds) of {@link #engine}.
		 */
		private volatile long targetBlockCreationTime;

		/**
		 * A lock for accessing {@link #lastHeadStateId} and {@link #storeOfHead}.
		 */
		private final Object headLock = new Object();

		/**
		 * The last state identifier used in {@link #getStoreOfHead()}, for caching.
		 */
		@GuardedBy("headLock")
		private byte[] lastHeadStateId;

		/**
		 * The last store computed in {@link #getStoreOfHead()}, for caching.
		 */
		@GuardedBy("headLock")
		private MokamintStore storeOfHead;

		/**
		 * Builds and starts a Mokamint node that uses an already existing store. The consensus
		 * parameters are recovered from the manifest in the store, hence the store must
		 * be that of an already initialized blockchain. It spawns the Mokamint engine
		 * and connects it to an application for handling its transactions.
		 * 
		 * @param config the configuration of the Hotmoka node
		 * @param init if true, the working directory of the node gets initialized
		 */
		private MokamintNodeImpl(MokamintNodeConfig config, boolean init) {
			super(config, init);

			int size = getLocalConfig().getIndexSize();
			if (size > 0) {
				var indexer = new Indexer(this, getStoreOfNode(), getEnvironment(), size);
				getExecutors().execute(indexer::run);
			}
		}

		@Override
		public void setMokamintEngine(E engine) throws TimeoutException, InterruptedException, io.mokamint.node.api.ClosedNodeException {
			this.engine = engine;
			this.targetBlockCreationTime = engine.getConfig().getTargetBlockCreationTime();
		}

		@Override
		public Optional<E> getMokamintEngine() {
			return Optional.ofNullable(engine);
		}

		@Override
		public NodeInfo getInfo() throws ClosedNodeException, InterruptedException, TimeoutException {
			try (var scope = mkScope()) {
				return NodeInfos.of(MokamintNode.class.getName(), Constants.HOTMOKA_VERSION, engine.getInfo().getUUID().toString());
			}
			catch (io.mokamint.node.api.ClosedNodeException e) {
				throw new ClosedNodeException(e);
			}
		}

		@Override
		protected MokamintStore mkEmptyStore() {
			return new MokamintStore(this);
		}

		@Override
		protected long getResponseWaitingTime() {
			// 20 blocks
			return targetBlockCreationTime * 20L;
		}

		@Override
		protected MokamintStore getStoreOfHead() throws ClosedNodeException, InterruptedException, TimeoutException {
			try {
				var maybeHeadStateId = engine.getChainInfo().getHeadStateId();
				if (maybeHeadStateId.isEmpty())
					return mkEmptyStore();

				synchronized (headLock) {
					if (lastHeadStateId != null && Arrays.equals(lastHeadStateId, maybeHeadStateId.get()))
						return storeOfHead;
				}

				var si = StateIds.of(maybeHeadStateId.get());
				MokamintStore result = mkStore(si, Optional.ofNullable(stateIdentifiersCache.get(si)));

				synchronized (headLock) {
					lastHeadStateId = maybeHeadStateId.get();
					storeOfHead = result;
				}

				return result;
			}
			catch (io.mokamint.node.api.ClosedNodeException e) {
				throw new ClosedNodeException(e);
			}
			catch (UnknownStateIdException e) {
				// this should not happen since this method is executed in mutual exclusion with garbage-collection;
				// therefore, the state of the head should not be garbage-collected after the call to getHeadStateId()
				throw new LocalNodeException("The state of the head block is unknown or has been garbage-collected!", e);
			}
		}

		@Override
		protected void postRequest(TransactionRequest<?> request) throws ClosedNodeException, InterruptedException, TimeoutException {
			try {
				engine.add(Transactions.of(request.toByteArray()));
			}
			catch (io.mokamint.node.api.TransactionRejectedException e) {
				// the mempool of the Mokamint engine has rejected the transaction
				signalRejected(request, new TransactionRejectedException(e.getMessage()));
			}
			catch (io.mokamint.node.api.ClosedNodeException e) {
				throw new ClosedNodeException(e);
			}
			catch (ApplicationTimeoutException e) {
				throw new LocalNodeException("Unexpected exception: the application is local and its method never go into timeout", e);
			}
		}

		@Override
		protected MokamintStore enter(StateId stateId, Optional<StoreCache> cache) throws UnknownStateIdException, InterruptedException {
			return super.enter(stateId, cache);
		}

		@Override
		protected void exit(MokamintStore store) {
			super.exit(store);
		}

		@Override
		protected Environment getEnvironment() {
			return super.getEnvironment();
		}

		@Override
		protected void signalRejected(TransactionRequest<?> request, TransactionRejectedException e) {
			super.signalRejected(request, e);
		}

		@Override
		protected void publish(TransactionReference transaction, TransactionResponse response, MokamintStore store) {
			super.publish(transaction, response, store);
		}

		@Override
		protected void keepPersistedOnlyNotOlderThan(long limitCreationTime, io.hotmoka.xodus.env.Transaction txn) {
			super.keepPersistedOnlyNotOlderThan(limitCreationTime, txn);
		}

		@Override
		protected void persist(StateId stateId, long now, io.hotmoka.xodus.env.Transaction txn) throws UnknownStateIdException {
			super.persist(stateId, now, txn);
		}

		@Override
		protected void free(StateId stateId, io.hotmoka.xodus.env.Transaction txn) throws UnknownStateIdException {
			super.free(stateId, txn);
		}

		private Optional<BigInteger> getBalance(SignatureAlgorithm signature, PublicKey publicKey) throws ClosedNodeException, InterruptedException {
			try {
				String publicKeyBase64;

				try {
					publicKeyBase64 = Base64.toBase64String(signature.encodingOf(publicKey));
				}
				catch (InvalidKeyException e) {
					LOGGER.warning("could not determine the balance of the public key since it is invalid for signature " + signature);
					return Optional.empty();
				}

				StorageReference manifest = getManifest();
				TransactionReference takamakaCode = getTakamakaCode();
				StorageValue account;
				var _500_000 = BigInteger.valueOf(500_000L);

				try {
					// we look for the accounts ledger
					var ledger = runInstanceMethodCallTransaction
							(TransactionRequests.instanceViewMethodCall(manifest, _500_000, takamakaCode, MethodSignatures.GET_ACCOUNTS_LEDGER, manifest))
							.orElseThrow(() -> new LocalNodeException(MethodSignatures.GET_ACCOUNTS_LEDGER + " should not return void"))
							.asReturnedReference(MethodSignatures.GET_ACCOUNTS_LEDGER, LocalNodeException::new);

					// we look for the public key inside the accounts ledger
					account = runInstanceMethodCallTransaction
							(TransactionRequests.instanceViewMethodCall(manifest, _500_000, takamakaCode, MethodSignatures.GET_FROM_ACCOUNTS_LEDGER, ledger, StorageValues.stringOf(publicKeyBase64)))
							.orElseThrow(() -> new LocalNodeException(MethodSignatures.GET_FROM_ACCOUNTS_LEDGER + " should not return void"));
				}
				catch (CodeExecutionException | TransactionException | TransactionRejectedException e) {
					throw new LocalNodeException("Could not access the accounts ledger", e);
				}

				if (account instanceof StorageReference sr) {
					try {
						BigInteger balance = runInstanceMethodCallTransaction
								(TransactionRequests.instanceViewMethodCall(manifest, _500_000, takamakaCode, MethodSignatures.BALANCE, sr))
								.orElseThrow(() -> new LocalNodeException(MethodSignatures.BALANCE + " should not return void"))
								.asReturnedBigInteger(MethodSignatures.BALANCE, LocalNodeException::new);

						return Optional.of(balance);
					}
					catch (CodeExecutionException | TransactionException | TransactionRejectedException e) {
						throw new LocalNodeException("Could not determine the balance of account " + sr + " found in the accounts ledger", e);
					}
				}
				else if (account instanceof NullValue)
					// there is no account in the accounts ledger for the given public key
					return Optional.empty();
				else
					throw new LocalNodeException("An unexpected value of type " + account.getClass().getSimpleName() + " has been found in the accounts ledger");
			}
			catch (TimeoutException | UninitializedNodeException e) {
				LOGGER.warning("could not determine the balance of a public key: " + e.getMessage());
				return Optional.empty();
			}
		}
	}
}