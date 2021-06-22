import {Marshallable} from "../marshalling/Marshallable";
import {MarshallingContext} from "../marshalling/MarshallingContext";
import {Selectors} from "../marshalling/Selectors";
import {HotmokaException} from "../exception/HotmokaException";

export class BasicType extends Marshallable {
    public static readonly BOOLEAN = new BasicType("boolean")
    public static readonly BYTE = new BasicType("byte")
    public static readonly CHAR = new BasicType("char")
    public static readonly SHORT = new BasicType("short")
    public static readonly INT = new BasicType("int")
    public static readonly LONG = new BasicType("long")
    public static readonly FLOAT = new BasicType("float")
    public static readonly DOUBLE = new BasicType("double")

    /**
     * The name of this basic type.
     */
    public readonly name;


    constructor(type: string) {
        super()
        this.name = type
    }


    public into(context: MarshallingContext): void {
        context.writeByte(this.getSelector())
    }

    /**
     * Returns the selector of this basic type.
     * @return the selector
     */
    private getSelector(): number {
        switch (this.name) {
            case BasicType.BOOLEAN.name:
                return Selectors.SELECTOR_BASIC_TYPE_BOOLEAN
            case BasicType.BYTE.name:
                return Selectors.SELECTOR_BASIC_TYPE_BYTE
            case BasicType.CHAR.name:
                return Selectors.SELECTOR_BASIC_TYPE_CHAR
            case BasicType.SHORT.name:
                return Selectors.SELECTOR_BASIC_TYPE_SHORT
            case BasicType.INT.name:
                return Selectors.SELECTOR_BASIC_TYPE_INT
            case BasicType.LONG.name:
                return Selectors.SELECTOR_BASIC_TYPE_LONG
            case BasicType.FLOAT.name:
                return Selectors.SELECTOR_BASIC_TYPE_FLOAT
            case BasicType.DOUBLE.name:
                return Selectors.SELECTOR_BASIC_TYPE_DOUBLE
            default:
                throw new HotmokaException("No selector found for this basic type")
        }
    }

    /**
     * Checks whether a type is a {@link BasicType}
     * @param type the type to check
     * @return true if the type is a BasicType, false otherwis
     */
    public static isBasicType(type: string): boolean {
        return type !== undefined && type !== null &&
            (type === BasicType.BOOLEAN.name ||
                type === BasicType.BYTE.name ||
                type === BasicType.CHAR.name ||
                type === BasicType.SHORT.name ||
                type === BasicType.INT.name ||
                type === BasicType.LONG.name ||
                type === BasicType.FLOAT.name ||
                type === BasicType.DOUBLE.name)
    }
}