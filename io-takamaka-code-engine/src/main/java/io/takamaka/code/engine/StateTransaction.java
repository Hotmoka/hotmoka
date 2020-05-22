package io.takamaka.code.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;

/**
 * A state transaction is an atomic sequence of state updates that occur
 * at the end of a successful Hotmoka transaction. It will modify
 * the store of the node with the result of the transaction, in an atomic
 * way: either all changes occur or the state update fails.
 */
public abstract class StateTransaction {

	/**
	 * The node that is performing the transaction.
	 */
	private final AbstractNode<?> node;

	private final static Logger logger = LoggerFactory.getLogger(StateTransaction.class);

	/**
	 * This constructor implements a generic algorithm that updates
	 * the store with a transaction: the given request, with the
	 * given reference, has successfully generated the given response,
	 * and the store must be modified accordingly. The behavior of this
	 * constructor can be specialized  through the template methods
	 * in this class.
	 * 
	 * @param node the node that is performing the transaction
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 */
	protected StateTransaction(AbstractNode<?> node, TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		this.node = node;

		beginTransaction();

		writeInStore(reference, request, response);

		if (response instanceof TransactionResponseWithUpdates)
			expandHistory(reference, (TransactionResponseWithUpdates) response);

		if (response instanceof JarStoreInitialTransactionResponse && ((JarStoreInitialTransactionRequest) request).setAsTakamakaCode) {
			Classpath takamakaCode = new Classpath(reference, true);
			node.setUncommittedTakamakaCode(takamakaCode);
			setTakamakaCode(takamakaCode);
		}

		if (request instanceof NonInitialTransactionRequest && node.markAsInitialized())
			initialize();

		endTransaction();
	}

	/**
	 * Starts the atomic transaction.
	 */
	protected abstract void beginTransaction();

	/**
	 * Mark the node as initialized. This happens when a non-initial transaction succeeds.
	 */
	protected abstract void initialize();

	/**
	 * Sets the given classpath for the
	 * Takamaka base classes installed in the store of the node.
	 * 
	 * @param takamakaCode the classpath
	 */
	protected abstract void setTakamakaCode(Classpath takamakaCode);

	/**
	 * Writes in store the given request and response for the given transaction reference.
	 * 
	 * @param reference the reference of the transaction
	 * @param request the request
	 * @param response the response
	 */
	protected abstract void writeInStore(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response);

	/**
	 * Yields the history of the given object, that is,
	 * the references to the transactions that provide information about
	 * its current state, in reverse chronological order (from newest to oldest).
	 * If the node has some form of commit, this history must include also
	 * transactions executed but not yet committed.
	 * 
	 * @param object the object whose update history must be looked for
	 * @return the transactions that compose the history of {@code object}, as an ordered stream
	 *         (from newest to oldest). If {@code object} has currently no history, it yields an
	 *         empty stream, but never throw an exception
	 */
	protected abstract Stream<TransactionReference> getHistory(StorageReference object);

	/**
	 * Sets the history of the given object, that is,
	 * the references to the transactions that provide information about
	 * its current state, in reverse chronological order (from newest to oldest).
	 * 
	 * @param object the object whose history is set
	 * @param history the stream that will become the history of the object,
	 *                replacing its previous history
	 */
	protected abstract void setHistory(StorageReference object, Stream<TransactionReference> history);

	/**
	 * Ends the atomic transaction.
	 */
	protected abstract void endTransaction();

	/**
	 * Process the updates contained in the given response, expanding the history of the affected objects.
	 * 
	 * @param reference the transaction that has generated the given response
	 * @param response the response
	 */
	private void expandHistory(TransactionReference reference, TransactionResponseWithUpdates response) {
		// we collect the storage references that have been updated in the response; for each of them,
		// we fetch the list of the transaction references that affected them in the past, we add the new transaction reference
		// in front of such lists and store back the updated lists, replacing the old ones
		response.getUpdates()
			.map(Update::getObject)
			.distinct()
			.forEachOrdered(object -> node.setHistoryWithCache
				(object, simplifiedHistory
					(object, reference, response.getUpdates(), node.getHistoryWithCache(object, this::getHistory)), this::setHistory));
	}

	/**
	 * Adds the given transaction reference to the history of the given object and yields the simplified
	 * history. Simplification means that some elements of the previous history might not be useful anymore,
	 * since they get shadowed by the updates in the added transaction reference. This occurs when the value
	 * of some fields are updated in {@code added} and the useless old history element provided only values
	 * for the newly updated fields.
	 * 
	 * @param object the object whose history is being simplified
	 * @param added the transaction reference to add in front of the history of {@code object}
	 * @param addedUpdates the updates generated in {@code added}
	 * @param old the old history
	 * @return the simplified history, with {@code added} in front followed by a subset of {@code old}
	 */
	private List<TransactionReference> simplifiedHistory(StorageReference object, TransactionReference added, Stream<Update> addedUpdates, Stream<TransactionReference> old) {
		// we trace the set of updates that are already covered by previous transactions, so that
		// subsequent history elements might become unnecessary, since they do not add any yet uncovered update
		Set<Update> covered = addedUpdates.filter(update -> update.getObject() == object).collect(Collectors.toSet());
		List<TransactionReference> simplified = new ArrayList<>();
		simplified.add(added);
	
		TransactionReference[] oldAsArray = old.toArray(TransactionReference[]::new);
		int length = oldAsArray.length;
		for (int pos = 0; pos < length - 1; pos++)
			addIfUncovered(oldAsArray[pos], object, covered, simplified);
	
		// the last is always useful, since it contains the final fields and the class tag of the object
		if (length >= 1)
			simplified.add(oldAsArray[length - 1]);
	
		return simplified;
	}

	/**
	 * Adds the given transaction reference to the history of the given object,
	 * if it provides updates for fields that have not yet been covered by other updates.
	 * 
	 * @param reference the transaction reference
	 * @param object the object
	 * @param covered the set of updates for the already covered fields
	 * @param history the history; this might be modified by the method, by prefixing {@code reference} at its front
	 */
	private void addIfUncovered(TransactionReference reference, StorageReference object, Set<Update> covered, List<TransactionReference> history) {
		TransactionResponse response = node.getResponseUncommittedAt(reference);

		if (!(response instanceof TransactionResponseWithUpdates)) {
			logger.error("history contains a reference to a transaction without updates");
			throw new InternalFailureException("history contains a reference to a transaction without updates");
		}

		// we check if there is at least an update for a field of the object
		// that is not yet covered by another update in a previous element of the history
		Set<Update> diff = ((TransactionResponseWithUpdates) response).getUpdates()
			.filter(update -> update.getObject().equals(object))
			.filter(update -> covered.stream().noneMatch(update::isForSamePropertyAs))
			.collect(Collectors.toSet());

		if (!diff.isEmpty()) {
			// the transaction reference actually adds at least one useful update
			history.add(reference);
			covered.addAll(diff);
		}
	}
}