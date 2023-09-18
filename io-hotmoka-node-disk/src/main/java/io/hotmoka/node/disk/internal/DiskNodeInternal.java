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

package io.hotmoka.node.disk.internal;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.node.disk.DiskNodeConfig;

/**
 * The view of the disk node that is used in the implementation of the module.
 * This allows one to enlarge the visibility of some methods, only for the implementation classes.
 */
public interface DiskNodeInternal {

	/**
	 * Yields the configuration of this node.
	 * 
	 * @return the configuration
	 */
	DiskNodeConfig getConfig();

	/**
	 * Checks that the given transaction request is valid.
	 * 
	 * @param request the request
	 * @throws TransactionRejectedException if the request is not valid
	 */
	void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException;

	/**
	 * Builds a response for the given request and adds it to the store of the node.
	 * 
	 * @param request the request
	 * @return the response; if this node has a notion of commit, this response is typically
	 *         still uncommitted
	 * @throws TransactionRejectedException if the response cannot be built
	 */
	TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException;

	/**
	 * Rewards the validators with the cost of the gas consumed by the
	 * transactions in the last block. This is meaningful only if the
	 * node has some form of commit.
	 * 
	 * @param behaving the space-separated sequence of identifiers of the
	 *                 validators that behaved correctly during the creation
	 *                 of the last block
	 * @param misbehaving the space-separated sequence of the identifiers that
	 *                    misbehaved during the creation of the last block
	 * @return true if and only if rewarding was performed; rewarding might not be
	 *         performed because the manifest is not yet installed or because
	 *         the code of the validators contract failed
	 */
	boolean rewardValidators(String behaving, String misbehaving);
}