import {Node} from "./Node"
import {StorageReferenceModel} from "../models/values/StorageReferenceModel";
import {ClassTagModel} from "../models/updates/ClassTagModel";
import {StateModel} from "../models/updates/StateModel";
import {TransactionReferenceModel} from "../models/values/TransactionReferenceModel";
import axios, {AxiosResponse} from "axios";
import {ErrorModel} from "../models/errors/ErrorModel";
import {TransactionRestRequestModel} from "../models/requests/TransactionRestRequestModel";
import {TransactionRestResponseModel} from "../models/responses/TransactionRestResponseModel";
import {JarStoreInitialTransactionRequestModel} from "../models/requests/JarStoreInitialTransactionRequestModel";
import {GameteCreationTransactionRequestModel} from "../models/requests/GameteCreationTransactionRequestModel";
import {InitializationTransactionRequestModel} from "../models/requests/InitializationTransactionRequestModel";
import {JarStoreTransactionRequestModel} from "../models/requests/JarStoreTransactionRequestModel";
import {ConstructorCallTransactionRequestModel} from "../models/requests/ConstructorCallTransactionRequestModel";
import {InstanceMethodCallTransactionRequestModel} from "../models/requests/InstanceMethodCallTransactionRequestModel";
import {StorageValueModel} from "../models/values/StorageValueModel";
import {StaticMethodCallTransactionRequestModel} from "../models/requests/StaticMethodCallTransactionRequestModel";
import {SignatureAlgorithmResponseModel} from "../models/responses/SignatureAlgorithmResponseModel";
import {Signer} from "./Signer";
import {PrivateKey} from "./PrivateKey";

export class RemoteNode implements Node {
    readonly url: string

    /**
     * It constructs the instance of the remote node.
     * @param url the url of the remote node
     * @param privateKey the private key to sign the requests
     */
    constructor(url: string, privateKey: PrivateKey) {
        this.url = url
        Signer.loadPrivateKey(privateKey)
    }


    /**
     * It resolves the given error received from the HTTP call.
     * @param error the error
     * @private
     * @return ErrorModel the parsed error model
     */
    private static resolveError(error: any): ErrorModel {
        if (error && error.response && error.response.data && error.response.data.message)
            return new ErrorModel(error.response.data.message, error.response.data.exceptionClassName || 'Exception')
        else
            return new ErrorModel('Internal Server Error', 'Exception')
    }

    /**
     * Performs a GET request and yields a Promise of entity T as response.
     * @param url the url
     * @private
     * @return a Promise of entity T
     */
    private static async get<T>(url: string): Promise<T> {
        try {
            const res: AxiosResponse = await axios.get<T>(url)
            return res.data
        } catch (error) {
            throw RemoteNode.resolveError(error)
        }
    }

    /**
     * Performs a POST request and yields a Promise of entity T as response.
     * @param url the url
     * @param body the body of type P
     * @private
     * @return a Promise of entity T
     */
    private static async post<T, P>(url: string, body?: P): Promise<T> {
        try {
            const res: AxiosResponse = await axios.post<T>(url, body)
            return res.data
        } catch (error) {
            throw RemoteNode.resolveError(error)
        }
    }

    // get

    async getClassTag(request: StorageReferenceModel): Promise<ClassTagModel> {
        return await RemoteNode.post<ClassTagModel, StorageReferenceModel>(this.url + '/get/classTag', request)
    }

    async getManifest(): Promise<StorageReferenceModel> {
        return await RemoteNode.get<StorageReferenceModel>(this.url + '/get/manifest')
    }

    async getState(request: StorageReferenceModel): Promise<StateModel> {
        return await RemoteNode.post<StateModel, StorageReferenceModel>(this.url + '/get/state', request)
    }

    async getTakamakaCode(): Promise<TransactionReferenceModel> {
        return await RemoteNode.get<TransactionReferenceModel>(this.url + '/get/takamakaCode')
    }

    async getNameOfSignatureAlgorithmForRequests(): Promise<SignatureAlgorithmResponseModel> {
        return await RemoteNode.get<SignatureAlgorithmResponseModel>(this.url + '/get/nameOfSignatureAlgorithmForRequests')
    }

    async getRequestAt(request: TransactionReferenceModel): Promise<TransactionRestRequestModel<unknown>> {
        return await RemoteNode.post<TransactionRestRequestModel<unknown>, TransactionReferenceModel>(this.url + '/get/request', request)
    }

    async getResponseAt(request: TransactionReferenceModel): Promise<TransactionRestResponseModel<unknown>> {
        return await RemoteNode.post<TransactionRestResponseModel<unknown>, TransactionReferenceModel>(this.url + '/get/response', request)
    }

    async getPolledResponseAt(request: TransactionReferenceModel): Promise<TransactionRestResponseModel<unknown>> {
        return await RemoteNode.post<TransactionRestResponseModel<unknown>, TransactionReferenceModel>(this.url + '/get/polledResponse', request)
    }

    // add

    async addJarStoreInitialTransaction(request: JarStoreInitialTransactionRequestModel): Promise<TransactionReferenceModel> {
        return await RemoteNode.post<TransactionReferenceModel, JarStoreInitialTransactionRequestModel>(this.url + '/add/jarStoreInitialTransaction', request)
    }

    async addRedGreenGameteCreationTransaction(request: GameteCreationTransactionRequestModel): Promise<StorageReferenceModel> {
        return await RemoteNode.post<StorageReferenceModel, GameteCreationTransactionRequestModel>(this.url + '/add/gameteCreationTransaction', request)
    }

    async addInitializationTransaction(request: InitializationTransactionRequestModel): Promise<void> {
        return await RemoteNode.post<void, InitializationTransactionRequestModel>(this.url + '/add/initializationTransaction', request)
    }

    async addJarStoreTransaction(request: JarStoreTransactionRequestModel): Promise<TransactionReferenceModel> {
        return await RemoteNode.post<TransactionReferenceModel, JarStoreTransactionRequestModel>(this.url + '/add/jarStoreTransaction', request)
    }

    async addConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): Promise<StorageReferenceModel> {
        return await RemoteNode.post<StorageReferenceModel, ConstructorCallTransactionRequestModel>(this.url + '/add/constructorCallTransaction', request)
    }

    async addInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): Promise<StorageValueModel> {
        return await RemoteNode.post<StorageValueModel, InstanceMethodCallTransactionRequestModel>(this.url + '/add/instanceMethodCallTransaction', request)
    }

    async addStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): Promise<StorageValueModel> {
        return await RemoteNode.post<StorageValueModel, StaticMethodCallTransactionRequestModel>(this.url + '/add/staticMethodCallTransaction', request)
    }

    // post

    async postJarStoreTransaction(request: JarStoreTransactionRequestModel): Promise<TransactionReferenceModel> {
        return await RemoteNode.post<TransactionReferenceModel, JarStoreTransactionRequestModel>(this.url + '/post/jarStoreTransaction', request)
    }

    async postConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): Promise<TransactionReferenceModel> {
        return await RemoteNode.post<TransactionReferenceModel, ConstructorCallTransactionRequestModel>(this.url + '/post/constructorCallTransaction', request)
    }

    async postInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): Promise<TransactionReferenceModel> {
        return await RemoteNode.post<TransactionReferenceModel, InstanceMethodCallTransactionRequestModel>(this.url + '/post/instanceMethodCallTransaction', request)
    }

    async postStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): Promise<TransactionReferenceModel> {
        return await RemoteNode.post<TransactionReferenceModel, StaticMethodCallTransactionRequestModel>(this.url + '/post/staticMethodCallTransaction', request)
    }

    // run

    async runInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): Promise<StorageValueModel> {
        return await RemoteNode.post<StorageValueModel, InstanceMethodCallTransactionRequestModel>(this.url + '/run/instanceMethodCallTransaction', request)
    }

    async runStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): Promise<StorageValueModel> {
        return await RemoteNode.post<StorageValueModel, StaticMethodCallTransactionRequestModel>(this.url + '/run/staticMethodCallTransaction', request)
    }
}