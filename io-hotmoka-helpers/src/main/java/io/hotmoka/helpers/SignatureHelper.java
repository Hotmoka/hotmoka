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

package io.hotmoka.helpers;

import java.security.NoSuchAlgorithmException;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.SignatureAlgorithmForTransactionRequests;
import io.hotmoka.verification.api.TakamakaClassLoader;

/**
 * An helper to determine the signature algorithm to use for an externally owned account.
 */
public class SignatureHelper {
	private final Node node;
	private final ClassLoaderHelper classLoaderHelper;

	public SignatureHelper(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		this.node = node;
		this.classLoaderHelper = new ClassLoaderHelper(node);
	}

	/**
	 * Yields the signature algorithm to use for signing transactions on behalf of the given account.
	 * 
	 * @param account the account
	 * @return the algorithm
	 */
	public SignatureAlgorithm<SignedTransactionRequest> signatureAlgorithmFor(StorageReference account) throws NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, ClassNotFoundException {
		ClassTag tag = node.getClassTag(account);

		// first we try without the class loader, that does not work under Android...
		if (tag.clazz.equals(ClassType.EOA_ED25519))
			return SignatureAlgorithmForTransactionRequests.ed25519();
		else if (tag.clazz.equals(ClassType.EOA_SHA256DSA))
			return SignatureAlgorithmForTransactionRequests.sha256dsa();
		else if (tag.clazz.equals(ClassType.EOA_QTESLA1))
			return SignatureAlgorithmForTransactionRequests.qtesla1();
		else if (tag.clazz.equals(ClassType.EOA_QTESLA3))
			return SignatureAlgorithmForTransactionRequests.qtesla3();

		TakamakaClassLoader classLoader = classLoaderHelper.classloaderFor(tag.jar);
		Class<?> clazz = classLoader.loadClass(tag.clazz.name);

		if (classLoader.getAccountED25519().isAssignableFrom(clazz))
			return SignatureAlgorithmForTransactionRequests.ed25519();
		else if (classLoader.getAccountSHA256DSA().isAssignableFrom(clazz))
			return SignatureAlgorithmForTransactionRequests.sha256dsa();
		else if (classLoader.getAccountQTESLA1().isAssignableFrom(clazz))
			return SignatureAlgorithmForTransactionRequests.qtesla1();
		else if (classLoader.getAccountQTESLA3().isAssignableFrom(clazz))
			return SignatureAlgorithmForTransactionRequests.qtesla3();
		else
			return SignatureAlgorithmForTransactionRequests.mk(node.getNameOfSignatureAlgorithmForRequests());
	}
}