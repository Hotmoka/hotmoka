package io.hotmoka.network.thin.client.models.signatures

class FieldSignature(val name: String, val type: String, definingClass: String): Signature(definingClass)