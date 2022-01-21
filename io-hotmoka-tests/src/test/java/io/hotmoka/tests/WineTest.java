package io.hotmoka.tests;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.*;
import org.junit.jupiter.api.*;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

import static io.hotmoka.beans.Coin.*;

/**
 * A test for the wine package.
 */
class WineTest extends TakamakaTest {
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
    private static final ClassType WORKER = new ClassType("io.hotmoka.examples.wine.staff.Worker");
    private static final ClassType ROLE = new ClassType("io.hotmoka.examples.wine.staff.Role");
    private static final ConstructorSignature CONSTRUCTOR_WORKER = new ConstructorSignature(WORKER,
            ClassType.EOA, ClassType.STRING, ROLE, new ClassType("java.lang.Integer"));

    private static final ClassType AUTHORITY = new ClassType("io.hotmoka.examples.wine.staff.Authority");
    private static final ConstructorSignature CONSTRUCTOR_AUTHORITY = new ConstructorSignature(AUTHORITY,
            ClassType.EOA, ClassType.STRING);

    private static final ClassType INTEGER = new ClassType("java.lang.Integer");

    /**
     * The classpath of the classes of code module.
     */
    private TransactionReference classpath_takamaka_code;

    /**
     * The owner of the supply chain.
     */
    private StorageReference owner;
    private PrivateKey owner_prv_key;

    /**
     * A producer.
     */
    private StorageReference producer;
    private PrivateKey producer_prv_key;

    /**
     * A wine-making centre.
     */
    private StorageReference wine_making_centre;
    private PrivateKey wine_making_centre_prv_key;

    /**
     * A bottling centre.
     */
    private StorageReference bottling_centre;
    private PrivateKey bottling_centre_prv_key;

    /**
     * A distribution centre.
     */
    private StorageReference distribution_centre;
    private PrivateKey distribution_centre_prv_key;

    /**
     * A retailer.
     */
    private StorageReference retailer;
    private PrivateKey retailer_prv_key;

    /**
     * An authority.
     */
    private StorageReference authority;
    private PrivateKey authority_prv_key;

    /**
     * An administrator.
     */
    private StorageReference administrator;
    private PrivateKey administrator_prv_key;

    @BeforeAll
    static void beforeAll() throws Exception {
        setJar("wine.jar");
    }

    @BeforeEach
    void beforeEach() throws Exception {
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
        classpath_takamaka_code = takamakaCode();
    }

    StorageReference createSupplyChain()
            throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException,
            SignatureException {
        return addConstructorCallTransaction(
                owner_prv_key, // an object that signs with the payer's private key
                owner, // payer of the transaction
                _500_000, // gas provided to the transaction
                panarea(1), // gas price
                jar(), // reference to the jar being tested
                CONSTRUCTOR_SUPPLYCHAIN, // constructor signature
                owner // constructor argument
        );
    }

    StorageReference newWorker(StorageReference chain, StorageReference account, PrivateKey account_prv_key,
                               String name,
                               String role, int max_products)
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        StorageReference worker = addConstructorCallTransaction(account_prv_key, account, _500_000, panarea(1), jar(),
                CONSTRUCTOR_WORKER, account, new StringValue(name), new EnumValue("io.hotmoka.examples.wine.staff.Role",
                        role), new IntValue(max_products));
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(),
                new VoidMethodSignature(SUPPLYCHAIN, "add", STAFF), chain, worker);
        return worker;
    }

    StorageReference newAuthority(StorageReference chain, StorageReference account, PrivateKey account_prv_key,
                                  String name)
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        StorageReference authority = addConstructorCallTransaction(account_prv_key, account, _500_000, panarea(1),
                jar(), CONSTRUCTOR_AUTHORITY, account, new StringValue(name));
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(),
                new VoidMethodSignature(SUPPLYCHAIN, "add", STAFF), chain, authority);
        return authority;
    }

    StorageReference newAdministrator(StorageReference chain, StorageReference account, PrivateKey account_prv_key,
                                      String name)
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        StorageReference administrator = addConstructorCallTransaction(account_prv_key, account, _500_000, panarea(1),
                jar(), CONSTRUCTOR_AUTHORITY, account, new StringValue(name));
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(),
                new VoidMethodSignature(SUPPLYCHAIN, "add", STAFF), chain, administrator);
        return administrator;
    }

    StorageReference newProduct(StorageReference chain, StorageReference worker, StorageReference account,
                                PrivateKey account_prv_key, String name, String description, int amount,
                                StorageValue prevProduct)
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        return (StorageReference) addInstanceMethodCallTransaction(account_prv_key, account, _500_000, panarea(1),
                jar(), new NonVoidMethodSignature(WORKER, "addProduct", RESOURCE, SUPPLYCHAIN, ClassType.STRING,
                        ClassType.STRING, INTEGER, RESOURCE), worker, chain,
                new StringValue(name), new StringValue(description), new IntValue(amount),
                prevProduct);
    }

    StorageReference transferProduct(StorageReference chain, StorageReference worker, StorageReference account,
                                     PrivateKey account_prv_key, StorageValue product)
            throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        return (StorageReference) addInstanceMethodCallTransaction(account_prv_key, account, _500_000, panarea(1),
                jar(), new NonVoidMethodSignature(SUPPLYCHAIN, "transferProduct", WORKER, RESOURCE, WORKER), chain,
                product, worker);
    }

    @Test
    @DisplayName("Basic iterations from Producer to Retailer")
    void test() throws TransactionException, TransactionRejectedException, CodeExecutionException, SignatureException,
            InvalidKeyException {
        // Creation of a SupplyChain and its Staff members
        StorageReference chain = createSupplyChain();
        StorageReference producer_obj = newWorker(chain, producer, producer_prv_key, "Mario", "PRODUCER", 10);
        StorageReference wine_making_centre_obj = newWorker(chain, wine_making_centre, wine_making_centre_prv_key,
                "Luigi", "WINE_MAKING_CENTRE", 50);
        StorageReference bottling_centre_obj = newWorker(chain, bottling_centre, bottling_centre_prv_key, "Cristina",
                "BOTTLING_CENTRE", 50);
        StorageReference distribution_centre_obj = newWorker(chain, distribution_centre, distribution_centre_prv_key,
                "Tommaso", "DISTRIBUTION_CENTRE", 200);
        StorageReference retailer_obj = newWorker(chain, retailer, retailer_prv_key, "Elena", "RETAILER", 20);
        StorageReference authority_obj = newAuthority(chain, authority, authority_prv_key, "Elisabetta");
        StorageReference administrator_obj = newAdministrator(chain, administrator, administrator_prv_key, "Alessia");

        // Producer create a new Vine
        StorageReference vine =
                newProduct(chain, producer_obj, producer, producer_prv_key, "Vine1", "Vine1 of Mario", 1,
                        NullValue.INSTANCE);
        addInstanceMethodCallTransaction(producer_prv_key, producer, _500_000, panarea(1), jar(),
                new VoidMethodSignature(VINE, "addFertilizer", ClassType.STRING), vine, new StringValue("Zeolite"));
        addInstanceMethodCallTransaction(producer_prv_key, producer, _500_000, panarea(1), jar(),
                new VoidMethodSignature(VINE, "addPesticide", ClassType.STRING), vine, new StringValue("Generic"));
        addInstanceMethodCallTransaction(producer_prv_key, producer, _500_000, panarea(1), jar(),
                new VoidMethodSignature(VINE, "setHarvestDate", INTEGER, INTEGER, INTEGER), vine,
                new IntValue(21), new IntValue(3), new IntValue(2022));

        // Producer create new Grape from Vine
        StorageReference grape =
                newProduct(chain, producer_obj, producer, producer_prv_key, "Grape1", "Grape1 from Vine1"
                        , 100, vine);

        // Authority sets Grape's state as eligible, then Resource can be transferred to the wine-making centre
        Assertions.assertThrows(TransactionException.class, () -> transferProduct(chain, producer_obj, producer,
                producer_prv_key, grape));
        addInstanceMethodCallTransaction(authority_prv_key, authority, _500_000, panarea(1), jar(),
                new VoidMethodSignature(GRAPE, "setState", GRAPE_STATE, AUTHORITY), grape,
                new EnumValue("io.hotmoka.examples.wine.resources.GrapeState", "ELIGIBLE"), authority_obj);
        StorageReference next_worker = transferProduct(chain, producer_obj, producer, producer_prv_key,
                grape);
        Assertions.assertEquals(wine_making_centre_obj, next_worker);

        // Wine-making centre create Must and Wine from the Grape obtained
        StorageReference must = newProduct(chain, wine_making_centre_obj, wine_making_centre,
                wine_making_centre_prv_key, "Must1", "Must1 from Grape1", 100, grape);
        StorageReference wine = newProduct(chain, wine_making_centre_obj, wine_making_centre,
                wine_making_centre_prv_key, "Wine1", "Wine1 from Must1", 10, must);
        addInstanceMethodCallTransaction(wine_making_centre_prv_key, wine_making_centre, _500_000, panarea(1), jar(),
                new VoidMethodSignature(WINE, "setUnitOfMeasure", ClassType.STRING), wine, new StringValue("l"));

        // Authority sets Wine's state as eligible, then Resource can be transferred to the bottling centre
        Assertions.assertThrows(TransactionException.class,
                () -> transferProduct(chain, wine_making_centre_obj, wine_making_centre, wine_making_centre_prv_key,
                        wine));
        addInstanceMethodCallTransaction(authority_prv_key, authority, _500_000, panarea(1), jar(),
                new VoidMethodSignature(WINE, "setEligible", AUTHORITY), wine, authority_obj);
        next_worker = transferProduct(chain, wine_making_centre_obj, wine_making_centre, wine_making_centre_prv_key,
                wine);
        Assertions.assertEquals(bottling_centre_obj, next_worker);

        // Bottling centre creates Bottles from Wine
        StorageReference bottle = newProduct(chain, bottling_centre_obj, bottling_centre,
                bottling_centre_prv_key, "Bottle1", "Bottle1 from Wine1", 10, wine);
        addInstanceMethodCallTransaction(bottling_centre_prv_key, bottling_centre, _500_000, panarea(1), jar(),
                new VoidMethodSignature(BOTTLE, "setAttributes", INTEGER,
                        ClassType.STRING, ClassType.STRING), bottle, new IntValue(5), new StringValue("Glass 1l"),
                new StringValue("Amarone"));

        // Authority can certify Bottles if they have requirements
        addInstanceMethodCallTransaction(authority_prv_key, authority, _500_000, panarea(1), jar(),
                new VoidMethodSignature(BOTTLE, "certify", CERTIFICATION, AUTHORITY), bottle,
                new EnumValue("io.hotmoka.examples.wine.resources.Certification", "DOC"), authority_obj);

        // Transfer of Bottles to distribution centre
        next_worker = transferProduct(chain, bottling_centre_obj, bottling_centre, bottling_centre_prv_key, bottle);
        Assertions.assertEquals(distribution_centre_obj, next_worker);

        // Tranfer Bottles to retailers
        next_worker = transferProduct(chain, distribution_centre_obj, distribution_centre, distribution_centre_prv_key,
                bottle);
        Assertions.assertEquals(retailer_obj, next_worker);

        // Retailers sell Bottles
        Assertions.assertThrows(TransactionException.class,
                () -> addInstanceMethodCallTransaction(retailer_prv_key, retailer, _500_000, panarea(1), jar(),
                        new VoidMethodSignature(BOTTLE, "sell", INTEGER,
                                WORKER), bottle, new IntValue(10), retailer_obj));
        addInstanceMethodCallTransaction(retailer_prv_key, retailer, _500_000, panarea(1), jar(),
                new VoidMethodSignature(BOTTLE, "sell", INTEGER,
                        WORKER), bottle, new IntValue(2), retailer_obj);

        // Remove Administrator for dismissal
        addInstanceMethodCallTransaction(owner_prv_key, owner, _500_000, panarea(1), jar(),
                new VoidMethodSignature(SUPPLYCHAIN, "remove", STAFF), chain, administrator_obj);
    }
}