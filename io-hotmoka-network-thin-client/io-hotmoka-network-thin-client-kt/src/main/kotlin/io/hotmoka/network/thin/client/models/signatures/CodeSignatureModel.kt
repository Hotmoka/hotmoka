package io.hotmoka.network.thin.client.models.signatures

/**
 * The model of the signature of a method or constructor.
 */
abstract class CodeSignatureModel(
        /**
         * The formal arguments of the method or constructor.
         */
        val formals: List<String>,
        definingClass: String
): SignatureModel(definingClass)