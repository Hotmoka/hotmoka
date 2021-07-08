package io.hotmoka.network.thin.client.models.signatures

/**
 * The model of the signature of a constructor of a class.
 */
class ConstructorSignatureModel(
        formals: List<String>,
        definingClass: String
): CodeSignatureModel(formals, definingClass)