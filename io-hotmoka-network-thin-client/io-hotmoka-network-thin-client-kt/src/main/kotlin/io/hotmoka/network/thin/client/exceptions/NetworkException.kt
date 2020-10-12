package io.hotmoka.network.thin.client.exceptions

import io.hotmoka.network.thin.client.models.errors.NodeError


class NetworkException(val error: NodeError) : Exception(error.message)