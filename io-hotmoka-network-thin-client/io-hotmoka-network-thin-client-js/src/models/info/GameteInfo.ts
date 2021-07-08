import {StorageReferenceModel} from "../values/StorageReferenceModel";

export class GameteInfo {
    gamete?: StorageReferenceModel
    balanceOfGamete?: string
    redBalance?: string
    maxFaucet?: string
    maxRedFaucet?: string

    constructor(
        gamete?: StorageReferenceModel,
        balanceOfGamete?: string,
        redBalance?: string,
        maxFaucet?: string,
        maxRedFaucet?: string) {
        this.gamete = gamete
        this.balanceOfGamete = balanceOfGamete ?? '0'
        this.redBalance = redBalance ?? '0'
        this.maxFaucet = maxFaucet ?? '0'
        this.maxRedFaucet = maxRedFaucet ?? '0'
    }
}