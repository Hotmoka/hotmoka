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

package io.hotmoka.helpers.internal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;

import io.hotmoka.beans.api.requests.GenericJarStoreTransactionRequest;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.LongValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.helpers.api.ClassLoaderHelper;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.api.TakamakaClassLoader;

/**
 * Implementation of a helper object for building class loaders for the jar installed at a given
 * transaction reference inside a node.
 */
public class ClassLoaderHelperImpl implements ClassLoaderHelper {
	private final Node node;
	private final StorageReference manifest;
	private final TransactionReference takamakaCode;
	private final StorageReference versions;
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	/**
	 * Creates the helper class for building class loaders for jars installed in the given node.
	 * 
	 * @param node the node
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws NodeException if the node is not able to complete the operation
	 */
	public ClassLoaderHelperImpl(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		this.node = node;
		this.manifest = node.getManifest();
		this.takamakaCode = node.getTakamakaCode();
		this.versions = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_VERSIONS, manifest));
	}

	@Override
	public TakamakaClassLoader classloaderFor(TransactionReference jar) throws ClassNotFoundException, TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		var ws = new ArrayList<TransactionReference>();
		var seen = new HashSet<TransactionReference>();
		var jars = new ArrayList<byte[]>();
		ws.add(jar);
		seen.add(jar);

		do {
			TransactionReference current = ws.remove(ws.size() - 1);
			var request = (GenericJarStoreTransactionRequest<?>) node.getRequest(current);
			jars.add(request.getJar());
			request.getDependencies().filter(seen::add).forEachOrdered(ws::add);
		}
		while (!ws.isEmpty());

		long verificationVersion = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_VERIFICATION_VERSION, versions))).getValue();

		return TakamakaClassLoaders.of(jars.stream(), verificationVersion);
	}
}