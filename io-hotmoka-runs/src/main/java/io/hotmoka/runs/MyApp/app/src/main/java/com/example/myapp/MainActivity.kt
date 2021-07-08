package com.example.myapp

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import io.hotmoka.network.thin.client.RemoteNodeClient
import io.hotmoka.network.thin.client.models.requests.InstanceMethodCallTransactionRequestModel
import io.hotmoka.network.thin.client.models.signatures.MethodSignatureModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    /**
     * The remote node url
     */
    private val HOTMOKA_REMOTE_NODE_URL = "10.0.2.2:8080"

    /**
     * The chain id
     */
    private var chainId = ""

    /**
     * The chain id
     */
    private var numOfValidators = ""

    /**
     * The gamete storage reference to set
     */
    private var gamete = StorageReferenceModel(
        TransactionReferenceModel(
            "local",
            ""
        ),
        "0"
    )

    /**
     * The manifest storage reference to set
     */
    private var manifest = StorageReferenceModel(
        TransactionReferenceModel(
            "local",
            ""
        ),
        "0"
    )

    /**
     * The validators storage reference to set
     */
    private var validators = StorageReferenceModel(
        TransactionReferenceModel(
            "local",
            ""
        ),
        "0"
    )

    /**
     * The shares storage reference to set
     */
    private var shares = StorageReferenceModel(
        TransactionReferenceModel(
            "local",
            ""
        ),
        "0"
    )

    /**
     * The transaction reference of the takamaka code jar example
     */
    private var takamakacode = TransactionReferenceModel(
        "local",
        ""
    )

    /**
     * The remote node client
     */
    private lateinit var remoteNodeClient: RemoteNodeClient


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize()
    }

    override fun onStart() {
        super.onStart()
        this.remoteNodeClient = RemoteNodeClient(HOTMOKA_REMOTE_NODE_URL)
    }

    override fun onStop() {
        super.onStop()
        this.remoteNodeClient.close()
    }


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    private fun initialize(){
        val takamakaCodeView = findViewById<TextView>(R.id.takamakaCode)
        val manifestView = findViewById<TextView>(R.id.manifest)
        val gameteView = findViewById<TextView>(R.id.gamete)
        val chainIdView = findViewById<TextView>(R.id.chainId)
        val validatorsView = findViewById<TextView>(R.id.validators)
        val numOfValidatorsView = findViewById<TextView>(R.id.numOfValidators)
        val validatorView = findViewById<TextView>(R.id.validator)

        GlobalScope.launch(Dispatchers.Main) {
            takamakacode = getTakamakacode()
            takamakaCodeView.setText(takamakacode.hash)

            manifest = getManifest()
            manifestView.setText(manifest.transaction.hash)

            gamete = getGamete()
            gameteView.setText(gamete.transaction.hash)

            chainId = getChainId()
            chainIdView.setText(chainId)

            validators = getValidators()
            validatorsView.setText(validators.transaction.hash)

            shares = getShares()

            numOfValidators = size()
            numOfValidatorsView.setText(numOfValidators)



            Log.d("takamakacode", takamakacode.hash)
            Log.d("manifest", manifest.transaction.hash)
            Log.d("gamete", gamete.transaction.hash)
            Log.d("chainID", chainId)
            Log.d("validators", validators.transaction.hash)
            Log.d("numberOfValidators", numOfValidators)

            val s = SpannableStringBuilder()

            for(i in 0 until numOfValidators.toInt()){
                val validator = select(i)

                Log.d("          validator #$i: ", validator.transaction.hash)

                val id = getId(validator)
                Log.d("          id: ", id)

                val power = getPower(validator)
                Log.d("          power: ", power)

                s
                    .bold{ append("validator #$i: ") }
                    .append(validator.transaction.hash)
                    .bold{ append("\nid: ") }
                    .append(id)
                    .bold{ append("\npower: ") }
                    .append("$power\n")

            }

            validatorView.setText(s)

        }
    }


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    /**
     * It calls the method getTakamakacode() which yields the reference of the Takamaka jar.
     */
    suspend fun getTakamakacode(): TransactionReferenceModel {
        return GlobalScope.async(Dispatchers.IO) {
            wrapExceptions {
                remoteNodeClient.getTakamakaCode()
            }
        }.await()
    }


    /**
     * It calls the method getManifest() which yields the manifest of the node.
     */
    suspend fun getManifest(): StorageReferenceModel {
        return GlobalScope.async(Dispatchers.IO) {
            wrapExceptions {
                remoteNodeClient.getManifest()
            }
        }.await()
    }



    /**
     * It calls the method GET_GAMETE.
     */
    suspend fun getGamete(): StorageReferenceModel {
        return GlobalScope.async(Dispatchers.IO) {

            val method = MethodSignatureModel(
                "getGamete",
                "io.takamaka.code.lang.Account",
                listOf(),
                "io.takamaka.code.system.Manifest"
            )

            val result = wrapExceptions {
                remoteNodeClient.runInstanceMethodCallTransaction(
                    InstanceMethodCallTransactionRequestModel(
                        "",
                        manifest,
                        "0",
                        takamakacode,
                        chainId,
                        "10000",
                        "0",
                        method,
                        listOf(),
                        manifest
                    )
                )
            }

            if (result != null) result.reference!! else StorageReferenceModel(
                transaction = TransactionReferenceModel(
                    "local",
                    ""
                ),
                progressive = "0"

            )

        }.await()
    }

    /**
     * It calls the method GET_CHAIN_ID.
     */
    suspend fun getChainId(): String {
        return GlobalScope.async(Dispatchers.IO) {

            val method = MethodSignatureModel(
                "getChainId",
                "java.lang.String",
                listOf(),
                "io.takamaka.code.system.Manifest"
            )

            val result = wrapExceptions {
                remoteNodeClient.runInstanceMethodCallTransaction(
                    InstanceMethodCallTransactionRequestModel(
                        "",
                        manifest,
                        "0",
                        takamakacode,
                        chainId,
                        "10000",
                        "0",
                        method,
                        listOf(),
                        manifest
                    )
                )
            }

            if (result != null) result.value!! else ""

        }.await()
    }



    /**
     * It calls the method GET_VALIDATORS.
     */
    suspend fun getValidators(): StorageReferenceModel {
        return GlobalScope.async(Dispatchers.IO) {

            val method = MethodSignatureModel(
                "getValidators",
                "io.takamaka.code.system.Validators",
                listOf(),
                "io.takamaka.code.system.Manifest"
            )

            val result = wrapExceptions {
                remoteNodeClient.runInstanceMethodCallTransaction(
                    InstanceMethodCallTransactionRequestModel(
                        "",
                        manifest,
                        "0",
                        takamakacode,
                        chainId,
                        "10000",
                        "0",
                        method,
                        listOf(),
                        manifest
                    )
                )
            }

            if (result != null) result.reference!! else StorageReferenceModel(
                transaction = TransactionReferenceModel(
                    "local",
                    ""
                ),
                progressive = "0"

            )

        }.await()
    }



    /**
     * gets shares values
     */
    suspend fun getShares(): StorageReferenceModel {
        return GlobalScope.async(Dispatchers.IO) {

            val method = MethodSignatureModel(
                "getShares",
                "io.takamaka.code.util.StorageMapView",
                listOf(),
                "io.takamaka.code.system.Validators"
            )

            val result = wrapExceptions {
                remoteNodeClient.runInstanceMethodCallTransaction(
                    InstanceMethodCallTransactionRequestModel(
                        "",
                        manifest,
                        "0",
                        takamakacode,
                        chainId,
                        "10000",
                        "0",
                        method,
                        listOf(),
                        validators
                    )
                )
            }

            if (result != null) result.reference!! else StorageReferenceModel(
                transaction = TransactionReferenceModel(
                    "local",
                    ""
                ),
                progressive = "0"

            )

        }.await()
    }

    /**
     * returns size
     */
    suspend fun size(): String {
        return GlobalScope.async(Dispatchers.IO) {

            val method = MethodSignatureModel(
                "size",
                "int",
                listOf(),
                "io.takamaka.code.util.StorageMapView"
            )

            val result = wrapExceptions {
                remoteNodeClient.runInstanceMethodCallTransaction(
                    InstanceMethodCallTransactionRequestModel(
                        "",
                        manifest,
                        "0",
                        takamakacode,
                        chainId,
                        "10000",
                        "0",
                        method,
                        listOf(),
                        shares
                    )
                )
            }

            if (result != null) result.value!! else ""

        }.await()
    }


    /**
     * It calls the method select.
     */
    suspend fun select(num : Int): StorageReferenceModel {
        return GlobalScope.async(Dispatchers.IO) {

            val method = MethodSignatureModel(
                "select",
                "java.lang.Object",
                listOf("int"),
                "io.takamaka.code.util.StorageMapView"
            )

            val actuals = listOf(
                StorageValueModel("int", num.toString())
            )

            val result = wrapExceptions {
                remoteNodeClient.runInstanceMethodCallTransaction(
                    InstanceMethodCallTransactionRequestModel(
                        "",
                        manifest,
                        "0",
                        takamakacode,
                        chainId,
                        "10000",
                        "0",
                        method,
                        actuals,
                        shares
                    )
                )
            }

            if (result != null) result.reference!! else StorageReferenceModel(
                transaction = TransactionReferenceModel(
                    "local",
                    ""
                ),
                progressive = "0"

            )

        }.await()
    }

    /**
     * returns id
     */
    suspend fun getId(validator : StorageReferenceModel): String {
        return GlobalScope.async(Dispatchers.IO) {

            val method = MethodSignatureModel(
                "id",
                "java.lang.String",
                listOf(),
                "io.takamaka.code.system.Validator"
            )

            val result = wrapExceptions {
                remoteNodeClient.runInstanceMethodCallTransaction(
                    InstanceMethodCallTransactionRequestModel(
                        "",
                        manifest,
                        "0",
                        takamakacode,
                        chainId,
                        "10000",
                        "0",
                        method,
                        listOf(),
                        validator
                    )
                )
            }

            if (result != null) result.value!! else ""

        }.await()
    }

    /**
     * returns power
     */
    suspend fun getPower(validator : StorageReferenceModel): String {
        return GlobalScope.async(Dispatchers.IO) {

            val method = MethodSignatureModel(
                "get",
                "java.lang.Object",
                listOf("java.lang.Object"),
                "io.takamaka.code.util.StorageMapView"
            )

            val actuals = listOf(
                StorageValueModel("reference", "", validator)
            )

            val result = wrapExceptions {
                remoteNodeClient.runInstanceMethodCallTransaction(
                    InstanceMethodCallTransactionRequestModel(
                        "",
                        manifest,
                        "0",
                        takamakacode,
                        chainId,
                        "10000",
                        "0",
                        method,
                        actuals,
                        shares
                    )
                )
            }

            if (result != null) result.value!! else ""

        }.await()
    }





///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    /**
     * Wrapper function to invoke a function that throws an exception.
     */
    fun <T> wrapExceptions(func: (() -> T)): T {
        try {
            return func.invoke()
        } catch (e: Exception) {
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Error REST method response", Toast.LENGTH_SHORT)
                    .show()
            }
            throw RuntimeException(e)
        }

    }

}
