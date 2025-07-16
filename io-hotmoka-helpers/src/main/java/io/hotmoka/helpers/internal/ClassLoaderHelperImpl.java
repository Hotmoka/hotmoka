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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;

import io.hotmoka.helpers.api.ClassLoaderHelper;
import io.hotmoka.helpers.api.MisbehavingNodeException;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.GenericJarStoreTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;

/**
 * Implementation of a helper object for building class loaders for the jar installed at a given
 * transaction reference inside a node.
 */
public class ClassLoaderHelperImpl implements ClassLoaderHelper {

	/**
	 * The node for which the jars get created.
	 */
	private final Node node;

	/**
	 * The verification version of {@code node}.
	 */
	private final long verificationVersion;

	/**
	 * Creates the helper class for building class loaders for jars installed in the given node.
	 * 
	 * @param node the node
	 * @throws InterruptedException if the current thread gets interrupted
	 * @throws TimeoutException if the operation times out
	 * @throws ClosedNodeException if {@code node} is already closed
	 */
	public ClassLoaderHelperImpl(Node node) throws ClosedNodeException, TimeoutException, InterruptedException {
		this.node = node;
		this.verificationVersion = node.getConfig().getVerificationVersion();
	}

	@Override
	public TakamakaClassLoader classloaderFor(TransactionReference jar) throws TimeoutException, InterruptedException, UnknownReferenceException, UnsupportedVerificationVersionException, ClosedNodeException, MisbehavingNodeException {
		var ws = new ArrayList<TransactionReference>();
		var seen = new HashSet<TransactionReference>();
		var jars = new ArrayList<byte[]>();

		var request = node.getRequest(jar);
		if (request instanceof GenericJarStoreTransactionRequest<?> gjstr) {
			jars.add(gjstr.getJar());
			seen.add(jar);
			gjstr.getDependencies().filter(seen::add).forEachOrdered(ws::add);
		}
		else
			throw new UnknownReferenceException("The transaction " + jar + " is not for a jar installation request");

		while (!ws.isEmpty()) {
			TransactionReference current = ws.remove(ws.size() - 1);
			request = node.getRequest(current);
			if (request instanceof GenericJarStoreTransactionRequest<?> gjstr2) {
				jars.add(gjstr2.getJar());
				gjstr2.getDependencies().filter(seen::add).forEachOrdered(ws::add);
			}
			else
				throw new MisbehavingNodeException("A jar dependency in store is not for a jar installation request");
		}

		try {
			return TakamakaClassLoaders.of(jars.stream(), verificationVersion);
		}
		catch (io.hotmoka.verification.api.UnknownTypeException e) {
			// when jar was installed in the store of the node, a classloader was created without problems,
			// hence the classes of the Takamaka runtime were accessible from its classpath;
			// therefore, it is impossible that this is not true anymore now, which means that the
			// node is misbehaving
			throw new MisbehavingNodeException(e);
		}
	}
}