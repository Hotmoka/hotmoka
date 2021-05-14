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

package io.hotmoka.network.responses;

import java.math.BigInteger;

import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.network.updates.UpdateModel;

public class ConstructorCallTransactionFailedResponseModel extends ConstructorCallTransactionResponseModel {

	/**
     * The amount of gas consumed by the transaction as penalty for the failure.
     */
    public String gasConsumedForPenalty;

    /**
     * The fully-qualified class name of the cause exception.
     */
    public String classNameOfCause;

    /**
     * The message of the cause exception.
     */
    public String messageOfCause;

    /**
     * The program point where the cause exception occurred.
     */
    public String where;

    public ConstructorCallTransactionFailedResponseModel(ConstructorCallTransactionFailedResponse response) {
        super(response);

        this.gasConsumedForPenalty = response.gasConsumedForPenalty().toString();
        this.classNameOfCause = response.classNameOfCause;
        this.messageOfCause = response.messageOfCause;
        this.where = response.where;
    }

    public ConstructorCallTransactionFailedResponseModel() {}

    public ConstructorCallTransactionFailedResponse toBean() {
        return new ConstructorCallTransactionFailedResponse(
        	classNameOfCause,
            messageOfCause,
            where,
            updates.stream().map(UpdateModel::toBean),
            new BigInteger(gasConsumedForCPU),
            new BigInteger(gasConsumedForRAM),
            new BigInteger(gasConsumedForStorage),
            new BigInteger(gasConsumedForPenalty)
        );
    }
}
