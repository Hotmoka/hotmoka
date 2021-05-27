import {StorageReferenceModel} from "./StorageReferenceModel";
import {Marshallable} from "../../internal/marshalling/Marshallable";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {BasicType} from "../../internal/lang/BasicType";
import {ClassType} from "../../internal/lang/ClassType";

/**
 * The model of a storage value.
 */
export class StorageValueModel extends Marshallable {
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
        super()
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

    public into(context: MarshallingContext): void {

        if (BasicType.isBasicType(this.type)) {
            if (this.value === null || this.value === undefined) {
                throw new Error("Unexpected null value")
            }
            this.writeBasicType(context)
        } else if (this.type === ClassType.STRING.name) {
            this.writeString(context)
        } else if (this.type === ClassType.BIG_INTEGER.name) {
            this.writeBigInteger(context)
        } else if (this.type === "reference") {
            if (this.reference !== null && this.reference !== undefined) {
                this.reference.into(context)
            } else {
                this.writeNull(context)
            }
        }  else if (this.enumElementName !== null && this.enumElementName !== undefined) {
            this.writeEnum(context)
        } else {
            throw new Error("unexpected value type " + this.type)
        }
    }

    protected intoWithoutSelector(context: MarshallingContext): void {
        // nothing
    }


    private writeBasicType(context: MarshallingContext): void {
        // TODO
    }


    private writeString(context: MarshallingContext): void {
        // TODO
    }

    private writeBigInteger(context: MarshallingContext): void {
        // TODO
    }

    private writeNull(context: MarshallingContext): void {
        // todo
    }

    private writeEnum(context: MarshallingContext): void {
        // TODO
    }
}