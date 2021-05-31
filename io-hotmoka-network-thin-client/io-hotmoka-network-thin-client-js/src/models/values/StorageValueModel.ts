import {StorageReferenceModel} from "./StorageReferenceModel";
import {Marshallable} from "../../internal/marshalling/Marshallable";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {BasicType} from "../../internal/lang/BasicType";
import {ClassType} from "../../internal/lang/ClassType";
import {Selectors} from "../../internal/marshalling/Selectors";

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

        if (this.enumElementName != null) {
            this.writeEnum(context)
        } else if (this.type === "reference") {
            if (this.reference !== null) {
                this.reference.into(context)
            } else {
                this.writeNull(context)
            }
        } else if (this.value == null) {
            throw new Error("Unexpected null value")
        } else if (BasicType.isBasicType(this.type)) {
            this.writeBasicType(context)
        } else if (this.type === ClassType.STRING.name) {
            this.writeString(context)
        } else if (this.type === ClassType.BIG_INTEGER.name) {
            this.writeBigInteger(context)
        } else {
            throw new Error("unexpected value type " + this.type)
        }
    }

    private writeBasicType(context: MarshallingContext): void {

        if (this.value == null) {
            throw new Error("Unexpected null value")
        }

        switch (this.type) {
            case BasicType.BOOLEAN.name:
                if (this.value === "true") {
                    context.writeByte(Selectors.SELECTOR_BOOLEAN_TRUE_VALUE)
                } else if (this.value === "false") {
                    context.writeByte(Selectors.SELECTOR_BOOLEAN_FALSE_VALUE)
                } else {
                    throw new Error("Unexpected booleab value")
                }
                break

            case BasicType.BYTE.name:
                context.writeByte(Selectors.SELECTOR_BYTE_VALUE)
                context.writeByte(Number(this.value))
                break

            case BasicType.CHAR.name:
                context.writeByte(Selectors.SELECTOR_CHAR_VALUE)
                context.writeChar(this.value)
                break

            case BasicType.SHORT.name:
                context.writeByte(Selectors.SELECTOR_SHORT_VALUE)
                context.writeShort(Number(this.value))
                break

            case BasicType.INT.name: {
                const intVal = Number(this.value)

                if (intVal >= 0 && intVal < 255 - Selectors.SELECTOR_INT_VALUE) {
                    context.writeByte(Selectors.SELECTOR_INT_VALUE + 1 + intVal)
                } else {
                    context.writeByte(Selectors.SELECTOR_INT_VALUE)
                    context.writeInt(intVal)
                }
                break
            }

            case BasicType.LONG.name:
                context.writeByte(Selectors.SELECTOR_LONG_VALUE)
                context.writeLong(Number(this.value))
                break

            case BasicType.FLOAT.name:
                context.writeByte(Selectors.SELECTOR_FLOAT_VALUE)
                context.writeFloat(Number(this.value))
                break

            case BasicType.DOUBLE.name:
                context.writeByte(Selectors.SELECTOR_DOUBLE_VALUE)
                context.writeDouble(Number(this.value))
                break

            default: throw new Error("Unexpected type " + this.type)
        }
    }


    private writeString(context: MarshallingContext): void {
        if (this.value == null) {
            throw new Error("Unexpected null value")
        }

        if (this.value === "") {
            context.writeByte(Selectors.SELECTOR_EMPTY_STRING_VALUE)
        } else {
            context.writeByte(Selectors.SELECTOR_STRING_VALUE)
            context.writeString(this.value)
        }
    }

    private writeBigInteger(context: MarshallingContext): void {
        if (this.value == null) {
            throw new Error("Unexpected null value")
        }

        context.writeByte(Selectors.SELECTOR_BIG_INTEGER_VALUE)
        context.writeBigInteger(Number(this.value))
    }

    private writeNull(context: MarshallingContext): void {
        context.writeByte(Selectors.SELECTOR_NULL_REFERENCE)
    }

    private writeEnum(context: MarshallingContext): void {
        if (this.enumElementName == null) {
            throw new Error("Unexpected null enum")
        }

        context.writeByte(Selectors.SELECTOR_ENUM_VALUE)
        context.writeString(this.enumElementName)
        context.writeString(this.type)
    }
}