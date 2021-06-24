import {TransactionReferenceModel} from "../models/values/TransactionReferenceModel";
import {StorageReferenceModel} from "../models/values/StorageReferenceModel";
import {StateModel} from "../models/updates/StateModel";
import {ClassTagModel} from "../models/updates/ClassTagModel";
import {TransactionRestRequestModel} from "../models/requests/TransactionRestRequestModel";
import {TransactionRestResponseModel} from "../models/responses/TransactionRestResponseModel";
import {JarStoreInitialTransactionRequestModel} from "../models/requests/JarStoreInitialTransactionRequestModel";
import {GameteCreationTransactionRequestModel} from "../models/requests/GameteCreationTransactionRequestModel";
import {InitializationTransactionRequestModel} from "../models/requests/InitializationTransactionRequestModel";
import {JarStoreTransactionRequestModel} from "../models/requests/JarStoreTransactionRequestModel";
import {ConstructorCallTransactionRequestModel} from "../models/requests/ConstructorCallTransactionRequestModel";
import {StorageValueModel} from "../models/values/StorageValueModel";
import {InstanceMethodCallTransactionRequestModel} from "../models/requests/InstanceMethodCallTransactionRequestModel";
import {StaticMethodCallTransactionRequestModel} from "../models/requests/StaticMethodCallTransactionRequestModel";
import {SignatureAlgorithmResponseModel} from "../models/responses/SignatureAlgorithmResponseModel";
import {InfoModel} from "../models/info/InfoModel";

export interface Node {

    // get
    getTakamakaCode(): Promise<TransactionReferenceModel>
    getManifest(): Promise<StorageReferenceModel>
    getState(request: StorageReferenceModel): Promise<StateModel>
    getClassTag(request: StorageReferenceModel): Promise<ClassTagModel>
    getNameOfSignatureAlgorithmForRequests(): Promise<SignatureAlgorithmResponseModel>
    getRequestAt(request: TransactionReferenceModel): Promise<TransactionRestRequestModel<unknown>>
    getResponseAt(request: TransactionReferenceModel): Promise<TransactionRestResponseModel<unknown>>
    getPolledResponseAt(request: TransactionReferenceModel): Promise<TransactionRestResponseModel<unknown>>

    // add
    addJarStoreInitialTransaction(request: JarStoreInitialTransactionRequestModel): Promise<TransactionReferenceModel>
    addRedGreenGameteCreationTransaction(request: GameteCreationTransactionRequestModel): Promise<StorageReferenceModel>
    addInitializationTransaction(request: InitializationTransactionRequestModel): Promise<void>
    addJarStoreTransaction(request: JarStoreTransactionRequestModel): Promise<TransactionReferenceModel>
    addConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): Promise<StorageReferenceModel>
    addInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): Promise<StorageValueModel>
    addStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): Promise<StorageValueModel>

    // post
    postJarStoreTransaction(request: JarStoreTransactionRequestModel): Promise<TransactionReferenceModel>
    postConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): Promise<TransactionReferenceModel>
    postInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): Promise<TransactionReferenceModel>
    postStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): Promise<TransactionReferenceModel>

    // run
    runInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): Promise<StorageValueModel>
    runStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): Promise<StorageValueModel>

    // info
    info(): Promise<InfoModel>
}