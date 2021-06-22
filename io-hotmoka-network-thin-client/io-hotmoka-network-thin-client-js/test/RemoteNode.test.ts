import {RemoteNode, Signature} from "../src"
import {expect} from 'chai';
import {TransactionReferenceModel} from "../src";
import {StateModel} from "../src/models/updates/StateModel";
import {StorageReferenceModel} from "../src";
import {TransactionRestRequestModel} from "../src/models/requests/TransactionRestRequestModel";
import {TransactionRestResponseModel} from "../src/models/responses/TransactionRestResponseModel";
import * as fs from "fs";
import * as path from "path"
import {JarStoreTransactionRequestModel} from "../src";
import {InstanceMethodCallTransactionRequestModel} from "../src";
import {SignatureAlgorithmResponseModel} from "../src/models/responses/SignatureAlgorithmResponseModel";
import {ConstructorCallTransactionRequestModel} from "../src";
import {ConstructorSignatureModel} from "../src";
import {StorageValueModel} from "../src";
import {JarStoreInitialTransactionResponseModel} from "../src/models/responses/JarStoreInitialTransactionResponseModel";
import {NonVoidMethodSignatureModel} from "../src/models/signatures/NonVoidMethodSignatureModel";
import {CodeSignature} from "../src";
import {BasicType} from "../src";
import {Algorithm} from "../src";
import {StaticMethodCallTransactionRequestModel} from "../src";
import {ConstructorCallTransactionSuccessfulResponseModel} from "../src/models/responses/ConstructorCallTransactionSuccessfulResponseModel";
import {MethodCallTransactionSuccessfulResponseModel} from "../src/models/responses/MethodCallTransactionSuccessfulResponseModel";

const getPrivateKey = (pathFile: string): string => {
    return fs.readFileSync(path.resolve(pathFile), "utf8");
}

const HOTMOKA_VERSION = "1.0.0"
const CHAIN_ID = "chain-btmZzq"
const REMOTE_NODE_URL = "http://panarea.hotmoka.io"
const basicDependencyJarClasspath = new TransactionReferenceModel("local", "a896f1cccd685a64571c1007410172c530c7e48c9822fcfbbe803748c08a3bf6")
const basicJarClasspath = new TransactionReferenceModel("local", "6b0a8e7397f474a4e803750064601fc124ceeba32b44ef7c8e08a55fdd505cb0")
const SIGNATURE = new Signature(Algorithm.ED25519, getPrivateKey("./test/keys/eoa.pri"))
const EOA = new StorageReferenceModel(new TransactionReferenceModel("local", "5472f09797ca4aa8b72bd3cfa77c7cf58e5f7bc9192b71b2297791d8df1d040d"), "0")

describe('Testing the GET methods of a remote hotmoka node', () => {

    it('getTakamakaCode - it should respond with a valid takamakaCode', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)
        const result: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        
        expect(result.hash).to.be.not.null
        expect(result.hash).to.be.have.length.above(10)
    })

    it('getManifest - it should respond with a valid manifest', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)
        const result: StorageReferenceModel = await remoteNode.getManifest()

        expect(result.transaction).to.be.not.null
        expect(result.transaction.hash).to.be.not.null
        expect(result.transaction.hash).to.be.have.length.above(10)
    })

    it('getState - it should respond with a valid state model of the manifest', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest: StorageReferenceModel = await remoteNode.getManifest()
        const result: StateModel = await remoteNode.getState(manifest)

        expect(result.updates).to.be.not.null
        expect(result.updates.length).to.be.above(1)

        const manifestUpdate = result.updates.filter(update => update.className === 'io.takamaka.code.governance.Manifest')
        expect(manifestUpdate).to.not.be.empty
        expect(manifestUpdate[0].object.transaction.hash).to.eql(manifest.transaction.hash)
    })

    it('getRequestAt - it should respond with a valid request for takamakaCode', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const takamakaCode: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        const result: TransactionRestRequestModel<unknown> = await remoteNode.getRequestAt(takamakaCode)
        const jarStoreTransaction = result.transactionRequestModel as JarStoreTransactionRequestModel

        expect(result.transactionRequestModel).to.be.not.null
        expect(jarStoreTransaction.jar).to.be.not.null
        expect(result.type).to.be.not.null
        expect(result.type).to.be.eql('io.hotmoka.network.requests.JarStoreInitialTransactionRequestModel')
    })

    it('getResponseAt - it should respond with a valid response for takamakaCode', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const takamakaCode: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        const result: TransactionRestResponseModel<unknown> = await remoteNode.getResponseAt(takamakaCode)
        const jarStoreInitialTransaction = result.transactionResponseModel as JarStoreInitialTransactionResponseModel

        expect(result.transactionResponseModel).to.be.not.null
        expect(jarStoreInitialTransaction.instrumentedJar).to.be.not.null
        expect(jarStoreInitialTransaction.dependencies.length).to.be.eql(0)
        expect(result.type).to.be.not.null
        expect(result.type).to.be.eql('io.hotmoka.network.responses.JarStoreInitialTransactionResponseModel')
    })

    it('getPolledResponseAt - it should respond with a valid polledResponse for takamakaCode', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const takamakaCode: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        const result: TransactionRestResponseModel<unknown> = await remoteNode.getPolledResponseAt(takamakaCode)
        const jarStoreInitialTransaction = result.transactionResponseModel as JarStoreInitialTransactionResponseModel

        expect(result.transactionResponseModel).to.be.not.null
        expect(jarStoreInitialTransaction.instrumentedJar).to.be.not.null
        expect(jarStoreInitialTransaction.dependencies.length).to.be.eql(0)
        expect(result.type).to.be.not.null
        expect(result.type).to.be.eql('io.hotmoka.network.responses.JarStoreInitialTransactionResponseModel')
    })

    it('getNameOfSignatureAlgorithmForRequests - it should respond with a signature algorithm', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)
        const result: SignatureAlgorithmResponseModel = await remoteNode.getNameOfSignatureAlgorithmForRequests()

        expect(result).to.be.not.null
        expect(result.algorithm).to.be.not.null
        expect(result.algorithm).to.be.not.empty
        expect(result.algorithm).to.be.eql(Algorithm[SIGNATURE.algorithm].toLocaleLowerCase())
    })
})


describe('Testing the RUN methods of a remote hotmoka node', () => {

    it('runInstanceMethodCallTransaction - getGasStation of manifest', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)
        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gasStation = await getGasStation(manifest, takamakaCode)

        expect(gasStation).to.be.not.null
        expect(gasStation.reference).to.be.not.null
        expect(gasStation.reference?.transaction).to.be.not.null
        expect(gasStation.reference?.transaction.hash).to.be.not.null
    })

    it('runInstanceMethodCallTransaction - getGasPrice of manifest', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)
        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gasStation = await getGasStation(manifest, takamakaCode)
        const gasPrice = await getGasPrice(manifest, takamakaCode, gasStation)

        expect(gasPrice.value).to.be.not.null
        expect(Number(gasPrice.value)).to.be.gte(1)
    })

    it('runInstanceMethodCallTransaction - getGamete', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)
        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakaCode)

        expect(gamete).to.be.not.null
        expect(gamete.reference).to.be.not.null
        expect(gamete.reference?.transaction).to.be.not.null
        expect(gamete.reference?.transaction.hash).to.be.not.null
    })

    it('runInstanceMethodCallTransaction - getNonceOf gamete', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)
        const takamakaCode = await remoteNode.getTakamakaCode()
        const manifest = await remoteNode.getManifest()
        const gamete = await getGamete(manifest, takamakaCode)
        const nonceOfGamete = await getNonceOf(gamete.reference!, takamakaCode)

        expect(nonceOfGamete).to.be.not.null
        expect(nonceOfGamete.value).to.be.not.null
        expect(Number(nonceOfGamete.value)).to.be.gt(1)
    })
})

describe('Testing the io-hotmoka-examples-1.0.0-lambdas.jar methods of a remote hotmoka node', () => {
    let lambdasJarClasspath: TransactionReferenceModel
    let lambdasStorageReference: StorageReferenceModel

    it.skip('addJarStoreTransaction - it should add a valid jar transaction', async () => {
        const jar = getLocalJar("lambdas.jar")
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakaCode)
        const nonceOfGamete = await getNonceOf(gamete.reference!, takamakaCode)

        const request = new JarStoreTransactionRequestModel(
            gamete.reference!,
            nonceOfGamete.value!,
            takamakaCode,
            "500000",
            "203377",
            jar.toString("base64"),
            [takamakaCode],
            "test"
        )

        lambdasJarClasspath = await remoteNode.addJarStoreTransaction(request)
        expect(lambdasJarClasspath.hash).to.be.not.null
        expect(lambdasJarClasspath.hash).to.be.have.length.above(10)
    })

    it.skip('addConstructorCallTransaction - it should invoke new Lambdas()', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakaCode)
        const nonceOfGamete = await getNonceOf(gamete.reference!, takamakaCode)

        const constructorSignature = new ConstructorSignatureModel(
            "io.hotmoka.examples.lambdas.Lambdas",
            ["java.math.BigInteger", "java.lang.String"]
        )
        const actuals = [StorageValueModel.newStorageValue("100000", "java.math.BigInteger"), StorageValueModel.newStorageValue("", "java.lang.String")]

        const request = new ConstructorCallTransactionRequestModel(
            gamete.reference!,
            nonceOfGamete.value!,
            lambdasJarClasspath,
            "500000",
            "1",
            constructorSignature,
            actuals,
            CHAIN_ID
        )

        lambdasStorageReference = await remoteNode.addConstructorCallTransaction(request)

        expect(lambdasStorageReference.transaction).to.be.not.null
        expect(lambdasStorageReference.transaction.hash).to.be.have.length.above(10)
    })


    it.skip('addInstanceMethodCallTransaction - it should invoke new Lambdas().whiteListChecks(13,1,1973) == 7', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakaCode)
        const nonceOfGamete = await getNonceOf(gamete.reference!, takamakaCode)

        const result: StorageValueModel = await remoteNode.addInstanceMethodCallTransaction(
            new InstanceMethodCallTransactionRequestModel(
                gamete.reference!,
                nonceOfGamete.value!,
                lambdasJarClasspath,
                "500000",
                "1",
                new NonVoidMethodSignatureModel(
                    "whiteListChecks",
                    "io.hotmoka.examples.lambdas.Lambdas",
                    ["java.lang.Object", "java.lang.Object", "java.lang.Object"],
                    "int"
                ),
                [StorageValueModel.newStorageValue("13", "java.math.BigInteger"), StorageValueModel.newStorageValue("1", "java.math.BigInteger"), StorageValueModel.newStorageValue("1973", "java.math.BigInteger")],
                lambdasStorageReference,
                CHAIN_ID
            )
        )

        expect(result.value).to.be.not.null
        expect(result.value).to.be.eql("7")
    })

})

/*
describe('Testing the io-hotmoka-examples-1.0.0-basic.jar of a remote hotmoka node', () => {
   let simpleStorageReference: StorageReferenceModel


    it.only('addConstructorCallTransaction - it should invoke new Simple(13)', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const takamakaCode = await remoteNode.getTakamakaCode()
        const nonceOfEOA = await getNonceOf(EOA, takamakaCode)
        EOA.progressive = nonceOfEOA.value!

        // constructor call
        const requestConstructorCall = new ConstructorCallTransactionRequestModel(
            EOA,
            nonceOfEOA.value!,
            basicJarClasspath,
            "19000",
            "0",
            new ConstructorSignatureModel(
                "io.hotmoka.examples.basic.Simple",
                [BasicType.INT.name]
            ),
            [StorageValueModel.newStorageValue("13", BasicType.INT.name)],
            CHAIN_ID
        )

        simpleStorageReference = await remoteNode.addConstructorCallTransaction(requestConstructorCall)
        expect(simpleStorageReference).to.be.not.null
        expect(simpleStorageReference.transaction).to.be.not.null
        expect(simpleStorageReference.transaction.hash).to.be.not.null
    })


    it('runInstanceMethodCallTransaction - it should invoke simple.foo3() == 13', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakaCode)
        const nonceOfGamete = await getNonceOf(gamete.reference!, takamakaCode)

        // method call
        const requestInstanceMethodCall = new InstanceMethodCallTransactionRequestModel(
            gamete.reference!,
            nonceOfGamete.value!,
            basicJarClasspath,
            "19000",
            "0",
            new NonVoidMethodSignatureModel(
                "foo3",
                "io.hotmoka.examples.basic.Simple",
                [],
                BasicType.INT.name
            ),
            [],
            simpleStorageReference,
            CHAIN_ID
        )

        const result = await remoteNode.runInstanceMethodCallTransaction(requestInstanceMethodCall)
        expect(result).to.be.not.null
        expect(result.value).to.be.not.null
        expect(result.value).to.be.eql('13')
    })


    it('addStaticMethodCallTransaction - it should invoke Simple.foo5() == 14', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakaCode)
        const nonceOfGamete = await getNonceOf(gamete.reference!, takamakaCode)

        // method call
        const requestInstanceMethodCall = new StaticMethodCallTransactionRequestModel(
            gamete.reference!,
            nonceOfGamete.value!,
            basicJarClasspath,
            "19000",
            "0",
            new NonVoidMethodSignatureModel(
                "foo5",
                "io.hotmoka.examples.basic.Simple",
                [],
                BasicType.INT.name
            ),
            [],
            CHAIN_ID
        )

        const result = await remoteNode.addStaticMethodCallTransaction(requestInstanceMethodCall)
        expect(result).to.be.not.null
        expect(result.value).to.be.not.null
        expect(result.value).to.be.eql('14')
    })

    it('runStaticMethodCallTransaction - it should invoke Simple.foo5() == 14', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakaCode)
        const nonceOfGamete = await getNonceOf(gamete.reference!, takamakaCode)

        // method call
        const requestInstanceMethodCall = new StaticMethodCallTransactionRequestModel(
            gamete.reference!,
            nonceOfGamete.value!,
            basicJarClasspath,
            "19000",
            "0",
            new NonVoidMethodSignatureModel(
                "foo5",
                "io.hotmoka.examples.basic.Simple",
                [],
                BasicType.INT.name
            ),
            [],
            CHAIN_ID
        )

        const result = await remoteNode.runStaticMethodCallTransaction(requestInstanceMethodCall)
        expect(result).to.be.not.null
        expect(result.value).to.be.not.null
        expect(result.value).to.be.eql('14')
    })

})*/

/*
describe('Testing the io-hotmoka-examples-1.0.0-basicdependency.jar of a remote hotmoka node [PROMISE version]', () => {

    it('postConstructorCallTransaction - it should invoke new Simple(13)', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakaCode)
        const nonceOfGamete = await getNonceOf(gamete.reference!, takamakaCode)

        // constructor call
        const requestConstructorCall = new ConstructorCallTransactionRequestModel(
            gamete.reference!,
            nonceOfGamete.value!,
            basicJarClasspath,
            "19000",
            "0",
            new ConstructorSignatureModel(
                "io.hotmoka.examples.basic.Simple",
                [BasicType.INT.name]
            ),
            [StorageValueModel.newStorageValue("13", BasicType.INT.name)],
            CHAIN_ID
        )

        const promiseResult = await remoteNode.postConstructorCallTransaction(requestConstructorCall)
        const result = await remoteNode.getResponseAt(promiseResult)
        const successfulTransaction = result.transactionResponseModel as ConstructorCallTransactionSuccessfulResponseModel

        expect(successfulTransaction).to.be.not.null
        const simpleStorageReference = successfulTransaction.newObject
        expect(simpleStorageReference).to.be.not.null
        expect(simpleStorageReference.transaction).to.be.not.null
        expect(simpleStorageReference.transaction.hash).to.be.not.null
    })


    it('postStaticMethodCallTransaction - it should invoke Simple.foo5() == 14', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakaCode)
        const nonceOfGamete = await getNonceOf(gamete.reference!, takamakaCode)

        // method call
        const requestInstanceMethodCall = new StaticMethodCallTransactionRequestModel(
            gamete.reference!,
            nonceOfGamete.value!,
            basicJarClasspath,
            "19000",
            "0",
            new NonVoidMethodSignatureModel(
                "foo5",
                "io.hotmoka.examples.basic.Simple",
                [],
                BasicType.INT.name
            ),
            [],
            CHAIN_ID
        )

        const promiseResult = await remoteNode.postStaticMethodCallTransaction(requestInstanceMethodCall)
        const result = await remoteNode.getPolledResponseAt(promiseResult)
        const successfulTransaction = result.transactionResponseModel as MethodCallTransactionSuccessfulResponseModel

        expect(successfulTransaction).to.be.not.null
        expect(successfulTransaction.result).to.be.not.null
        expect(successfulTransaction.result.value).to.be.not.null
        expect(successfulTransaction.result.value).to.be.eql('14')
    })

})
*/


const getLocalJar = (jarName: string): Buffer => {
    return fs.readFileSync(
        path.join(
            __dirname,
            "../../../io-hotmoka-examples/target/io-hotmoka-examples-" + HOTMOKA_VERSION + "-" + jarName
        )
    )
}

const getGamete = (manifest: StorageReferenceModel, takamakaCode: TransactionReferenceModel): Promise<StorageValueModel> => {
    const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

    return remoteNode.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequestModel(
        manifest,
        "0",
        takamakaCode,
        "16999",
        "0",
        CodeSignature.GET_GAMETE,
        [],
        manifest,
        CHAIN_ID
    ))
}

const getNonceOf = (eoa: StorageReferenceModel, takamakaCode: TransactionReferenceModel) => {
    const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

    return remoteNode.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequestModel(
        eoa,
        "0",
        takamakaCode,
        "16999",
        "0",
        CodeSignature.NONCE,
        [],
        eoa,
        CHAIN_ID
    ))
}


const getGasStation = (manifest: StorageReferenceModel, takamakaCode: TransactionReferenceModel): Promise<StorageValueModel> => {
    const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

    return remoteNode.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequestModel(
        manifest,
        "0",
        takamakaCode,
        "100000",
        "0",
        CodeSignature.GET_GAS_STATION,
        [],
        manifest,
        CHAIN_ID
    ))
}


const getGasPrice = (manifest: StorageReferenceModel, takamakaCode: TransactionReferenceModel, gasStation: StorageValueModel) => {
    const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

    return remoteNode.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequestModel(
        manifest,
        "0",
        takamakaCode,
        "100000",
        "0",
        CodeSignature.GET_GAS_PRICE,
        [],
        gasStation.reference!,
        CHAIN_ID
    ))
}



