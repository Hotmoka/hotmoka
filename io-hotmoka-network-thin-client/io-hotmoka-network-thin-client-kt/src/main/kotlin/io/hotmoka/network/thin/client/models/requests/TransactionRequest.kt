package io.hotmoka.network.thin.client.models.requests

class TransactionRequest<T>(val type: String, val transactionResponseModel: T)