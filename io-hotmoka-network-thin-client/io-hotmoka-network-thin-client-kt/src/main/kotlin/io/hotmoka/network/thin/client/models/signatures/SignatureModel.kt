package io.hotmoka.network.thin.client.models.signatures

/**
 * The model of the signature of a field, method or constructor.
 */
abstract class SignatureModel(
        /**
         * The name of the class defining the field, method or constructor.
         */
        val definingClass: String
)