export class ClassType {
    /**
     * The frequently used class type for {@link java.lang.Object}.
     */
    public static readonly OBJECT = new ClassType("java.lang.Object")

    /**
     * The frequently used class type for {@link java.lang.String}.
     */
    public static readonly STRING = new ClassType("java.lang.String")

    /**
     * The frequently used class type for {@link java.math.BigInteger}.
     */
    public static readonly BIG_INTEGER = new ClassType("java.math.BigInteger")

    /**
     * The frequently used class type for {@link io.takamaka.code.math.UnsignedBigInteger}.
     */
    public static readonly UNSIGNED_BIG_INTEGER = new ClassType("io.takamaka.code.math.UnsignedBigInteger")

    /**
     * The frequently used class type for {@link io.takamaka.code.tokens.ERC20}.
     */
    public static readonly ERC20 = new ClassType("io.takamaka.code.tokens.ERC20")

    /**
     * The frequently used class type for {@link io.takamaka.code.governance.GasPriceUpdate}.
     */
    public static readonly GAS_PRICE_UPDATE = new ClassType("io.takamaka.code.governance.GasPriceUpdate")

    /**
     * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccount}.
     */
    public static readonly EOA = new ClassType("io.takamaka.code.lang.ExternallyOwnedAccount")

    /**
     * The frequently used class type for {@link io.takamaka.code.lang.Contract}.
     */
    public static readonly CONTRACT = new ClassType("io.takamaka.code.lang.Contract")

    /**
     * The frequently used class type for {@link io.takamaka.code.lang.Gamete}.
     */
    public static readonly GAMETE = new ClassType("io.takamaka.code.lang.Gamete")

    /**
     * The frequently used class type for {@link io.takamaka.code.lang.Account}.
     */
    public static readonly ACCOUNT = new ClassType("io.takamaka.code.lang.Account")

    /**
     * The frequently used class type for {@link io.takamaka.code.lang.Accounts}.
     */
    public static readonly ACCOUNTS = new ClassType("io.takamaka.code.lang.Accounts")

    /**
     * The frequently used class type for {@link io.takamaka.code.tokens.IERC20}.
     */
    public static readonly IERC20 = new ClassType("io.takamaka.code.tokens.IERC20")

    /**
     * The frequently used class type for {@link io.takamaka.code.governance.Manifest}.
     */
    public static readonly MANIFEST = new ClassType("io.takamaka.code.governance.Manifest")

    /**
     * The frequently used class type for {@link io.takamaka.code.governance.Validator}.
     */
    public static readonly VALIDATOR = new ClassType("io.takamaka.code.governance.Validator")

    /**
     * The frequently used class type for {@link io.takamaka.code.governance.Validators}.
     */
    public static readonly VALIDATORS = new ClassType("io.takamaka.code.governance.Validators")

    /**
     * The frequently used class type for {@link io.takamaka.code.governance.Versions}.
     */
    public static readonly VERSIONS = new ClassType("io.takamaka.code.governance.Versions")

    /**
     * The frequently used class type for {@link io.takamaka.code.governance.GasStation}.
     */
    public static readonly GAS_STATION = new ClassType("io.takamaka.code.governance.GasStation")

    /**
     * The frequently used class type for {@link io.takamaka.code.governance.GenericGasStation}.
     */
    public static readonly GENERIC_GAS_STATION = new ClassType("io.takamaka.code.governance.GenericGasStation")

    /**
     * The frequently used class type for {@link io.takamaka.code.governance.tendermint.TendermintValidators}.
     */
    public static readonly TENDERMINT_VALIDATORS = new ClassType("io.takamaka.code.governance.tendermint.TendermintValidators")

    /**
     * The frequently used class type for {@link io.takamaka.code.lang.Storage}.
     */
    public static readonly STORAGE = new ClassType("io.takamaka.code.lang.Storage")

    /**
     * The frequently used class type for {@link io.takamaka.code.lang.Takamaka}.
     */
    public static readonly TAKAMAKA = new ClassType("io.takamaka.code.lang.Takamaka")

    /**
     * The frequently used class type for {@link io.takamaka.code.lang.Event}.
     */
    public static readonly EVENT = new ClassType("io.takamaka.code.lang.Event")

    /**
     * The frequently used class type for {@link io.takamaka.code.lang.PayableContract}.
     */
    public static readonly PAYABLE_CONTRACT = new ClassType("io.takamaka.code.lang.PayableContract")

    /**
     * The frequently used class type for {@code io.takamaka.code.lang.FromContract}.
     */
    public static readonly FROM_CONTRACT = new ClassType("io.takamaka.code.lang.FromContract")

    /**
     * The frequently used class type for {@link io.takamaka.code.lang.View}.
     */
    public static readonly VIEW = new ClassType("io.takamaka.code.lang.View")

    /**
     * The frequently used class type for {@link io.takamaka.code.lang.Payable}.
     */
    public static readonly PAYABLE = new ClassType("io.takamaka.code.lang.Payable")

    /**
     * The frequently used class type for {@link io.takamaka.code.lang.ThrowsExceptions}.
     */
    public static readonly THROWS_EXCEPTIONS = new ClassType("io.takamaka.code.lang.ThrowsExceptions")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.Bytes32}.
     */
    public static readonly BYTES32 = new ClassType("io.takamaka.code.util.Bytes32");

    /**
     * The frequently used class type for {@link io.takamaka.code.util.Bytes32Snapshot}.
     */
    public static readonly BYTES32_SNAPSHOT = new ClassType("io.takamaka.code.util.Bytes32Snapshot");

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageArray}.
     */
    public static readonly STORAGE_ARRAY = new ClassType("io.takamaka.code.util.StorageArray")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageListView}.
     */
    public static readonly STORAGE_LIST = new ClassType("io.takamaka.code.util.StorageListView")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageLinkedList}.
     */
    public static readonly STORAGE_LINKED_LIST = new ClassType("io.takamaka.code.util.StorageLinkedList")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageMapView}.
     */
    public static readonly STORAGE_MAP = new ClassType("io.takamaka.code.util.StorageMapView")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap}.
     */
    public static readonly STORAGE_TREE_MAP = new ClassType("io.takamaka.code.util.StorageTreeMap")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageTreeArray}.
     */
    public static readonly STORAGE_TREE_ARRAY = new ClassType("io.takamaka.code.util.StorageTreeArray")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageTreeArray.Node}.
     */
    public static readonly STORAGE_TREE_ARRAY_NODE = new ClassType("io.takamaka.code.util.StorageTreeArray.Node")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageTreeIntMap}.
     */
    public static readonly STORAGE_TREE_INTMAP = new ClassType("io.takamaka.code.util.StorageTreeIntMap")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageTreeSet}.
     */
    public static readonly STORAGE_TREE_SET = new ClassType("io.takamaka.code.util.StorageTreeSet")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.BlackNode}.
     */
    public static readonly STORAGE_TREE_MAP_BLACK_NODE = new ClassType("io.takamaka.code.util.StorageTreeMap.BlackNode")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.RedNode}.
     */
    public static readonly STORAGE_TREE_MAP_RED_NODE = new ClassType("io.takamaka.code.util.StorageTreeMap.RedNode")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageSetView}.
     */
    public static readonly STORAGE_SET_VIEW = new ClassType("io.takamaka.code.util.StorageSetView")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageMap}.
     */
    public static readonly MODIFIABLE_STORAGE_MAP = new ClassType("io.takamaka.code.util.StorageMap")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageLinkedList.Node}.
     */
    public static readonly STORAGE_LINKED_LIST_NODE = new ClassType("io.takamaka.code.util.StorageLinkedList.Node")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.Node}.
     */
    public static readonly STORAGE_TREE_MAP_NODE = new ClassType("io.takamaka.code.util.StorageTreeMap.Node")

    /**
     * The frequently used class type for {@link io.takamaka.code.util.StorageTreeIntMap.Node}.
     */
    public static readonly STORAGE_TREE_INTMAP_NODE = new ClassType("io.takamaka.code.util.StorageTreeIntMap.Node")

    /**
     * The frequently used class type for {@link io.takamaka.code.governance.GenericValidators}.
     */
    public static readonly GENERIC_VALIDATORS = new ClassType("io.takamaka.code.governance.GenericValidators")

    /**
     * The frequently used class type for {@link io.takamaka.code.dao.Poll}.
     */
    public static readonly POLL = new ClassType("io.takamaka.code.dao.Poll")

    /**
     * The frequently used class type for {@link io.takamaka.code.dao.SharedEntity}.
     */
    public static readonly SHARED_ENTITY =  new ClassType("io.takamaka.code.dao.SharedEntity")

    /**
     * The frequently used class type for {@link io.takamaka.code.dao.SharedEntityView}.
     */
    public static readonly SHARED_ENTITY_VIEW =  new ClassType("io.takamaka.code.dao.SharedEntityView")

    /**
     * The name of the class type.
     */
    public readonly name: string

    private constructor(name: string) {
       this.name = name
    }
}