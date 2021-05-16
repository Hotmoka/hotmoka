import {TransactionReferenceModel} from "../models/values/TransactionReferenceModel";
import {StorageReferenceModel} from "../models/values/StorageReferenceModel";
import {StateModel} from "../models/updates/StateModel";
import {ClassTagModel} from "../models/updates/ClassTagModel";
import {SignatureModel} from "../models/signatures/SignatureModel";
import {TransactionRestRequestModel} from "../models/requests/TransactionRestRequestModel";
import {TransactionRestResponseModel} from "../models/responses/TransactionRestResponseModel";

export interface Node {

    getTakamakaCode(): Promise<TransactionReferenceModel>
    getManifest(): Promise<StorageReferenceModel>
    getState(request: StorageReferenceModel): Promise<StateModel>
    getClassTag(request: StorageReferenceModel): Promise<ClassTagModel>
    getNameOfSignatureAlgorithmForRequests(): Promise<SignatureModel>
    getRequestAt(request: TransactionReferenceModel): Promise<TransactionRestRequestModel<unknown>>
    getResponseAt(request: TransactionReferenceModel): Promise<TransactionRestResponseModel<unknown>>
    getPolledResponseAt(request: TransactionReferenceModel): Promise<TransactionRestResponseModel<unknown>>
}