package io.hotmoka.network.thin.client.exceptions

class CodeExecutionException(message: String): RuntimeException(message) {

    /**
     * Builds the wrapper of an exception that occurred during the execution
     * of a Takamaka constructor or method that was allowed to throw it.
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