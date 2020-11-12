package io.hotmoka.network.thin.client.exceptions

class TransactionException(message: String): RuntimeException(message) {

    /**
     * Builds an exception that didn't occur during the execution of a Takamaka constructor or method,
     * or that did well occur inside it, but the constructor or method wasn't allowed to throw it.
     *
     * @param classNameOfCause the name of the class of the cause of the exception
     * @param messageOfCause the message of the cause of the exception. This might be {@code null}
     * @param where a description of the program point of the exception. This might be {@code null}
     */
    constructor(classNameOfCause: String, messageOfCause: String, where: String): this(
        classNameOfCause
                + (if (messageOfCause.isEmpty()) "" else ": $messageOfCause")
                + if (where.isEmpty()) "" else "@$where"
    )
}