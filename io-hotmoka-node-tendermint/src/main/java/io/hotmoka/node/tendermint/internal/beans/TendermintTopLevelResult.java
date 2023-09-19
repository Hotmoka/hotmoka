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

package io.hotmoka.node.tendermint.internal.beans;

/**
 * A bean corresponding to the top level result of a query about a transaction.
 */
public class TendermintTopLevelResult {

	/**
	 * Creates the bean.
	 */
	public TendermintTopLevelResult() {}

	/**
	 * The height of the block.
	 */
	public long height;

	/**
	 * The hash of the trasaction.
	 */
	public String hash;

	/**
	 * The result of checking the transaction.
	 */
	public Object check_tx;

	/**
	 * The result of delivering the transaction.
	 */
	public Object deliver_tx;

	/**
	 * The result of the transaction.
	 */
	public TendermintTxResult tx_result;

	/**
	 * The trasaction data.
	 */
	public String tx;
}