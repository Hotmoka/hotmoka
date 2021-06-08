import {expect} from "chai";
import {Signer} from "../src/internal/Signer"
import {InstanceMethodCallTransactionRequestModel} from "../src/models/requests/InstanceMethodCallTransactionRequestModel";
import {StorageReferenceModel} from "../src/models/values/StorageReferenceModel";
import {TransactionReferenceModel} from "../src/models/values/TransactionReferenceModel";
import {CodeSignature} from "../src/internal/lang/CodeSignature";

// load private key
Signer.loadPrivateKey("./test/keys/ed25519.pri")

describe('Testing the signed requests of the Hotmoka JS objects', () => {

    it('Signed string', async () => {
        const result = Signer.sign(Buffer.from("hello"))
        expect(result).to.be.eq("Zest4OcIbf6LLGkXPw7zOL4WTTSNUyRO/4ipi/UE6bVvdx8hRUl5nmjweF1/7TnIrrgtdhK8gWpu3XAz78H6Bw==")
    })

    it('new InstanceMethodCallTransactionRequestModel(..) NonVoidMethod = rO0ABXdIBQAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQABEwAACWdldEdhbWV0ZRIA', async () => {

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

        const signature = request.signature
        expect(signature).to.be.eq('M93FSC2jthDeWkcEDNFMnl7G88i9MXeTJfH0R2nzaHfodQNwqm9udQ2aPpZfKMNSQ1y2EvS1w0Eo1GcEzD2rDQ==')
    })
})


