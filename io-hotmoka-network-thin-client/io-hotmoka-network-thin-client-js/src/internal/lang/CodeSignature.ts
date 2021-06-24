import {VoidMethodSignatureModel} from "../../models/signatures/VoidMethodSignatureModel";
import {ClassType} from "./ClassType";
import {BasicType} from "./BasicType";
import {NonVoidMethodSignatureModel} from "../../models/signatures/NonVoidMethodSignatureModel";

export class CodeSignature {
    /**
     * The method {@code receive} of a payable contract, with an int argument.
     */
    public static readonly RECEIVE_INT = new VoidMethodSignatureModel("receive", ClassType.PAYABLE_CONTRACT.name, [BasicType.INT.name])

    /**
     * The method {@code receive} of a payable contract, with a long argument.
     */
    public static readonly RECEIVE_LONG = new VoidMethodSignatureModel("receive", ClassType.PAYABLE_CONTRACT.name, [BasicType.LONG.name])

    /**
     * The method {@code receive} of a payable contract, with a big integer argument.
     */
    public static readonly RECEIVE_BIG_INTEGER = new VoidMethodSignatureModel("receive", ClassType.PAYABLE_CONTRACT.name, [ClassType.BIG_INTEGER.name])

    /**
     * The method {@code getGamete} of the manifest.
     */
    public static readonly GET_GAMETE = new NonVoidMethodSignatureModel("getGamete", ClassType.MANIFEST.name, [], ClassType.ACCOUNT.name)

    /**
     * The method {@code nonce} of an account.
     */
    public static readonly NONCE = new NonVoidMethodSignatureModel( "nonce", ClassType.ACCOUNT.name, [], ClassType.BIG_INTEGER.name)

    /**
     * The method {@code getGasStation} of the manifest.
     */
    public static readonly GET_GAS_STATION = new NonVoidMethodSignatureModel("getGasStation", ClassType.MANIFEST.name, [], ClassType.GAS_STATION.name)

    /**
     * The method {@code getGasPrice} of the gas station.
     */
    public static readonly GET_GAS_PRICE = new NonVoidMethodSignatureModel("getGasPrice", ClassType.GAS_STATION.name, [], ClassType.BIG_INTEGER.name)

    /**
     * The method {@code ignoresGasPrice} of the gas station.
     */
    public static readonly IGNORES_GAS_PRICE = new NonVoidMethodSignatureModel("ignoresGasPrice", ClassType.GAS_STATION.name, [] , BasicType.BOOLEAN.name)

    /**
     * The method {@code getChainId} of the manifest.
     */
    public static readonly GET_CHAIN_ID = new NonVoidMethodSignatureModel("getChainId", ClassType.MANIFEST.name, [], ClassType.STRING.name)

    /**
     * The method {@code getMaxErrorLength} of the manifest.
     */
    public static readonly GET_MAX_ERROR_LENGTH = new NonVoidMethodSignatureModel("getMaxErrorLength", ClassType.MANIFEST.name, [], BasicType.INT.name)

    /**
     * The method {@code getMaxDependencies} of the manifest.
     */
    public static readonly GET_MAX_DEPENDENCIES = new NonVoidMethodSignatureModel("getMaxDependencies", ClassType.MANIFEST.name, [], BasicType.INT.name)

    /**
     * The method {@code getMaxCumulativeSizeOfDependencies} of the manifest.
     */
    public static readonly GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES = new NonVoidMethodSignatureModel("getMaxCumulativeSizeOfDependencies", ClassType.MANIFEST.name, [], BasicType.LONG.name)

    /**
     * The method {@code allowsSelfCharged} of the manifest.
     */
    public static readonly ALLOWS_SELF_CHARGED = new NonVoidMethodSignatureModel("allowsSelfCharged", ClassType.MANIFEST.name, [], BasicType.BOOLEAN.name)

    /**
     * The method {@code allowsUnsignedFaucet} of the manifest.
     */
    public static readonly ALLOWS_UNSIGNED_FAUCET = new NonVoidMethodSignatureModel("allowsUnsignedFaucet", ClassType.MANIFEST.name, [], BasicType.BOOLEAN.name)

    /**
     * The method {@code skipsVerification} of the manifest.
     */
    public static readonly SKIPS_VERIFICATION = new NonVoidMethodSignatureModel("skipsVerification", ClassType.MANIFEST.name,  [], BasicType.BOOLEAN.name)

    /**
     * The method {@code getSignature} of the manifest.
     */
    public static readonly GET_SIGNATURE = new NonVoidMethodSignatureModel("getSignature", ClassType.MANIFEST.name, [], ClassType.STRING.name)

    /**
     * The method {@code balance} of a contract.
     */
    public static readonly BALANCE = new NonVoidMethodSignatureModel("balance", ClassType.CONTRACT.name, [], ClassType.BIG_INTEGER.name)

    /**
     * The method {@code balanceRed} of a contract.
     */
    public static readonly BALANCE_RED = new NonVoidMethodSignatureModel("balanceRed", ClassType.CONTRACT.name, [], ClassType.BIG_INTEGER.name)

    /**
     * The method {@code getMaxFaucet} of the gamete.
     */
    public static readonly GET_MAX_FAUCET = new NonVoidMethodSignatureModel("getMaxFaucet", ClassType.GAMETE.name,[], ClassType.BIG_INTEGER.name)

    /**
     * The method {@code getMaxRedFaucet} of the gamete.
     */
    public static readonly GET_MAX_RED_FAUCET = new NonVoidMethodSignatureModel("getMaxRedFaucet", ClassType.GAMETE.name, [],  ClassType.BIG_INTEGER.name)

    /**
     * The method {@code getValidators} of the manifest.
     */
    public static readonly GET_VALIDATORS = new NonVoidMethodSignatureModel("getValidators", ClassType.MANIFEST.name, [], ClassType.VALIDATORS.name)

    /**
     * The method {@code getVersions} of the manifest.
     */
    public static readonly GET_VERSIONS = new NonVoidMethodSignatureModel("getVersions", ClassType.MANIFEST.name, [], ClassType.VERSIONS.name)

    /**
     * The method {@code getMaxGasPerTransaction} of the gas station.
     */
    public static readonly GET_MAX_GAS_PER_TRANSACTION = new NonVoidMethodSignatureModel("getMaxGasPerTransaction", ClassType.GAS_STATION.name, [], ClassType.BIG_INTEGER.name)

    /**
     * The method {@code getTargetGasAtReward} of the gas station.
     */
    public static readonly GET_TARGET_GAS_AT_REWARD = new NonVoidMethodSignatureModel("getTargetGasAtReward", ClassType.GAS_STATION.name, [],  ClassType.BIG_INTEGER.name)

    /**
     * The method {@code getInflation} of the gas station.
     */
    public static readonly GET_INFLATION = new NonVoidMethodSignatureModel("getInflation", ClassType.GAS_STATION.name, [],  BasicType.LONG.name)

    /**
     * The method {@code getOblivion} of the gas station.
     */
    public static readonly GET_OBLIVION = new NonVoidMethodSignatureModel("getOblivion", ClassType.GAS_STATION.name,  [], BasicType.LONG.name)

    /**
     * The method {@code getHeight} of the manifest.
     */
    public static readonly GET_HEIGHT = new NonVoidMethodSignatureModel("getHeight", ClassType.VALIDATORS.name, [], ClassType.BIG_INTEGER.name)

    /**
     * The method {@code getNumberOfTransactions} of the manifest.
     */
    public static readonly GET_NUMBER_OF_TRANSACTIONS = new NonVoidMethodSignatureModel("getNumberOfTransactions", ClassType.VALIDATORS.name, [], ClassType.BIG_INTEGER.name)

    /**
     * The method {@code getTicketForNewPoll} of the manifest.
     */
    public static readonly GET_TICKET_FOR_NEW_POLL = new NonVoidMethodSignatureModel("getTicketForNewPoll", ClassType.VALIDATORS.name, [], ClassType.BIG_INTEGER.name)

    /**
     * The method {@code getPolls} of the validators object.
     */
    public static readonly GET_POLLS = new NonVoidMethodSignatureModel("getPolls", ClassType.VALIDATORS.name, [], ClassType.STORAGE_SET_VIEW.name)

    /**
     * The method {@code getVerificationVersion} of the versions object.
     */
    public static readonly GET_VERIFICATION_VERSION = new NonVoidMethodSignatureModel("getVerificationVersion", ClassType.VERSIONS.name, [], BasicType.INT.name)

    /**
     * The method {@code id} of a validator.
     */
    public static readonly ID = new NonVoidMethodSignatureModel("id", ClassType.VALIDATOR.name, [], ClassType.STRING.name)
}