package io.hotmoka.network.thin.client.internal.exceptions

import io.hotmoka.network.thin.client.internal.models.errors.NodeError


class NetworkException(val error: NodeError) : Exception(error.message)