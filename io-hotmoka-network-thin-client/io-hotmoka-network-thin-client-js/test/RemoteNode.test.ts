import {RemoteNode} from "../src"
import {expect} from 'chai';
import {TransactionReferenceModel} from "../src/models/values/TransactionReferenceModel";

describe('GET test for remote hotmoka node', () => {

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    it('it should respond with a valid takamakacode', async () => {
        const remoteNode = new RemoteNode("http://localhost:8080")

        const result: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        expect(result.hash).to.be.not.null
        expect(result.hash).to.be.have.length.above(10)
    })
})

