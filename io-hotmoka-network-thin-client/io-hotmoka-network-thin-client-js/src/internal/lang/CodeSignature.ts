import {VoidMethodSignatureModel} from "../../models/signatures/VoidMethodSignatureModel";
import {ClassType} from "./ClassType";
import {BasicType} from "./BasicType";
import {NonVoidMethodSignatureModel} from "../../models/signatures/NonVoidMethodSignatureModel";

export class CodeSignature {
    /**
     * The method {@code receive} of a payable contract, with an int argument.
     */
    public static readonly RECEIVE_INT = new VoidMethodSignatureModel("receive", ClassType.PAYABLE_CONTRACT.name, [BasicType.INT.name])

    /**
     * The method {@code receive} of a payable contract, with a long argument.
     */
    public static readonly RECEIVE_LONG = new VoidMethodSignatureModel("receive", ClassType.PAYABLE_CONTRACT.name, [BasicType.LONG.name])

    /**
     * The method {@code receive} of a payable contract, with a big integer argument.
     */
    public static readonly RECEIVE_BIG_INTEGER = new VoidMethodSignatureModel("receive", ClassType.PAYABLE_CONTRACT.name, [ClassType.BIG_INTEGER.name])

    /**
     * The method {@code getGamete} of the manifest.
     */
    public static readonly GET_GAMETE = new NonVoidMethodSignatureModel("getGamete", ClassType.MANIFEST.name, [], ClassType.ACCOUNT.name)

}