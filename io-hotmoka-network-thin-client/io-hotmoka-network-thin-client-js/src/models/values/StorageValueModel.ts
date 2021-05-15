import {StorageReferenceModel} from "./StorageReferenceModel";

/**
 * The model of a storage value.
 */
export class StorageValueModel {
    /**
     * Used for primitive values, big integers, strings and null.
     * For the null value, this field holds exactly null, not the string "null".
     */
    value: string

    /**
     * Used for storage references.
     */
    reference: StorageReferenceModel

    /**
     * The type of the value. For storage references and {@code null}, this is {@code "reference"}.
     */
    type: string

    /**
     * Used for enumeration values only: it is the name of the element in the enumeration.
     */
    enumElementName: string

    constructor(value: string,
                referene: StorageReferenceModel,
                type: string,
                enumElementName: string
    ) {
        this.value = value
        this.reference = referene
        this.type = type
        this.enumElementName = enumElementName
    }
}