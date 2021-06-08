import {RemoteNode} from "../src"
import {expect} from 'chai';
import {TransactionReferenceModel} from "../src/models/values/TransactionReferenceModel";
import {StateModel} from "../src/models/updates/StateModel";
import {StorageReferenceModel} from "../src/models/values/StorageReferenceModel";
import {TransactionRestRequestModel} from "../src/models/requests/TransactionRestRequestModel";
import {TransactionRestResponseModel} from "../src/models/responses/TransactionRestResponseModel";
import * as fs from "fs";
import * as path from "path"
import {JarStoreTransactionRequestModel} from "../src/models/requests/JarStoreTransactionRequestModel";
import {InstanceMethodCallTransactionRequestModel} from "../src/models/requests/InstanceMethodCallTransactionRequestModel";
import {SignatureAlgorithmResponseModel} from "../src/models/responses/SignatureAlgorithmResponseModel";
import {ConstructorCallTransactionRequestModel} from "../src/models/requests/ConstructorCallTransactionRequestModel";
import {ConstructorSignatureModel} from "../src/models/signatures/ConstructorSignatureModel";
import {StorageValueModel} from "../src/models/values/StorageValueModel";
import {JarStoreInitialTransactionResponseModel} from "../src/models/responses/JarStoreInitialTransactionResponseModel";
import {NonVoidMethodSignatureModel} from "../src/models/signatures/NonVoidMethodSignatureModel";
import {ClassType} from "../src/internal/lang/ClassType";


const HOTMOKA_VERSION = "1.0.0"
const CHAIN_ID = "test"
const REMOTE_NODE_URL = "http://localhost:8080"

describe('Testing the GET methods of a remote hotmoka node', () => {

    it('getTakamakaCode - it should respond with a valid takamakacode', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL)

        const result: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        expect(result.hash).to.be.not.null
        expect(result.hash).to.be.have.length.above(10)
    })

    it('getManifest - it should respond with a valid manifest', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL)

        const result: StorageReferenceModel = await remoteNode.getManifest()

        expect(result.transaction).to.be.not.null
        expect(result.transaction.hash).to.be.not.null
        expect(result.transaction.hash).to.be.have.length.above(10)
    })

    it('getState - it should respond with a valid state model of the manifest', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL)

        const manifest: StorageReferenceModel = await remoteNode.getManifest()
        const result: StateModel = await remoteNode.getState(manifest)

        expect(result.updates).to.be.not.null
        expect(result.updates.length).to.be.above(1)

        const manifestUpdate = result.updates.filter(update => update.className === 'io.takamaka.code.governance.Manifest')
        expect(manifestUpdate).to.not.be.empty
        expect(manifestUpdate[0].object.transaction.hash).to.eql(manifest.transaction.hash)
    })

    it('getRequestAt - it should respond with a valid request for takamakacode', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL)

        const takamakacode: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        const result: TransactionRestRequestModel<unknown> = await remoteNode.getRequestAt(takamakacode)
        const jarStoreTransaction = result.transactionRequestModel as JarStoreTransactionRequestModel

        expect(result.transactionRequestModel).to.be.not.null
        expect(jarStoreTransaction.jar).to.be.not.null
        expect(result.type).to.be.not.null
        expect(result.type).to.be.eql('io.hotmoka.network.requests.JarStoreInitialTransactionRequestModel')
    })

    it('getResponseAt - it should respond with a valid response for takamakacode', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL)

        const takamakacode: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        const result: TransactionRestResponseModel<unknown> = await remoteNode.getResponseAt(takamakacode)
        const jarStoreInitialTransaction = result.transactionResponseModel as JarStoreInitialTransactionResponseModel

        expect(result.transactionResponseModel).to.be.not.null
        expect(jarStoreInitialTransaction.instrumentedJar).to.be.not.null
        expect(jarStoreInitialTransaction.dependencies.length).to.be.eql(0)
        expect(result.type).to.be.not.null
        expect(result.type).to.be.eql('io.hotmoka.network.responses.JarStoreInitialTransactionResponseModel')
    })

    it('getPolledResponseAt - it should respond with a valid polledResponse for takamakacode', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL)

        const takamakacode: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        const result: TransactionRestResponseModel<unknown> = await remoteNode.getPolledResponseAt(takamakacode)
        const jarStoreInitialTransaction = result.transactionResponseModel as JarStoreInitialTransactionResponseModel

        expect(result.transactionResponseModel).to.be.not.null
        expect(jarStoreInitialTransaction.instrumentedJar).to.be.not.null
        expect(jarStoreInitialTransaction.dependencies.length).to.be.eql(0)
        expect(result.type).to.be.not.null
        expect(result.type).to.be.eql('io.hotmoka.network.responses.JarStoreInitialTransactionResponseModel')
    })

    it('getNameOfSignatureAlgorithmForRequests - it should respond with a signature algorithm', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL)
        const result: SignatureAlgorithmResponseModel = await remoteNode.getNameOfSignatureAlgorithmForRequests()

        expect(result).to.be.not.null
        expect(result.algorithm).to.be.not.null
        expect(result.algorithm).to.be.not.empty
    })
})

describe('Testing the ADD methods of a remote hotmoka node', () => {
    let lambdasjarTransaction: TransactionReferenceModel
    let lambdasTransactionReference: StorageReferenceModel

    it('addJarStoreTransaction - it should add a valid jar transaction', async () => {
        const jar = getLocalJar("lambdas.jar")
        const remoteNode = new RemoteNode(REMOTE_NODE_URL)

        const manifest = await remoteNode.getManifest()
        const takamakacode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakacode)
        const nonceOfGamete = await getNonceOfGamete(gamete.reference!, takamakacode)

        const request = new JarStoreTransactionRequestModel(
            gamete.reference!,
            nonceOfGamete.value!,
            takamakacode,
            "500000",
            "203377",
            jar.toString("base64"),
            [takamakacode],
            "test"
        )

        lambdasjarTransaction = await remoteNode.addJarStoreTransaction(request)
        expect(lambdasjarTransaction.hash).to.be.not.null
        expect(lambdasjarTransaction.hash).to.be.have.length.above(10)
    })

    it('addConstructorCallTransaction - it should invoke new Lambdas() on the lambdas jar and get a valid result', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL)

        const manifest = await remoteNode.getManifest()
        const takamakacode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakacode)
        const nonceOfGamete = await getNonceOfGamete(gamete.reference!, takamakacode)

        const constructorSignature = new ConstructorSignatureModel(
            "io.hotmoka.examples.lambdas.Lambdas",
            ["java.math.BigInteger", "java.lang.String"]
        )
        const actuals = [StorageValueModel.newStorageValue("100000", "java.math.BigInteger"), StorageValueModel.newStorageValue("", "java.lang.String")]

        const request = new ConstructorCallTransactionRequestModel(
            gamete.reference!,
            nonceOfGamete.value!,
            lambdasjarTransaction,
            "500000",
            "1",
            constructorSignature,
            actuals,
            CHAIN_ID
        )

        lambdasTransactionReference = await remoteNode.addConstructorCallTransaction(request)

        expect(lambdasTransactionReference.transaction).to.be.not.null
        expect(lambdasTransactionReference.transaction.hash).to.be.have.length.above(10)
    })


    it('addInstanceMethodCallTransaction - it should invoke new Lambdas().whiteListChecks(13,1,1973) == 7 on the lambdas jar and get a valid result', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL)

        const manifest = await remoteNode.getManifest()
        const takamakacode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakacode)
        const nonceOfGamete = await getNonceOfGamete(gamete.reference!, takamakacode)

        const result: StorageValueModel = await remoteNode.addInstanceMethodCallTransaction(
            new InstanceMethodCallTransactionRequestModel(
                gamete.reference!,
                nonceOfGamete.value!,
                lambdasjarTransaction,
                "500000",
                "1",
                new NonVoidMethodSignatureModel(
                    "whiteListChecks",
                    "io.hotmoka.examples.lambdas.Lambdas",
                    ["java.lang.Object", "java.lang.Object", "java.lang.Object"],
                    "int"
                ),
                [StorageValueModel.newStorageValue("13", "java.math.BigInteger"), StorageValueModel.newStorageValue("1", "java.math.BigInteger"), StorageValueModel.newStorageValue("1973", "java.math.BigInteger")],
                lambdasTransactionReference,
                CHAIN_ID
            )
        )

        expect(result.value).to.be.not.null
        expect(result.value).to.be.eql("7")
    })

})


const getLocalJar = (jarName: string): Buffer => {
    return fs.readFileSync(
        path.join(
            __dirname,
            "../../../io-hotmoka-examples/target/io-hotmoka-examples-" + HOTMOKA_VERSION + "-" + jarName
        )
    )
}

const getGamete = (manifest: StorageReferenceModel, takamakacode: TransactionReferenceModel) => {
    const remoteNode = new RemoteNode(REMOTE_NODE_URL)

    const methodSignature = new NonVoidMethodSignatureModel(
        "getGamete",
        ClassType.MANIFEST.name,
        [],
        ClassType.ACCOUNT.name
    )

    return remoteNode.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequestModel(
        manifest,
        "0",
        takamakacode,
        "15000",
        "0",
        methodSignature,
        [],
        manifest,
        CHAIN_ID
    ))
}

const getNonceOfGamete = (gamete: StorageReferenceModel, takamakacode: TransactionReferenceModel) => {
    const remoteNode = new RemoteNode(REMOTE_NODE_URL)

    const methodSignature = new NonVoidMethodSignatureModel(
        "nonce",
        ClassType.ACCOUNT.name,
        [],
        ClassType.BIG_INTEGER.name
    )

    return remoteNode.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequestModel(
        gamete,
        "0",
        takamakacode,
        "15000",
        "0",
        methodSignature,
        [],
        gamete,
        CHAIN_ID
    ))
}



