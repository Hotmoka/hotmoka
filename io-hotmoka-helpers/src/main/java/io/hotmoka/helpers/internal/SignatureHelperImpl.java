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

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.helpers.ClassLoaderHelpers;
import io.hotmoka.helpers.api.ClassLoaderHelper;
import io.hotmoka.helpers.api.SignatureHelper;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.verification.api.TakamakaClassLoader;

/**
 * Implementation of a helper to determine the signature algorithm to use for an externally owned account.
 */
public class SignatureHelperImpl implements SignatureHelper {
	private final Node node;
	private final ClassLoaderHelper classLoaderHelper;

	public SignatureHelperImpl(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		this.node = node;
		this.classLoaderHelper = ClassLoaderHelpers.of(node);
	}

	@Override
	public SignatureAlgorithm signatureAlgorithmFor(StorageReference account) throws NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		var tag = node.getClassTag(account);

		// first we try without the class loader, that does not work under Android...
		if (tag.getClazz().equals(StorageTypes.EOA_ED25519))
			return SignatureAlgorithms.ed25519();
		else if (tag.getClazz().equals(StorageTypes.EOA_SHA256DSA))
			return SignatureAlgorithms.sha256dsa();
		else if (tag.getClazz().equals(StorageTypes.EOA_QTESLA1))
			return SignatureAlgorithms.qtesla1();
		else if (tag.getClazz().equals(StorageTypes.EOA_QTESLA3))
			return SignatureAlgorithms.qtesla3();

		TakamakaClassLoader classLoader = classLoaderHelper.classloaderFor(tag.getJar());
		Class<?> clazz = classLoader.loadClass(tag.getClazz().getName());

		if (classLoader.getAccountED25519().isAssignableFrom(clazz))
			return SignatureAlgorithms.ed25519();
		else if (classLoader.getAccountSHA256DSA().isAssignableFrom(clazz))
			return SignatureAlgorithms.sha256dsa();
		else if (classLoader.getAccountQTESLA1().isAssignableFrom(clazz))
			return SignatureAlgorithms.qtesla1();
		else if (classLoader.getAccountQTESLA3().isAssignableFrom(clazz))
			return SignatureAlgorithms.qtesla3();
		else
			return SignatureAlgorithms.of(node.getConsensusConfig());
	}
}