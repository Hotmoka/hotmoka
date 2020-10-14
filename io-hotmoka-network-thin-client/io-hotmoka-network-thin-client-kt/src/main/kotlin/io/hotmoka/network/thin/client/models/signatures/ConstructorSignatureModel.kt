package io.hotmoka.network.thin.client.models.signatures

class ConstructorSignatureModel(
        formals: List<String>,
        definingClass: String
): CodeSignatureModel(formals, definingClass)