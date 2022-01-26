package io.hotmoka.tests;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.*;
import org.junit.jupiter.api.*;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Objects;

import static io.hotmoka.beans.Coin.*;

/**
 * A test for the wine package.
 */
class WineTest extends TakamakaTest {
    /**
     * Class types and most used constructors signatures
     */
    private static final ClassType SUPPLYCHAIN = new ClassType("io.hotmoka.examples.wine.staff.SupplyChain");
    private static final ConstructorSignature CONSTRUCTOR_SUPPLYCHAIN = new ConstructorSignature(SUPPLYCHAIN,
            ClassType.EOA);

    private static final ClassType RESOURCE = new ClassType("io.hotmoka.examples.wine.resources.Resource");
    private static final ClassType VINE = new ClassType("io.hotmoka.examples.wine.resources.Vine");
    private static final ClassType GRAPE = new ClassType("io.hotmoka.examples.wine.resources.Grape");
    private static final ClassType GRAPE_STATE = new ClassType("io.hotmoka.examples.wine.resources.GrapeState");
    private static final ClassType WINE = new ClassType("io.hotmoka.examples.wine.resources.Wine");
    private static final ClassType BOTTLE = new ClassType("io.hotmoka.examples.wine.resources.Bottle");
    private static final ClassType CERTIFICATION = new ClassType("io.hotmoka.examples.wine.resources.Certification");

    private static final ClassType STAFF = new ClassType("io.hotmoka.examples.wine.staff.Staff");
    private static final ClassType ADMINISTRATOR = new ClassType("io.hotmoka.examples.wine.staff.Administrator");
    private static final ConstructorSignature CONSTRUCTOR_ADMINISTRATOR = new ConstructorSignature(ADMINISTRATOR,
            ClassType.EOA, ClassType.STRING);
    private static final VoidMethodSignature ADD_STAFF =
            new VoidMethodSignature(SUPPLYCHAIN, "add", STAFF, ADMINISTRATOR);
    private static final VoidMethodSignature REMOVE_STAFF =
            new VoidMethodSignature(SUPPLYCHAIN, "remove", STAFF, ADMINISTRATOR);

    private static final ClassType AUTHORITY = new ClassType("io.hotmoka.examples.wine.staff.Authority");
    private static final ConstructorSignature CONSTRUCTOR_AUTHORITY = new ConstructorSignature(AUTHORITY,
            ClassType.EOA, ClassType.STRING);

    private static final ClassType WORKER = new ClassType("io.hotmoka.examples.wine.staff.Worker");
    private static final ClassType ROLE = new ClassType("io.hotmoka.examples.wine.staff.Role");
    private static final ConstructorSignature CONSTRUCTOR_WORKER = new ConstructorSignature(WORKER,
            ClassType.EOA, ClassType.STRING, ROLE, BasicTypes.INT);

    /**
     * The owner and the supply chain.
     */
    private StorageReference owner;
    private PrivateKey owner_prv_key;
    private StorageReference chain;

    /**
     * An administrator.
     */
    private StorageReference administrator;
    private PrivateKey administrator_prv_key;
    private StorageReference administrator_obj;

    /**
     * An authority.
     */
    private StorageReference authority;
    private PrivateKey authority_prv_key;
    private StorageReference authority_obj;

    /**
     * A producer.
     */
    private StorageReference producer;
    private PrivateKey producer_prv_key;
    private StorageReference producer_obj;

    /**
     * A wine-making centre.
     */
    private StorageReference wine_making_centre;
    private PrivateKey wine_making_centre_prv_key;
    private StorageReference wine_making_centre_obj;

    /**
     * A bottling centre.
     */
    private StorageReference bottling_centre;
    private PrivateKey bottling_centre_prv_key;
    private StorageReference bottling_centre_obj;

    /**
     * A distribution centre.
     */
    private StorageReference distribution_centre;
    private PrivateKey distribution_centre_prv_key;
    private StorageReference distribution_centre_obj;

    /**
     * A retailer.
     */
    private StorageReference retailer;
    private PrivateKey retailer_prv_key;
    private StorageReference retailer_obj;

    @BeforeAll
    static void beforeAll() throws Exception {
        setJar("wine.jar");
    }

    @BeforeEach
    void setAccounts() throws Exception {
        setAccounts(stromboli(1), filicudi(100), filicudi(100), filicudi(100), filicudi(100), filicudi(100),
                filicudi(100), filicudi(100), filicudi(100));
        owner = account(0);
        producer = account(2);
        wine_making_centre = account(3);
        bottling_centre = account(4);
        distribution_centre = account(5);
        retailer = account(6);
        authority = account(7);
        administrator = account(8);
        owner_prv_key = privateKey(0);
        producer_prv_key = privateKey(2);
        wine_making_centre_prv_key = privateKey(3);
        bottling_centre_prv_key = privateKey(4);
        distribution_centre_prv_key = privateKey(5);
        retailer_prv_key = privateKey(6);
        authority_prv_key = privateKey(7);
        administrator_prv_key = privateKey(8);
    }

    // Creation of a SupplyChain and its Staff
    @BeforeEach
    void createSupplyChain()
            throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException,
            SignatureException {
        chain = addConstructorCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(),
                CONSTRUCTOR_SUPPLYCHAIN, owner);
        administrator_obj = addConstructorCallTransaction(owner_prv_key, owner, _500_000, panarea(1),
                jar(), CONSTRUCTOR_ADMINISTRATOR, administrator, new StringValue("Alessia"));
        authority_obj = addConstructorCallTransaction(administrator_prv_key, administrator, _500_000, panarea(1),
                jar(), CONSTRUCTOR_AUTHORITY, authority, new StringValue("Stefania"));
        producer_obj = newWorker(producer, producer_prv_key, "Mario", "PRODUCER", 5);
        wine_making_centre_obj = newWorker(wine_making_centre, wine_making_centre_prv_key, "Luigi",
                "WINE_MAKING_CENTRE", 10);
        bottling_centre_obj = newWorker(bottling_centre, bottling_centre_prv_key, "Cristina",
                "BOTTLING_CENTRE", 10);
        distribution_centre_obj = newWorker(distribution_centre, distribution_centre_prv_key,
                "Tommaso", "DISTRIBUTION_CENTRE", 20);
        retailer_obj = newWorker(retailer, retailer_prv_key, "Elena", "RETAILER", 5);
    }

    // Creation of a new Worker
    StorageReference newWorker(StorageReference account, PrivateKey account_prv_key,
                               String name, String role, int max_products)
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        return addConstructorCallTransaction(account_prv_key, account, _500_000, panarea(1), jar(),
                CONSTRUCTOR_WORKER, account, new StringValue(name), new EnumValue("io.hotmoka.examples.wine.staff.Role",
                        role), new IntValue(max_products));
    }

    // Creation of a new Resource for Workers
    StorageReference newProduct(StorageValue chain, StorageReference worker, StorageReference account,
                                PrivateKey account_prv_key, String name, String description, int amount,
                                StorageValue prevProduct)
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        return (StorageReference) addInstanceMethodCallTransaction(account_prv_key, account, _500_000, panarea(1),
                jar(), new NonVoidMethodSignature(WORKER, "addProduct", RESOURCE, SUPPLYCHAIN, ClassType.STRING,
                        ClassType.STRING, BasicTypes.INT, RESOURCE), worker, chain,
                new StringValue(name), new StringValue(description), new IntValue(amount),
                prevProduct);
    }

    // Transfer a Resource from a Worker to the following
    StorageReference transferProduct(StorageReference chain, StorageReference worker, StorageReference account,
                                     PrivateKey account_prv_key, StorageValue product)
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        return (StorageReference) addInstanceMethodCallTransaction(account_prv_key, account, _500_000, panarea(1),
                jar(), new NonVoidMethodSignature(SUPPLYCHAIN, "transferProduct", WORKER, RESOURCE, WORKER), chain,
                product, worker);
    }

    // Create products and transfer them until they arrive to Worker with role X
    // and return the last product created or transferred
    StorageReference createInteractionsUntil(String role)
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        StorageReference vine, grape, must, wine, bottle;
        // Add everyone to the Staff
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, authority_obj, NullValue.INSTANCE);
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, producer_obj, NullValue.INSTANCE);
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, wine_making_centre_obj, NullValue.INSTANCE);
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, bottling_centre_obj, NullValue.INSTANCE);
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, distribution_centre_obj, NullValue.INSTANCE);
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, retailer_obj, NullValue.INSTANCE);

        // Create Vine
        vine = newProduct(chain, producer_obj, producer, producer_prv_key, "Vine", "", 1,
                NullValue.INSTANCE);
        // Create Grape and transfer it to the wine-making centre
        if (!Objects.equals(role, "PRODUCER")) {
            grape = newProduct(chain, producer_obj, producer, producer_prv_key, "Grape", "", 100,
                    vine);
            addInstanceMethodCallTransaction(authority_prv_key, authority, _500_000, panarea(1), jar(),
                    new VoidMethodSignature(GRAPE, "setState", GRAPE_STATE, AUTHORITY), grape,
                    new EnumValue(GRAPE_STATE.name, "ELIGIBLE"), authority_obj);
            transferProduct(chain, producer_obj, producer, producer_prv_key, grape);
        } else
            return vine;
        if (!Objects.equals(role, "WINE_MAKING_CENTRE")) {
            // Create Must and Wine from Grape and transfer it to the bottling centre
            must = newProduct(chain, wine_making_centre_obj, wine_making_centre,
                    wine_making_centre_prv_key, "Must", "", 100, grape);
            wine = newProduct(chain, wine_making_centre_obj, wine_making_centre,
                    wine_making_centre_prv_key, "Wine", "", 10, must);
            addInstanceMethodCallTransaction(authority_prv_key, authority, _500_000, panarea(1), jar(),
                    new VoidMethodSignature(WINE, "setEligible", AUTHORITY), wine, authority_obj);
            transferProduct(chain, wine_making_centre_obj, wine_making_centre, wine_making_centre_prv_key, wine);
        } else
            return grape;
        if (!Objects.equals(role, "BOTTLING_CENTRE")) {
            // Create Bottle and transfer it to the distribution centre
            bottle = newProduct(chain, bottling_centre_obj, bottling_centre,
                    bottling_centre_prv_key, "Bottle", "", 10, wine);
            transferProduct(chain, bottling_centre_obj, bottling_centre, bottling_centre_prv_key, bottle);
        } else
            return wine;
        if (!Objects.equals(role, "DISTRIBUTION_CENTRE")) {
            // Transfer Bottles to retailers
            transferProduct(chain, distribution_centre_obj, distribution_centre, distribution_centre_prv_key,
                    bottle);
        }
        return bottle;
    }

    @Test
    @DisplayName("Creation of a SupplyChain with various Staff")
    void addRemoveStaff()
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        // Add an Administrator to the SupplyChain (by owner)
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF, chain,
                administrator_obj, NullValue.INSTANCE);

        // Add and remove an Authority from the SupplyChain (by an Administrator)
        addInstanceMethodCallTransaction(administrator_prv_key, administrator, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, authority_obj, administrator_obj);
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), REMOVE_STAFF, chain,
                authority_obj, administrator_obj);

        // Add and remove a Worker from the SupplyChain (by owner)
        addInstanceMethodCallTransaction(administrator_prv_key, administrator, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, producer_obj, administrator_obj);

        // Check that a new Worker is added in the list
        StorageReference workers =
                (StorageReference) addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(),
                        new NonVoidMethodSignature(SUPPLYCHAIN, "getWorkers", ClassType.STORAGE_LIST), chain);
        IntValue workers_size =
                (IntValue) addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(),
                        new NonVoidMethodSignature(ClassType.STORAGE_LIST, "size", BasicTypes.INT), workers);
        Assertions.assertEquals(1, workers_size.value);

        addInstanceMethodCallTransaction(administrator_prv_key, administrator, _500_000, panarea(1), jar(),
                REMOVE_STAFF, chain, producer_obj, administrator_obj);

        // Check that a Worker is removed from the list
        workers =
                (StorageReference) addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(),
                        new NonVoidMethodSignature(SUPPLYCHAIN, "getWorkers", ClassType.STORAGE_LIST), chain);
        workers_size =
                (IntValue) addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(),
                        new NonVoidMethodSignature(ClassType.STORAGE_LIST, "size", BasicTypes.INT), workers);
        Assertions.assertEquals(0, workers_size.value);

        // Remove an Administrator from the SupplyChain
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), REMOVE_STAFF, chain,
                administrator_obj, NullValue.INSTANCE);
    }

    @Test
    @DisplayName("Transfer a Resource")
    void transferProduct()
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        // Create a Grape from Vine and transfer it
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, producer_obj, NullValue.INSTANCE);
        StorageReference vine = newProduct(chain, producer_obj, producer, producer_prv_key, "Vine", "", 1,
                NullValue.INSTANCE);
        StorageReference grape = newProduct(chain, producer_obj, producer, producer_prv_key, "Grape", "", 100,
                vine);

        // EXCEPTION: Grape isn't ELIGIBLE
        Assertions.assertThrows(TransactionException.class,
                () -> transferProduct(chain, producer_obj, producer, producer_prv_key, grape));

        // Call Authority to make it eligible
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, authority_obj, NullValue.INSTANCE);
        addInstanceMethodCallTransaction(authority_prv_key, authority, _500_000, panarea(1), jar(),
                new VoidMethodSignature(GRAPE, "setState", GRAPE_STATE, AUTHORITY), grape,
                new EnumValue(GRAPE_STATE.name, "ELIGIBLE"), authority_obj);

        // EXCEPTION: No next Worker can be found
        Assertions.assertThrows(TransactionException.class,
                () -> transferProduct(chain, producer_obj, producer, producer_prv_key, grape));

        // Add a wine-making centre to the Staff
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, wine_making_centre_obj, NullValue.INSTANCE);

        // SUCCESS
        StorageReference next_worker = transferProduct(chain, producer_obj, producer, producer_prv_key, grape);
        Assertions.assertEquals(next_worker, wine_making_centre_obj);

        // EXCEPTION: Worker doesn't possess the Resource anymore
        Assertions.assertThrows(TransactionException.class,
                () -> transferProduct(chain, producer_obj, producer, producer_prv_key, grape));

        // Check that a new Resource is added to the products list of the receiver
        StorageReference products =
                (StorageReference) addInstanceMethodCallTransaction(wine_making_centre_prv_key, wine_making_centre,
                        _500_000, panarea(1), jar(), new NonVoidMethodSignature(WORKER, "getProducts",
                                ClassType.STORAGE_LIST), wine_making_centre_obj);
        IntValue products_size =
                (IntValue) addInstanceMethodCallTransaction(wine_making_centre_prv_key, wine_making_centre, _500_000,
                        panarea(1), jar(), new NonVoidMethodSignature(ClassType.STORAGE_LIST, "size",
                                BasicTypes.INT), products);
        Assertions.assertEquals(1, products_size.value);

        //  Check that the producers list of Grape is updated when it is transferred
        StorageReference producers =
                (StorageReference) addInstanceMethodCallTransaction(wine_making_centre_prv_key, wine_making_centre,
                        _500_000, panarea(1), jar(), new NonVoidMethodSignature(RESOURCE, "getProducers",
                                ClassType.STORAGE_LIST), grape);
        IntValue producers_size =
                (IntValue) addInstanceMethodCallTransaction(wine_making_centre_prv_key, wine_making_centre, _500_000,
                        panarea(1), jar(), new NonVoidMethodSignature(ClassType.STORAGE_LIST, "size", BasicTypes.INT)
                        , producers);
        Assertions.assertEquals(2, producers_size.value);

        // Create Must and Wine from Grape and transfer it to the bottling centre
        StorageReference must = newProduct(chain, wine_making_centre_obj, wine_making_centre,
                wine_making_centre_prv_key, "Must", "", 100, grape);
        StorageReference wine = newProduct(chain, wine_making_centre_obj, wine_making_centre,
                wine_making_centre_prv_key, "Wine", "", 10, must);
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, bottling_centre_obj, NullValue.INSTANCE);

        // EXCEPTION: Wine isn't ELIGIBLE
        Assertions.assertThrows(TransactionException.class,
                () -> transferProduct(chain, wine_making_centre_obj, wine_making_centre, wine_making_centre_prv_key,
                        wine));

        // Call Authority to make it eligible
        addInstanceMethodCallTransaction(authority_prv_key, authority, _500_000, panarea(1), jar(),
                new VoidMethodSignature(WINE, "setEligible", AUTHORITY), wine, authority_obj);

        // SUCCESS
        next_worker =
                transferProduct(chain, wine_making_centre_obj, wine_making_centre, wine_making_centre_prv_key, wine);
        Assertions.assertEquals(next_worker, bottling_centre_obj);

        // Create Bottle and transfer it to the distribution centre
        StorageReference bottle = newProduct(chain, bottling_centre_obj, bottling_centre,
                bottling_centre_prv_key, "Bottle", "", 10, wine);
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, distribution_centre_obj, NullValue.INSTANCE);
        next_worker = transferProduct(chain, bottling_centre_obj, bottling_centre, bottling_centre_prv_key, bottle);
        Assertions.assertEquals(next_worker, distribution_centre_obj);

        // Transfer Bottles to retailers
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, retailer_obj, NullValue.INSTANCE);
        next_worker = transferProduct(chain, distribution_centre_obj, distribution_centre, distribution_centre_prv_key,
                bottle);
        Assertions.assertEquals(retailer_obj, next_worker);

        // EXCEPTION: Transferring a Resource from a Retailer isn't possible
        Assertions.assertThrows(TransactionException.class,
                () -> transferProduct(chain, retailer_obj, retailer, retailer_prv_key,
                        bottle));
    }

    @Test
    @DisplayName("Call Authority to make a Resource eligible")
    void callAuthority()
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        // Create a Grape from Vine
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, producer_obj, NullValue.INSTANCE);
        StorageReference vine = newProduct(chain, producer_obj, producer, producer_prv_key, "Vine", "", 1,
                NullValue.INSTANCE);
        StorageReference grape = newProduct(chain, producer_obj, producer, producer_prv_key, "Grape", "", 100,
                vine);

        // Check Grape isn't eligible
        StorageValue grape_state = addInstanceMethodCallTransaction(producer_prv_key, producer, _500_000, panarea(1),
                jar(), new NonVoidMethodSignature(GRAPE, "getState", GRAPE_STATE), grape);
        Assertions.assertEquals(grape_state, NullValue.INSTANCE);

        // Call Authority to make it eligible
        // EXCEPTION: There's no Authority the SupplyChain
        Assertions.assertThrows(TransactionException.class,
                () -> addInstanceMethodCallTransaction(producer_prv_key, producer, _500_000, panarea(1), jar(),
                        new VoidMethodSignature(SUPPLYCHAIN, "callAuthority", RESOURCE, WORKER), chain,
                        grape, producer_obj));

        // SUCCESS
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, authority_obj, NullValue.INSTANCE);
        addInstanceMethodCallTransaction(producer_prv_key, producer, _500_000, panarea(1), jar(),
                new VoidMethodSignature(SUPPLYCHAIN, "callAuthority", RESOURCE, WORKER), chain,
                grape, producer_obj);

        // Check that a new Resource is added after the call
        StorageReference products =
                (StorageReference) addInstanceMethodCallTransaction(authority_prv_key, authority, _500_000, panarea(1),
                        jar(), new NonVoidMethodSignature(AUTHORITY, "getProducts", ClassType.STORAGE_LIST),
                        authority_obj);
        IntValue products_size =
                (IntValue) addInstanceMethodCallTransaction(authority_prv_key, authority, _500_000, panarea(1), jar(),
                        new NonVoidMethodSignature(ClassType.STORAGE_LIST, "size", BasicTypes.INT), products);
        Assertions.assertEquals(1, products_size.value);

        addInstanceMethodCallTransaction(authority_prv_key, authority, _500_000, panarea(1), jar(),
                new VoidMethodSignature(GRAPE, "setState", GRAPE_STATE, AUTHORITY), grape,
                new EnumValue(GRAPE_STATE.name, "ELIGIBLE"), authority_obj);

        // When Grape is already ELIGIBLE there's no sense in calling Authority
        Assertions.assertThrows(TransactionException.class,
                () -> addInstanceMethodCallTransaction(producer_prv_key, producer, _500_000, panarea(1), jar(),
                        new VoidMethodSignature(SUPPLYCHAIN, "callAuthority", RESOURCE, WORKER), chain,
                        grape, producer_obj));

        // Transfer Grape to the wine-making centre
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, wine_making_centre_obj, NullValue.INSTANCE);
        transferProduct(chain, producer_obj, producer, producer_prv_key, grape);

        // Create Must and Wine from Grape
        StorageReference must = newProduct(chain, wine_making_centre_obj, wine_making_centre,
                wine_making_centre_prv_key, "Must", "", 100, grape);
        StorageReference wine = newProduct(chain, wine_making_centre_obj, wine_making_centre,
                wine_making_centre_prv_key, "Wine", "", 10, must);
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, bottling_centre_obj, NullValue.INSTANCE);

        // Check Wine isn't eligible
        BooleanValue wine_state = (BooleanValue) addInstanceMethodCallTransaction(wine_making_centre_prv_key,
                wine_making_centre,
                _500_000, panarea(1), jar(), new NonVoidMethodSignature(WINE, "isEligible", BasicTypes.BOOLEAN), wine);
        Assertions.assertFalse(wine_state.value);

        // Call Authority to make it eligible
        addInstanceMethodCallTransaction(wine_making_centre_prv_key, wine_making_centre, _500_000, panarea(1), jar(),
                new VoidMethodSignature(SUPPLYCHAIN, "callAuthority", RESOURCE, WORKER), chain,
                wine, wine_making_centre_obj);
        addInstanceMethodCallTransaction(authority_prv_key, authority, _500_000, panarea(1), jar(),
                new VoidMethodSignature(WINE, "setEligible", AUTHORITY), wine, authority_obj);

        // Check Wine is eligible
        wine_state = (BooleanValue) addInstanceMethodCallTransaction(wine_making_centre_prv_key, wine_making_centre,
                _500_000, panarea(1), jar(), new NonVoidMethodSignature(WINE, "isEligible", BasicTypes.BOOLEAN), wine);
        Assertions.assertTrue(wine_state.value);

        // Only Grape and Wine can be set as eligible
        Assertions.assertThrows(TransactionException.class,
                () -> addInstanceMethodCallTransaction(wine_making_centre_prv_key, wine_making_centre, _500_000,
                        panarea(1), jar(), new VoidMethodSignature(SUPPLYCHAIN, "callAuthority", RESOURCE, WORKER),
                        chain, must, wine_making_centre_obj));
    }

    @Test
    @DisplayName("Add a new Resource (Vine)")
    void addProduct()
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        // Producer adds a new Vine
        // EXCEPTION: SupplyChain is null
        Assertions.assertThrows(TransactionException.class, () -> newProduct(NullValue.INSTANCE, producer_obj,
                producer,
                producer_prv_key, "Vine", "", 1, NullValue.INSTANCE));

        // EXCEPTION: Worker isn't part of the SupplyChain
        Assertions.assertThrows(TransactionException.class, () -> newProduct(chain, producer_obj, producer,
                producer_prv_key, "Vine", "", 1, NullValue.INSTANCE));

        // Add Worker to the SupplyChain
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(), ADD_STAFF,
                chain, producer_obj, NullValue.INSTANCE);

        // EXCEPTION: Caller doesn't match with the producer instance
        Assertions.assertThrows(TransactionException.class, () -> newProduct(chain, producer_obj, owner,
                owner_prv_key, "Vine", "", 1, NullValue.INSTANCE));

        // EXCEPTION: The amount must be grater than 0
        Assertions.assertThrows(TransactionException.class, () -> newProduct(chain, producer_obj, producer,
                producer_prv_key, "Vine", "", 0, NullValue.INSTANCE));

        // EXCEPTION: Producer isn't available
        addInstanceMethodCallTransaction(producer_prv_key, producer, _500_000, panarea(1), jar(),
                new VoidMethodSignature(WORKER, "setMaxProducts", BasicTypes.INT), producer_obj, new IntValue(0));
        Assertions.assertThrows(TransactionException.class, () -> newProduct(chain, producer_obj, producer,
                producer_prv_key, "Vine", "", 1, NullValue.INSTANCE));

        // SUCCESS
        addInstanceMethodCallTransaction(producer_prv_key, producer, _500_000, panarea(1), jar(),
                new VoidMethodSignature(WORKER, "setMaxProducts", BasicTypes.INT), producer_obj, new IntValue(5));
        newProduct(chain, producer_obj, producer, producer_prv_key, "Vine", "", 1,
                NullValue.INSTANCE);
    }

    @Test
    @DisplayName("Check pending products (Grape case)")
    void checkPendingProducts()
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        // Get first pack of Grape from the producer
        StorageReference grape1 = createInteractionsUntil("WINE_MAKING_CENTRE");
        BooleanValue pending =
                (BooleanValue) addInstanceMethodCallTransaction(wine_making_centre_prv_key, wine_making_centre,
                        _500_000, panarea(1), jar(), new NonVoidMethodSignature(WORKER, "getPending",
                                BasicTypes.BOOLEAN), wine_making_centre_obj);
        Assertions.assertTrue(pending.value);

        // Get another pack of Grape from the producer
        StorageReference grape2 = createInteractionsUntil("WINE_MAKING_CENTRE");
        pending =
                (BooleanValue) addInstanceMethodCallTransaction(wine_making_centre_prv_key, wine_making_centre,
                        _500_000, panarea(1), jar(), new NonVoidMethodSignature(WORKER, "getPending",
                                BasicTypes.BOOLEAN), wine_making_centre_obj);
        Assertions.assertTrue(pending.value);

        // When the wine-making centre adds a new Must, it automatically controls is there are still pending resources
        newProduct(chain, wine_making_centre_obj, wine_making_centre, wine_making_centre_prv_key, "Must", "", 100,
                grape1);
        pending =
                (BooleanValue) addInstanceMethodCallTransaction(wine_making_centre_prv_key, wine_making_centre,
                        _500_000, panarea(1), jar(), new NonVoidMethodSignature(WORKER, "getPending",
                                BasicTypes.BOOLEAN), wine_making_centre_obj);
        Assertions.assertTrue(pending.value);

        // Creating another Must means there aren't more pending resources
        newProduct(chain, wine_making_centre_obj, wine_making_centre, wine_making_centre_prv_key, "Must", "", 100,
                grape2);
        pending =
                (BooleanValue) addInstanceMethodCallTransaction(wine_making_centre_prv_key, wine_making_centre,
                        _500_000, panarea(1), jar(), new NonVoidMethodSignature(WORKER, "getPending",
                                BasicTypes.BOOLEAN), wine_making_centre_obj);
        Assertions.assertFalse(pending.value);

        // Similarly, for Wine and Bottle cases
    }

    @Test
    @DisplayName("Add analysis results to a Resource")
    void addAnalysisResults()
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        // Create a Vine
        StorageReference vine = createInteractionsUntil("PRODUCER");

        // Add new analysis link on it
        // EXCEPTION: Only current Worker or Authority can add the link
        Assertions.assertThrows(TransactionException.class,
                () -> addInstanceMethodCallTransaction(administrator_prv_key, administrator, _500_000, panarea(1),
                        jar(), new VoidMethodSignature(VINE, "addAnalysisResults", ClassType.STRING, STAFF), vine,
                        new StringValue("www.analysis.com"), administrator_obj));

        // SUCCESS
        addInstanceMethodCallTransaction(producer_prv_key, producer,
                _500_000, panarea(1), jar(), new VoidMethodSignature(VINE, "addAnalysisResults",
                        ClassType.STRING, STAFF), vine, new StringValue("www.analysis.com"), producer_obj);
    }

    @Test
    @DisplayName("Set attributes to Vine")
    void setAttributesVine()
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        // Create Vine
        StorageReference vine = createInteractionsUntil("PRODUCER");

        // Add fertilizers and pesticides
        addInstanceMethodCallTransaction(producer_prv_key, producer, _500_000, panarea(1), jar(),
                new VoidMethodSignature(VINE, "addFertilizer", ClassType.STRING, WORKER), vine, new StringValue(
                        "Zeolite"), producer_obj);
        addInstanceMethodCallTransaction(producer_prv_key, producer, _500_000, panarea(1), jar(),
                new VoidMethodSignature(VINE, "addPesticide", ClassType.STRING, WORKER), vine,
                new StringValue("Generic"), producer_obj);

        // Set harvest date
        addInstanceMethodCallTransaction(producer_prv_key, producer, _500_000, panarea(1), jar(),
                new VoidMethodSignature(VINE, "setHarvestDate", BasicTypes.INT, BasicTypes.INT, BasicTypes.INT, WORKER),
                vine, new IntValue(21), new IntValue(3), new IntValue(2022), producer_obj);
    }

    @Test
    @DisplayName("Set unit of measure to Wine")
    void setAttributesWine()
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        // Create Wine
        StorageReference grape = createInteractionsUntil("WINE_MAKING_CENTRE");
        StorageReference must = newProduct(chain, wine_making_centre_obj, wine_making_centre,
                wine_making_centre_prv_key, "Must", "", 100, grape);
        StorageReference wine = newProduct(chain, wine_making_centre_obj, wine_making_centre,
                wine_making_centre_prv_key, "Wine", "", 10, must);

        // Set unit of measure
        addInstanceMethodCallTransaction(wine_making_centre_prv_key, wine_making_centre, _500_000, panarea(1), jar(),
                new VoidMethodSignature(WINE, "setUnitOfMeasure", ClassType.STRING, WORKER), wine, new StringValue(
                        "oz"), wine_making_centre_obj);
    }

    @Test
    @DisplayName("Set attributes to Bottle")
    void setAttributesBottle()
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        // Create Bottle
        StorageReference wine = createInteractionsUntil("BOTTLING_CENTRE");
        StorageReference bottle =
                newProduct(chain, bottling_centre_obj, bottling_centre, bottling_centre_prv_key, "Bottle", "", 10,
                        wine);

        // Set attributes
        addInstanceMethodCallTransaction(bottling_centre_prv_key, bottling_centre, _500_000, panarea(1), jar(),
                new VoidMethodSignature(BOTTLE, "setAttributes", BasicTypes.INT, ClassType.STRING, ClassType.STRING,
                        WORKER), bottle, new IntValue(5), new StringValue("Glass 1l"), new StringValue("Amarone"),
                bottling_centre_obj);
    }

    @Test
    @DisplayName("Sell Bottles")
    void sellBottle()
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        // Create Bottle and pass it to the retailer
        StorageReference bottle = createInteractionsUntil("RETAILER");

        // Sell Bottle
        // EXCEPTION: Amount is greater than the number of Bottles available
        Assertions.assertThrows(TransactionException.class, () -> addInstanceMethodCallTransaction(retailer_prv_key,
                retailer, _500_000, panarea(1), jar(), new VoidMethodSignature(BOTTLE, "sell", BasicTypes.INT, WORKER),
                bottle, new IntValue(11), retailer_obj));

        // SUCCESS
        addInstanceMethodCallTransaction(retailer_prv_key, retailer, _500_000, panarea(1), jar(),
                new VoidMethodSignature(BOTTLE, "sell", BasicTypes.INT, WORKER), bottle, new IntValue(3),
                retailer_obj);

        // If all Bottles are sold, check that the retailer doesn't possess the product anymore
        StorageReference products =
                (StorageReference) addInstanceMethodCallTransaction(retailer_prv_key, retailer, _500_000, panarea(1),
                        jar(), new NonVoidMethodSignature(WORKER, "getProducts", ClassType.STORAGE_LIST),
                        retailer_obj);
        IntValue products_size =
                (IntValue) addInstanceMethodCallTransaction(retailer_prv_key, retailer, _500_000, panarea(1), jar(),
                        new NonVoidMethodSignature(ClassType.STORAGE_LIST, "size", BasicTypes.INT), products);
        Assertions.assertEquals(1, products_size.value);

        addInstanceMethodCallTransaction(retailer_prv_key, retailer, _500_000, panarea(1), jar(),
                new VoidMethodSignature(BOTTLE, "sell", BasicTypes.INT, WORKER), bottle, new IntValue(7),
                retailer_obj);

        products =
                (StorageReference) addInstanceMethodCallTransaction(retailer_prv_key, retailer, _500_000, panarea(1),
                        jar(), new NonVoidMethodSignature(WORKER, "getProducts", ClassType.STORAGE_LIST),
                        retailer_obj);
        products_size =
                (IntValue) addInstanceMethodCallTransaction(retailer_prv_key, retailer, _500_000, panarea(1), jar(),
                        new NonVoidMethodSignature(ClassType.STORAGE_LIST, "size", BasicTypes.INT), products);
        Assertions.assertEquals(0, products_size.value);
    }

    @Test
    @DisplayName("Certify a Bottle")
    void certifyBottle()
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        // Create Bottle
        StorageReference bottle = createInteractionsUntil("DISTRIBUTION_CENTRE");

        // Certify as DOC
        addInstanceMethodCallTransaction(authority_prv_key, authority, _500_000, panarea(1), jar(),
                new VoidMethodSignature(BOTTLE, "certify", CERTIFICATION, AUTHORITY), bottle, new EnumValue(
                        CERTIFICATION.name, "DOC"), authority_obj);
    }
}