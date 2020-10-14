package io.hotmoka.network.thin.client.models.signatures

abstract class CodeSignatureModel(
        val formals: List<String>,
        definingClass: String
): SignatureModel(definingClass)