package io.hotmoka.network.thin.client.models.requests

class TransactionRequestModel<T>(val type: String, val transactionResponseModel: T)