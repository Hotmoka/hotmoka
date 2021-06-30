import {StorageReferenceModel} from "./StorageReferenceModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {BasicType} from "../../internal/lang/BasicType";
import {ClassType} from "../../internal/lang/ClassType";
import {Selectors} from "../../internal/marshalling/Selectors";
import {HotmokaException} from "../../internal/exception/HotmokaException";

/**
 * The model of a storage value.
 */
export class StorageValueModel {
    /**
     * Used for primitive values, big integers, strings and null.
     * For the null value, this field holds exactly null, not the string "null".
     */
    value?: string

    /**
     * Used for storage references.
     */
    reference?: StorageReferenceModel

    /**
     * The type of the value. For storage references and {@code null}, this is {@code "reference"}.
     */
    type?: string

    /**
     * Used for enumeration values only: it is the name of the element in the enumeration.
     */
    enumElementName?: string

    constructor(value?: string,
                reference?: StorageReferenceModel,
                type?: string,
                enumElementName?: string
    ) {

        if (type === null || type === undefined) {
            throw new HotmokaException("Unexpected value type " + type)
        }

        this.value = value
        this.reference = reference
        this.type = type
        this.enumElementName = enumElementName
    }

    /**
     * Yields a storage value.
     * @param value the value
     * @param type the type of the value
     */
    public static newStorageValue(value: string, type: string): StorageValueModel {
        return new StorageValueModel(value, undefined, type, undefined)
    }

    /**
     * Yields a reference storage value.
     * @param reference the reference
     */
    public static newReference(reference: StorageReferenceModel): StorageValueModel {
        return new StorageValueModel(undefined, reference, "reference", undefined)
    }

    /**
     * Yields an enum storage value.
     * @param enumElementName the enum name
     * @param type the type of enum
     */
    public static newEnum(enumElementName: string, type: string): StorageValueModel {
        return new StorageValueModel(undefined, undefined, type, enumElementName)
    }

    public static into(context: MarshallingContext, storageValue: StorageValueModel): void {
        if (storageValue.type === null || storageValue.type === undefined) {
            throw new HotmokaException("Unexpected value type " + storageValue.type)
        }

        if (storageValue.enumElementName !== null && storageValue.enumElementName !== undefined) {
            this.writeEnum(context, storageValue)
        } else if (storageValue.type === "reference") {
            if (storageValue.reference) {
                StorageReferenceModel.into(context, storageValue.reference)
            } else {
                this.writeNull(context)
            }
        } else if (storageValue.value === null || storageValue.value === undefined) {
            throw new HotmokaException("Unexpected value " + storageValue.value)
        } else if (BasicType.isBasicType(storageValue.type)) {
            StorageValueModel.writeBasicType(context, storageValue)
        } else if (storageValue.type === ClassType.STRING.name) {
            StorageValueModel.writeString(context, storageValue)
        } else if (storageValue.type === ClassType.BIG_INTEGER.name) {
            StorageValueModel.writeBigInteger(context, storageValue)
        } else {
            throw new HotmokaException("Unexpected value type " + storageValue.type)
        }
    }

    /**
     * Marshals an array of storage values into a given stream.
     * @param storageValues the array of storage values
     * @param context the context holding the stream
     */
    public static intoArray(storageValues: Array<StorageValueModel>, context: MarshallingContext): void {
        context.writeCompactInt(storageValues.length)
        storageValues.forEach(storageValue => StorageValueModel.into(context, storageValue))
    }

    private static writeBasicType(context: MarshallingContext, storageValue: StorageValueModel): void {

        if (storageValue.value === null || storageValue.value === undefined) {
            throw new HotmokaException("Unexpected value " + storageValue.value)
        }

        switch (storageValue.type) {
            case BasicType.BOOLEAN.name:
                if (storageValue.value === "true") {
                    context.writeByte(Selectors.SELECTOR_BOOLEAN_TRUE_VALUE)
                } else if (storageValue.value === "false") {
                    context.writeByte(Selectors.SELECTOR_BOOLEAN_FALSE_VALUE)
                } else {
                    throw new HotmokaException("Unexpected boolean value " + storageValue.value)
                }
                break

            case BasicType.BYTE.name:
                context.writeByte(Selectors.SELECTOR_BYTE_VALUE)
                context.writeByte(Number(storageValue.value))
                break

            case BasicType.CHAR.name:
                context.writeByte(Selectors.SELECTOR_CHAR_VALUE)
                context.writeChar(storageValue.value)
                break

            case BasicType.SHORT.name:
                context.writeByte(Selectors.SELECTOR_SHORT_VALUE)
                context.writeShort(Number(storageValue.value))
                break

            case BasicType.INT.name: {
                const intVal = Number(storageValue.value)

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
                context.writeLong(Number(storageValue.value))
                break

            case BasicType.FLOAT.name:
                context.writeByte(Selectors.SELECTOR_FLOAT_VALUE)
                context.writeFloat(Number(storageValue.value))
                break

            case BasicType.DOUBLE.name:
                context.writeByte(Selectors.SELECTOR_DOUBLE_VALUE)
                context.writeDouble(Number(storageValue.value))
                break

            default: throw new HotmokaException("Unexpected type " + storageValue.type)
        }
    }


    private static writeString(context: MarshallingContext, storageValue: StorageValueModel): void {
        if (storageValue.value === null || storageValue.value === undefined) {
            throw new HotmokaException("Unexpected value " + storageValue.value)
        }

        if (storageValue.value === "") {
            context.writeByte(Selectors.SELECTOR_EMPTY_STRING_VALUE)
        } else {
            context.writeByte(Selectors.SELECTOR_STRING_VALUE)
            context.writeString(storageValue.value)
        }
    }

    private static writeBigInteger(context: MarshallingContext, storageValue: StorageValueModel): void {
        if (storageValue.value === null || storageValue.value === undefined) {
            throw new HotmokaException("Unexpected value " + storageValue.value)
        }

        context.writeByte(Selectors.SELECTOR_BIG_INTEGER_VALUE)
        context.writeBigInteger(storageValue.value)
    }

    private static writeNull(context: MarshallingContext): void {
        context.writeByte(Selectors.SELECTOR_NULL_REFERENCE)
    }

    private static writeEnum(context: MarshallingContext, storageValue: StorageValueModel): void {
        if (storageValue.enumElementName === null || storageValue.enumElementName === undefined) {
            throw new HotmokaException("Unexpected enum " + storageValue.enumElementName)
        }

        if (storageValue.type === null || storageValue.type === undefined) {
            throw new HotmokaException("Unexpected type " + storageValue.type)
        }

        context.writeByte(Selectors.SELECTOR_ENUM_VALUE)
        context.writeString(storageValue.enumElementName)
        context.writeString(storageValue.type)
    }
}