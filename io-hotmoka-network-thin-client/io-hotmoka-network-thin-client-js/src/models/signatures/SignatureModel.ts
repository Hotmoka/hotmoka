/**
 * The model of the signature of a field, method or constructor.
 */
import {Marshallable} from "../../internal/marshalling/Marshallable";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {ClassType} from "./ClassType";
import {Selectors} from "../../internal/marshalling/Selectors";
import {Constants} from "../Constants";

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
            context.writeByte(Selectors.SELECTOR_BIGINTEGER);
        else if (this.equals(ClassType.UNSIGNED_BIG_INTEGER))
            context.writeByte(Selectors.SELECTOR_UNSIGNED_BIG_INTEGER);
        else if (this.equals(ClassType.GAS_PRICE_UPDATE))
            context.writeByte(Selectors.SELECTOR_GAS_PRICE_UPDATE);
        else if (this.equals(ClassType.ERC20))
            context.writeByte(Selectors.SELECTOR_ERC20);
        else if (this.equals(ClassType.IERC20))
            context.writeByte(Selectors.SELECTOR_IERC20);
        else if (this.equals(ClassType.STRING))
            context.writeByte(Selectors.SELECTOR_STRING);
        else if (this.equals(ClassType.ACCOUNT))
            context.writeByte(Selectors.SELECTOR_ACCOUNT);
        else if (this.equals(ClassType.MANIFEST))
            context.writeByte(Selectors.SELECTOR_MANIFEST);
        else if (this.equals(ClassType.GAS_STATION))
            context.writeByte(Selectors.SELECTOR_GAS_STATION);
        else if (this.equals(ClassType.STORAGE_TREE_ARRAY))
            context.writeByte(Selectors.SELECTOR_STORAGE_TREE_ARRAY);
        else if (this.equals(ClassType.STORAGE_TREE_ARRAY_NODE))
            context.writeByte(Selectors.SELECTOR_STORAGE_TREE_ARRAY_NODE);
        else if (this.equals(ClassType.OBJECT))
            context.writeByte(Selectors.SELECTOR_OBJECT);
        else if (this.equals(ClassType.CONTRACT))
            context.writeByte(Selectors.SELECTOR_CONTRACT);
        else if (this.equals(ClassType.STORAGE))
            context.writeByte(Selectors.SELECTOR_STORAGE);
        else if (this.equals(ClassType.PAYABLE_CONTRACT))
            context.writeByte(Selectors.SELECTOR_PAYABLE_CONTRACT);
        else if (this.definingClass === Constants.STORAGE_MAP_VIEW_NAME)
            context.writeByte(Selectors.SELECTOR_STORAGE_MAP);
        else if (this.equals(ClassType.STORAGE_TREE_MAP))
            context.writeByte(Selectors.SELECTOR_STORAGE_TREE_MAP);
        else if (this.equals(ClassType.STORAGE_TREE_MAP_BLACK_NODE))
            context.writeByte(Selectors.SELECTOR_STORAGE_TREE_MAP_BLACK_NODE);
        else if (this.equals(ClassType.STORAGE_TREE_MAP_RED_NODE))
            context.writeByte(Selectors.SELECTOR_STORAGE_TREE_MAP_RED_NODE);
        else if (this.equals(ClassType.STORAGE_TREE_INTMAP_NODE))
            context.writeByte(Selectors.SELECTOR_STORAGE_TREE_INTMAP_NODE);
        else if (this.equals(ClassType.STORAGE_TREE_SET))
            context.writeByte(Selectors.SELECTOR_STORAGE_TREE_SET);
        else if (this.definingClass === Constants.STORAGE_LIST_VIEW_NAME)
            context.writeByte(Selectors.SELECTOR_STORAGE_LIST);
        else if (this.definingClass === Constants.STORAGE_TREE_MAP_NODE_NAME)
            context.writeByte(Selectors.SELECTOR_STORAGE_TREE_MAP_NODE);
        else if (this.definingClass === Constants.STORAGE_LINKED_LIST_NODE_NAME)
            context.writeByte(Selectors.SELECTOR_STORAGE_LINKED_LIST_NODE);
        else if (this.definingClass === Constants.PAYABLE_CONTRACT_NAME)
            context.writeByte(Selectors.SELECTOR_PAYABLE_CONTRACT);
        else if (this.equals(ClassType.EOA))
            context.writeByte(Selectors.SELECTOR_EOA);
        else if (this.equals(ClassType.GENERIC_GAS_STATION))
            context.writeByte(Selectors.SELECTOR_GENERIC_GAS_STATION);
        else if (this.equals(ClassType.EVENT))
            context.writeByte(Selectors.SELECTOR_EVENT);
        else if (this.definingClass.startsWith(Constants.IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME)) {
            context.writeByte(Selectors.SELECTOR_IO_TAKAMAKA_CODE_LANG);
            // we drop the initial io.takamaka.code.lang. portion of the name
            context.writeStringShared(this.definingClass.substring(Constants.IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME.length));
        }
        else if (this.definingClass.startsWith(Constants.IO_TAKAMAKA_CODE_UTIL_PACKAGE_NAME)) {
            context.writeByte(Selectors.SELECTOR_IO_TAKAMAKA_CODE_UTIL);
            // we drop the initial io.takamaka.code.util. portion of the name
            context.writeStringShared(this.definingClass.substring(Constants.IO_TAKAMAKA_CODE_UTIL_PACKAGE_NAME.length));
        }
        else if (this.definingClass.startsWith(Constants.IO_TAKAMAKA_CODE_TOKENS_PACKAGE_NAME)) {
            context.writeByte(Selectors.SELECTOR_IO_TAKAMAKA_CODE_TOKENS);
            // we drop the initial io.takamaka.code.tokens. portion of the name
            context.writeStringShared(this.definingClass.substring(Constants.IO_TAKAMAKA_CODE_TOKENS_PACKAGE_NAME.length))
        }
        else if (this.definingClass.startsWith(Constants.IO_TAKAMAKA_CODE_PACKAGE_NAME)) {
            context.writeByte(Selectors.SELECTOR_IO_TAKAMAKA_CODE);
            // we drop the initial io.takamaka.code. portion of the name
            context.writeStringShared(this.definingClass.substring(Constants.IO_TAKAMAKA_CODE_PACKAGE_NAME.length))
        }
        else {
            context.writeByte(Selectors.SELECTOR_CLASS_TYPE); // to distinguish from the basic types
            context.writeStringShared(this.definingClass);
        }
    }

    protected intoWithoutSelector(context: MarshallingContext): void {
        // nothing
    }
}