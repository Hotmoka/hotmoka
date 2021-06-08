import {expect} from "chai";
import {Signer} from "../src/internal/Signer"
import {InstanceMethodCallTransactionRequestModel} from "../src/models/requests/InstanceMethodCallTransactionRequestModel";
import {StorageReferenceModel} from "../src/models/values/StorageReferenceModel";
import {TransactionReferenceModel} from "../src/models/values/TransactionReferenceModel";
import {CodeSignature} from "../src/internal/lang/CodeSignature";
import {VoidMethodSignatureModel} from "../src/models/signatures/VoidMethodSignatureModel";
import {ClassType} from "../src/internal/lang/ClassType";
import {BasicType} from "../src/internal/lang/BasicType";
import {StorageValueModel} from "../src/models/values/StorageValueModel";
import {JarStoreTransactionRequestModel} from "../src/models/requests/JarStoreTransactionRequestModel";
import * as fs from "fs";
import * as path from "path";
import {StaticMethodCallTransactionRequestModel} from "../src/models/requests/StaticMethodCallTransactionRequestModel";
import {NonVoidMethodSignatureModel} from "../src/models/signatures/NonVoidMethodSignatureModel";
import {ConstructorSignatureModel} from "../src/models/signatures/ConstructorSignatureModel";
import {ConstructorCallTransactionRequestModel} from "../src/models/requests/ConstructorCallTransactionRequestModel";


const HOTMOKA_VERSION = "1.0.0"

// load private key
Signer.loadPrivateKey("./test/keys/ed25519.pri")

describe('Testing the signed requests of the Hotmoka JS objects', () => {

    it('Signed string', async () => {
        const result = Signer.sign(Buffer.from("hello"))
        expect(result).to.be.eq("Zest4OcIbf6LLGkXPw7zOL4WTTSNUyRO/4ipi/UE6bVvdx8hRUl5nmjweF1/7TnIrrgtdhK8gWpu3XAz78H6Bw==")
    })

    it('new ConstructorCallTransactionRequestModel(..) = ss3eEquOXh2a2t3CERB2Eth91T5GL8KxYn6DS3uG9mVBe1CulbP/geqXmHg+sCD2ql56iG9jG9nQ7VVD9lOaBA==', async () => {

        const constructorSignature = new ConstructorSignatureModel(
            ClassType.MANIFEST.name,
            [ClassType.BIG_INTEGER.name]
        )

        const request = new ConstructorCallTransactionRequestModel(
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "1",
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "11500",
            "500",
            constructorSignature,
            [StorageValueModel.newStorageValue("999", ClassType.BIG_INTEGER.name)],
            "chaintest"
        )

        expect(request.signature).to.be.eq('ss3eEquOXh2a2t3CERB2Eth91T5GL8KxYn6DS3uG9mVBe1CulbP/geqXmHg+sCD2ql56iG9jG9nQ7VVD9lOaBA==')
    })

    it('new InstanceMethodCallTransactionRequestModel(..) NonVoidMethod = M93FSC2jthDeWkcEDNFMnl7G88i9MXeTJfH0R2nzaHfodQNwqm9udQ2aPpZfKMNSQ1y2EvS1w0Eo1GcEzD2rDQ==', async () => {

        const request = new InstanceMethodCallTransactionRequestModel(
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "1",
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "5000",
            "4000",
            CodeSignature.GET_GAMETE,
            [],
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "chaintest"
        )

        expect(request.signature).to.be.eq('M93FSC2jthDeWkcEDNFMnl7G88i9MXeTJfH0R2nzaHfodQNwqm9udQ2aPpZfKMNSQ1y2EvS1w0Eo1GcEzD2rDQ==')
    })

    it('new InstanceMethodCallTransactionRequestModel(..) VoidMethod = daUNn5acSc8knsDGOHD11EO1Mrai6rvJpd/F2TBlwxcCrzqJg49G8HqG4dKoU9dxHOkeIh2anCNG5Fdx5VOJDA==', async () => {

        const RECEIVE_INT = new VoidMethodSignatureModel(
            "receive",
            ClassType.PAYABLE_CONTRACT.name,
            [BasicType.INT.name]
        )

        const request = new InstanceMethodCallTransactionRequestModel(
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "1",
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "5000",
            "4000",
            RECEIVE_INT,
            [StorageValueModel.newStorageValue("300", BasicType.INT.name)],
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "chaintest"
        )

        expect(request.signature).to.be.eq('daUNn5acSc8knsDGOHD11EO1Mrai6rvJpd/F2TBlwxcCrzqJg49G8HqG4dKoU9dxHOkeIh2anCNG5Fdx5VOJDA==')
    })


    it('new StaticMethodCallTransactionRequestModel(..) NonVoidMethod = kGANXzM4vsYs/HPhFb+NdfAY10clY/qHbnX3C0oIvip1w8zKXMeymLsl3XLsXbpEsRR5IvfOppIL6B8sa4qhCw==', async () => {

        const request = new StaticMethodCallTransactionRequestModel(
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "1",
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "5000",
            "4000",
            CodeSignature.NONCE,
            [],
            "chaintest"
        )

        expect(request.signature).to.be.eq('kGANXzM4vsYs/HPhFb+NdfAY10clY/qHbnX3C0oIvip1w8zKXMeymLsl3XLsXbpEsRR5IvfOppIL6B8sa4qhCw==')
    })

    it('new StaticMethodCallTransactionRequestModel(..) VoidMethod = Kfrc1vksJppDGsNBOC1dmhm6QGr9g5CaY9mjplBHzmBxplbUWsFQ7Aif6oWJ76ROIq41f0UNltKlxQJ8uYi9BA==', async () => {

        const RECEIVE_INT = new VoidMethodSignatureModel(
            "receive",
            ClassType.PAYABLE_CONTRACT.name,
            [BasicType.INT.name]
        )

        const request = new StaticMethodCallTransactionRequestModel(
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "1",
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "5000",
            "4000",
            RECEIVE_INT,
            [StorageValueModel.newStorageValue("300", BasicType.INT.name)],
            "chaintest"
        )

        expect(request.signature).to.be.eq('Kfrc1vksJppDGsNBOC1dmhm6QGr9g5CaY9mjplBHzmBxplbUWsFQ7Aif6oWJ76ROIq41f0UNltKlxQJ8uYi9BA==')
    })

    it('new StaticMethodCallTransactionRequestModel(..) NonVoidMethod gas station = Pq+zM4j1WeB/eiUGohYrHuz8ZdXYTPIJrdBn/hEvrxyukvd1onqeXbyVF5CXcXvoEP1NjleEhxLHItjFsd/4BA==', async () => {

        const nonVoidMethodSignature = new NonVoidMethodSignatureModel(
            "balance",
            ClassType.GAS_STATION.name,
            [ClassType.STORAGE.name],
            ClassType.BIG_INTEGER.name
        )

        const request = new StaticMethodCallTransactionRequestModel(
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "1",
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "5000",
            "4000",
            nonVoidMethodSignature,
            [StorageValueModel.newReference(new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ))],
            "chaintest"
        )

        expect(request.signature).to.be.eq('Pq+zM4j1WeB/eiUGohYrHuz8ZdXYTPIJrdBn/hEvrxyukvd1onqeXbyVF5CXcXvoEP1NjleEhxLHItjFsd/4BA==')
    })

    it.skip('new JarStoreTransactionRequestModel(..) = Eb/8lnQZ7C5mPVJAq9wYFjz59VPp9MR05XpRLcubNw1GIKJw+NTAreGdUD3JFCDv8lZCrDzKW3iiCRGWGRq4BA==', async () => {

        const request = new JarStoreTransactionRequestModel(
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "1",
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "5000",
            "4000",
            getLocalJar('lambdas.jar').toString('base64'),
            [],
            "chaintest"
        )

        expect(request.signature).to.be.eq('Eb/8lnQZ7C5mPVJAq9wYFjz59VPp9MR05XpRLcubNw1GIKJw+NTAreGdUD3JFCDv8lZCrDzKW3iiCRGWGRq4BA==')
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