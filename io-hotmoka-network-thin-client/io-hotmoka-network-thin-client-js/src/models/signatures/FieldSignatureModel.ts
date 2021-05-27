import {SignatureModel} from "./SignatureModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {ClassType} from "../../internal/lang/ClassType";
import {BasicType} from "../../internal/lang/BasicType";

/**
 * The model of the signature of a field of a class.
 */
export class FieldSignatureModel extends SignatureModel {

    /**
     * The name of the field.
     */
    name: string

    /**
     * The type of the field.
     */
    type: string

    constructor(name: string, type: string, definingClass: string) {
        super(definingClass)
        this.name = name
        this.type = type
    }

    protected intoWithoutSelector(context: MarshallingContext): void {
        // nothing
    }

    public into(context: MarshallingContext): void {
        super.into(context)
        context.writeString(this.name)

        if (BasicType.isBasicType(this.type)) {
            new BasicType(this.type).into(context)
        } else {
            new ClassType(this.type).into(context)
        }
    }
}