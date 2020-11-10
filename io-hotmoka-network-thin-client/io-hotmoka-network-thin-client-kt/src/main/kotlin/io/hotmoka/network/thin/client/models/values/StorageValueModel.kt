package io.hotmoka.network.thin.client.models.values

class StorageValueModel(
        /**
         * The type of the value. For storage references and {@code null}, this is {@code "reference"}.
         */
        val type: String,
        /**
         * Used for primitive values, big integers, strings and null.
         * For the null value, this field holds exactly null, not the string "null".
         */
        val value: String? = null,
        /**
         * Used for storage references.
         */
        val reference: StorageReferenceModel? = null,
        /**
         * Used for enumeration values only: it is the name of the element in the enumeration.
         */
        val enumElementName: String? = null
)