package io.hotmoka.network.thin.client.models.signatures

class MethodSignatureModel(
        val methodName: String,
        val returnType: String,
        formals: List<String>,
        definingClass: String
): CodeSignatureModel(formals, definingClass)