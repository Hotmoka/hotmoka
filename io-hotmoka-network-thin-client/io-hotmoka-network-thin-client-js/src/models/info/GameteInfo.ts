export class GameteInfo {
    balanceOfGamete?: string
    redBalance?: string
    maxFaucet?: string
    maxRedFaucet?: string

    constructor(balanceOfGamete?: string,
                redBalance?: string,
                maxFaucet?: string,
                maxRedFaucet?: string) {
        this.balanceOfGamete = balanceOfGamete ?? '0'
        this.redBalance = redBalance ?? '0'
        this.maxFaucet = maxFaucet ?? '0'
        this.maxRedFaucet = maxRedFaucet ?? '0'
    }
}