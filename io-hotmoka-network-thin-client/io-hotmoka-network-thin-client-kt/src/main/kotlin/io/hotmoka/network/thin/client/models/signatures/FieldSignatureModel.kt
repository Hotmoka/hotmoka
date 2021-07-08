package io.hotmoka.network.thin.client.models.signatures

/**
 * The model of the signature of a field of a class.
 */
class FieldSignatureModel(
        /**
         * The name of the field.
         */
        val name: String,
        /**
         * The type of the field.
         */
        val type: String,
        definingClass: String
): SignatureModel(definingClass)