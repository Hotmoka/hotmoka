import {TransactionReferenceModel} from "../models/values/TransactionReferenceModel";
import {StorageReferenceModel} from "../models/values/StorageReferenceModel";
import {StateModel} from "../models/updates/StateModel";
import {ClassTagModel} from "../models/updates/ClassTagModel";

export interface Node {

    getTakamakaCode(): Promise<TransactionReferenceModel>
    getManifest(): Promise<StorageReferenceModel>
    getState(request: StorageReferenceModel): Promise<StateModel>
    getClassTag(request: StorageReferenceModel): Promise<ClassTagModel>
}