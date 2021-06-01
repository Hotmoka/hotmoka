import {MethodSignatureModel} from "./MethodSignatureModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {ClassType} from "../../internal/lang/ClassType";
import {Selectors} from "../../internal/marshalling/Selectors";

export class VoidMethodSignatureModel extends MethodSignatureModel {
    /**
     * The method {@code reward} of the validators contract.
     */
    public static readonly VALIDATORS_REWARD = new VoidMethodSignatureModel(ClassType.VALIDATORS.name, "reward", [ClassType.BIG_INTEGER.name, ClassType.STRING.name, ClassType.STRING.name, ClassType.BIG_INTEGER.name, ClassType.BIG_INTEGER.name])


    public equals(other: any): boolean {
        return other instanceof VoidMethodSignatureModel && super.equals(other)
    }

    public into(context: MarshallingContext): void {
        if (this.equals(VoidMethodSignatureModel.VALIDATORS_REWARD)) {
            context.writeByte(Selectors.SELECTOR_REWARD)
        } else {
            context.writeByte(Selectors.SELECTOR_VOID_METHOD)
            super.into(context)
        }
    }
}