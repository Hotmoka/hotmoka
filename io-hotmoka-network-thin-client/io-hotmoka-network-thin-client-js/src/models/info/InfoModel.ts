import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {GameteInfo} from "./GameteInfo";
import {GasStation} from "./GasStation";
import {Validators} from "./Validators";

export class InfoModel {
    takamakaCode?: TransactionReferenceModel
    manifest?: StorageReferenceModel
    chainId?: string
    versions?: StorageReferenceModel
    verificationVersion?: string
    maxErrorLength?: number
    maxDependencies?: number
    maxCumulativeSizeOfDependencies?: number
    allowsSelfCharged?: boolean
    allowsUnsignedFaucet?: boolean
    skipsVerification?: boolean
    signature?: string
    gameteInfo?: GameteInfo
    gasStation?: GasStation
    validators?: Validators
}