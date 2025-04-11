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
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
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
	 * Creates the helper class for building class loaders for jars installed in the given node.
	 * 
	 * @param node the node
	 */
	public ClassLoaderHelperImpl(Node node) {
		this.node = node;
	}

	@Override
	public TakamakaClassLoader classloaderFor(TransactionReference jar) throws NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
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
				throw new NodeException("A jar dependency in store is not for a jar installation request");
		}

		try {
			return TakamakaClassLoaders.of(jars.stream(), node.getConfig().getVerificationVersion());
		}
		catch (UnsupportedVerificationVersionException e) {
			throw new NodeException(e);
		}
		catch (io.hotmoka.verification.api.UnknownTypeException e) {
			// when jar was installed in the store of the node, a classloader was created without problems,
			// hence the classes of the Takamaka runtime were accessible from its classpath;
			// therefore, it is impossible that this is not true anymore now
			throw new NodeException(e);
		}
	}
}