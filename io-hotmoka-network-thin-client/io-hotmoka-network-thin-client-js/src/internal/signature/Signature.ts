import {Algorithm} from "./Algorithm";

export class Signature {
    /**
     * The algorithm of the signatures.
     */
    public readonly algorithm: Algorithm

    /**
     * The raw key encoded in base64 or wrapped in a PEM format.
     */
    public readonly privateKey: string


    constructor(algorithm: Algorithm, privateKey: string) {
        this.algorithm = algorithm
        this.privateKey = privateKey
    }
}

