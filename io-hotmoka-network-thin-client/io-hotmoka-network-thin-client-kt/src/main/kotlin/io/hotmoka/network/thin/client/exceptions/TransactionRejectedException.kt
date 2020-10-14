package io.hotmoka.network.thin.client.exceptions

import java.lang.RuntimeException


class TransactionRejectedException(message: String): RuntimeException(message)