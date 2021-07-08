package io.hotmoka.network.thin.client.models.signatures

/**
 * The model of the signature of a method of a class.
 */
class MethodSignatureModel(
        /**
         * The name of the method.
         */
        val methodName: String,
        /**
         * The return type of the method, null if it's void.
         */
        val returnType: String?,
        formals: List<String>,
        definingClass: String
): CodeSignatureModel(formals, definingClass)