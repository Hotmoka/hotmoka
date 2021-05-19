import {StorageReferenceModel} from "./StorageReferenceModel";

/**
 * The model of a storage value.
 */
export class StorageValueModel {
    /**
     * Used for primitive values, big integers, strings and null.
     * For the null value, this field holds exactly null, not the string "null".
     */
    value: string | null

    /**
     * Used for storage references.
     */
    reference: StorageReferenceModel | null

    /**
     * The type of the value. For storage references and {@code null}, this is {@code "reference"}.
     */
    type: string

    /**
     * Used for enumeration values only: it is the name of the element in the enumeration.
     */
    enumElementName: string | null

    constructor(value: string | null,
                reference: StorageReferenceModel | null,
                type: string,
                enumElementName: string | null
    ) {
        this.value = value
        this.reference = reference
        this.type = type
        this.enumElementName = enumElementName
    }

    public static newStorageValue(value: string, type: string): StorageValueModel {
        return new StorageValueModel(value, null, type, null)
    }

    public static newReference(reference: StorageReferenceModel): StorageValueModel {
        return new StorageValueModel(null, reference, "reference", null)
    }

    public static newEnum(enumElementName: string, type: string): StorageValueModel {
        return new StorageValueModel(null, null, type, enumElementName)
    }
}