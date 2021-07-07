/*
Copyright 2021 Dinu Berinde and Fausto Spoto

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

package io.hotmoka.network.responses;

import java.util.List;
import java.util.stream.Collectors;

import io.hotmoka.beans.responses.JarStoreNonInitialTransactionResponse;
import io.hotmoka.network.updates.UpdateModel;

public abstract class JarStoreTransactionResponseModel extends TransactionResponseModel {

    /**
     * The updates resulting from the execution of the transaction.
     */
    public List<UpdateModel> updates;

    /**
     * The amount of gas consumed by the transaction for CPU execution.
     */
    public String gasConsumedForCPU;

    /**
     * The amount of gas consumed by the transaction for RAM allocation.
     */
    public String gasConsumedForRAM;

    /**
     * The amount of gas consumed by the transaction for storage consumption.
     */
    public String gasConsumedForStorage;

    protected JarStoreTransactionResponseModel(JarStoreNonInitialTransactionResponse response) {
       this.updates = response.getUpdates().map(UpdateModel::new).collect(Collectors.toList());
       this.gasConsumedForCPU = response.gasConsumedForCPU.toString();
       this.gasConsumedForRAM = response.gasConsumedForRAM.toString();
       this.gasConsumedForStorage = response.gasConsumedForStorage.toString();
    }

    protected JarStoreTransactionResponseModel() {}
}