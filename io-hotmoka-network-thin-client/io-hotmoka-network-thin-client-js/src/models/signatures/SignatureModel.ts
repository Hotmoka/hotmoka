/**
 * The model of the signature of a field, method or constructor.
 */
import {Marshallable} from "../../internal/marshalling/Marshallable";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {ClassType} from "./ClassType";

export abstract class SignatureModel extends Marshallable {
    /**
     * The name of the class defining the field, method or constructor.
     */
    definingClass: string

    protected constructor(definingClass: string) {
        super()
        this.definingClass = definingClass
    }

    private equals(classType: ClassType): boolean {
        return classType.name === this.definingClass
    }

    protected into(context: MarshallingContext): void {
        if (this.equals(ClassType.BIG_INTEGER))
            context.writeByte(26)
    }

    protected intoWithoutSelector(context: MarshallingContext): void {
        // nothing
    }
}