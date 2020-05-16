package io.hotmoka.nodes.internal;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.InitializedNodeWithHistory;
import io.hotmoka.nodes.NodeWithHistory;

/**
 * A decorator of a node, that installs a jar and creates some initial accounts in it.
 * It is mainly useful for testing.
 */
public class InitializedNodeWithHistoryImpl extends InitializedNodeImpl implements InitializedNodeWithHistory {

	/**
	 * Creates a decorated node by storing into it a jar and creating initial accounts.
	 * 
	 * @param parent the node that gets decorated
	 * @param payer the payer of the initialization transactions; if red/green accounts are being created,
	 *              then this must be a red/green externally owned account; otherwise, it can also be
	 *              a normal externally owned account
	 * @param jar the path of a jar that must be further installed in blockchain. This might be {@code null}
	 * @param redGreen true if red/green accounts must be created; if false, normal externally owned accounts are created
	 * @param funds the initial funds of the accounts that are created; if {@code redGreen} is true,
	 *              they must be understood in pairs, each pair for the green/red initial funds of each account (red before green)
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 */
	public InitializedNodeWithHistoryImpl(NodeWithHistory parent, StorageReference payer, Path jar, boolean redGreen, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException {
		super(parent, payer, jar, redGreen, funds);
	}

	@Override
	public TransactionRequest<?> getRequestAt(TransactionReference reference) throws NoSuchElementException {
		return ((NodeWithHistory) parent).getRequestAt(reference);
	}

	@Override
	public TransactionResponse getResponseAt(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
		return ((NodeWithHistory) parent).getResponseAt(reference);
	}
}