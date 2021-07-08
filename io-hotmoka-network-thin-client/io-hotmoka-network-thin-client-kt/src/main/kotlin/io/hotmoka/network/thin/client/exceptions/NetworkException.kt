package io.hotmoka.network.thin.client.exceptions

import io.hotmoka.network.thin.client.models.errors.ErrorModel


class NetworkException(val errorModel: ErrorModel) : Exception(errorModel.message)