import {Node} from "./Node"
import {StorageReferenceModel} from "../models/values/StorageReferenceModel";
import {ClassTagModel} from "../models/updates/ClassTagModel";
import {StateModel} from "../models/updates/StateModel";
import {TransactionReferenceModel} from "../models/values/TransactionReferenceModel";
import axios, {AxiosResponse} from "axios";
import {ErrorModel} from "../models/errors/ErrorModel";

export class RemoteNode implements Node {
    readonly url: string

    /**
     * It constructs the instance of the remote node.
     * @param url the url of the remote node
     */
    constructor(url: string) {
        this.url = url
    }

    /**
     * It resolves the given error received from the HTTP call.
     * @param error the error
     * @private
     * @return ErrorModel the parsed error model
     */
    private static resolveError(error): ErrorModel {
        if (error && error.response && error.response.data && error.response.data.message)
            return  new ErrorModel(error.response.data.message, error.response.data.exceptionClassName || 'Exception')
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
}