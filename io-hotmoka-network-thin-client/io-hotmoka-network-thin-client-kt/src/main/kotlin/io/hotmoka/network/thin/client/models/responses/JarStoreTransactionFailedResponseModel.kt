package io.hotmoka.network.thin.client.models.responses

class JarStoreTransactionFailedResponseModel(
    val gasConsumedForPenalty: String,
    val classNameOfCause: String,
    val messageOfCause: String,
    val where: String
)