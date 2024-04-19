/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.tests;

import static io.hotmoka.helpers.Coin.filicudi;
import static io.hotmoka.helpers.Coin.panarea;
import static io.hotmoka.helpers.Coin.stromboli;
import static io.hotmoka.node.StorageTypes.LONG;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test showing that it is possible to have a shared entity with unrelated
 * shareholders' types, since generic types are erased at compilation time.
 */
class SharedEntity2AllowsArbitraryShareholdersTypes extends HotmokaTest {
    private static final ClassType MY_CLASS = StorageTypes.classNamed("io.hotmoka.examples.sharedentities.MyClass");
    private static final ClassType SHARED_ENTITY_2 = StorageTypes.classNamed("io.hotmoka.examples.sharedentities.SharedEntity2");
    private static final ClassType SIMPLE_SHARED_ENTITY_2 = StorageTypes.classNamed("io.hotmoka.examples.sharedentities.SimpleSharedEntity2");
    private static final ClassType OFFER_2 = StorageTypes.classNamed(SHARED_ENTITY_2 + "$Offer");
    private static final ConstructorSignature MY_CLASS_CONSTRUCTOR = ConstructorSignatures.of(MY_CLASS);
    private static final ConstructorSignature SIMPLE_SHARED_ENTITY_2_CONSTRUCTOR = ConstructorSignatures.of(SIMPLE_SHARED_ENTITY_2, StorageTypes.PAYABLE_CONTRACT, StorageTypes.BIG_INTEGER);
    private static final BigInteger _200_000 = BigInteger.valueOf(200_000);
    private StorageReference creator;
    private StorageReference seller;
    private StorageReference buyer;
    private TransactionReference classpath;

    @BeforeAll
	static void beforeAll() throws Exception {
		setJar("sharedentities.jar");
	}

    @BeforeEach
    void beforeEach() throws Exception {
        setAccounts(stromboli(1), filicudi(100), filicudi(100), filicudi(100));
        creator = account(0);
        seller = account(1);
        buyer = account(2);
        classpath = jar();
    }

    @Test
    @DisplayName("acceptance with different shareholder classes works")
    void acceptanceWithDifferentShareholderClassesWorks() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
        // create the MyClass contract from the seller
        StorageReference sellerContractMyClass = addConstructorCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath, MY_CLASS_CONSTRUCTOR);

        // create a shared entity contract (v2)
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath,
                SIMPLE_SHARED_ENTITY_2_CONSTRUCTOR, sellerContractMyClass, StorageValues.bigIntegerOf(10));

        // create an offer by the seller using his contract
        var offer = (StorageReference) addInstanceNonVoidMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath,
                MethodSignatures.of(MY_CLASS, "createOffer2", OFFER_2, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, LONG),
                sellerContractMyClass, StorageValues.bigIntegerOf(2), StorageValues.bigIntegerOf(2), StorageValues.longOf(1893456000));

        // the seller places his offer using his contract
        addInstanceVoidMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath,
        		MethodSignatures.ofVoid(MY_CLASS, "placeOffer", SHARED_ENTITY_2, StorageTypes.BIG_INTEGER, OFFER_2),
                sellerContractMyClass, sharedEntity, StorageValues.bigIntegerOf(0), offer);

        // the buyer is an account (EOA) and he accepts the offer: this should not be valid but the test shows that it actually works
        addInstanceVoidMethodCallTransaction(privateKey(2), buyer, _200_000, panarea(1), classpath,
        		MethodSignatures.ofVoid(SIMPLE_SHARED_ENTITY_2, "accept", StorageTypes.BIG_INTEGER, OFFER_2),
                sharedEntity, StorageValues.bigIntegerOf(2), offer);
    }
}