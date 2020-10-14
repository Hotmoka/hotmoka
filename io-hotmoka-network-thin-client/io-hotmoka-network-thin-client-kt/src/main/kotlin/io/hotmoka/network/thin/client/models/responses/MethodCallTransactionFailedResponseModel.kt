package io.hotmoka.network.thin.client.models.responses

class MethodCallTransactionFailedResponseModel(
        val gasConsumedForPenalty: String,
        val classNameOfCause: String,
        val messageOfCause: String
)