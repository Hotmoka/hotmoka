package io.hotmoka.network.thin.client.models.errors

/**
 * The model of an exception thrown by an endpoint method.
 */
class ErrorModel(
        /**
         * The message of the exception.
         */
        val message: String,
        /**
         * The fully-qualified name of the class of the exception.
         */
        val exceptionClassName: String
)