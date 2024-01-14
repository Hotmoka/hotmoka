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

import static io.hotmoka.beans.Coin.filicudi;
import static io.hotmoka.beans.Coin.panarea;
import static io.hotmoka.beans.Coin.stromboli;
import static io.hotmoka.beans.StorageTypes.BIG_INTEGER;
import static io.hotmoka.beans.StorageTypes.LONG;
import static io.hotmoka.beans.StorageTypes.SHARED_ENTITY;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test about subclassing of the shared entity contract with a specific class for the shareholders.
 */
class MyClassSharedEntityWithArbitraryShareholdersTypes extends HotmokaTest {
    private static final ClassType MY_CLASS = StorageTypes.classNamed("io.hotmoka.examples.sharedentities.MyClass");
    private static final ClassType MY_CLASS_SHARED_ENTITY_1 = StorageTypes.classNamed("io.hotmoka.examples.sharedentities.MyClassSharedEntity1");
    private static final ClassType MY_CLASS_SHARED_ENTITY_2 = StorageTypes.classNamed("io.hotmoka.examples.sharedentities.MyClassSharedEntity2");
    private static final ClassType OFFER = StorageTypes.SHARED_ENTITY_OFFER;
    private static final ConstructorSignature MY_CLASS_CONSTRUCTOR = new ConstructorSignature(MY_CLASS);
    private static final ConstructorSignature MY_CLASS_SHARED_ENTITY_1_CONSTRUCTOR = new ConstructorSignature(MY_CLASS_SHARED_ENTITY_1, MY_CLASS, BIG_INTEGER);
    private static final ConstructorSignature MY_CLASS_SHARED_ENTITY_2_CONSTRUCTOR = new ConstructorSignature(MY_CLASS_SHARED_ENTITY_2, MY_CLASS, BIG_INTEGER);
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
    @DisplayName("acceptance with different shareholder classes works in MyClassSharedEntity1")
    void MyClassSharedEntity1DifferentShareholderClassesWorks() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create the MyClass contract from the seller
        StorageReference sellerContractMyClass = addConstructorCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath, MY_CLASS_CONSTRUCTOR);

        // create a shared entity contract (v3)
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath,
                MY_CLASS_SHARED_ENTITY_1_CONSTRUCTOR, sellerContractMyClass, StorageValues.bigIntegerOf(10));

        // create an offer (v3) by the seller using his contract
        StorageReference offer = (StorageReference) addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath,
                new NonVoidMethodSignature(MY_CLASS, "createOffer", OFFER, BIG_INTEGER, BIG_INTEGER, LONG),
                sellerContractMyClass, StorageValues.bigIntegerOf(2), StorageValues.bigIntegerOf(2), StorageValues.longOf(1893456000));

        // the seller places his offer using his contract
        addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath,
                new VoidMethodSignature(MY_CLASS, "placeOffer", SHARED_ENTITY, BIG_INTEGER, OFFER),
                sellerContractMyClass, sharedEntity, StorageValues.bigIntegerOf(0), offer);

        // the buyer is an account (EOA) and he accepts the offer
        // this would not be valid but the test passes
        addInstanceMethodCallTransaction(privateKey(2), buyer, _200_000, panarea(1), classpath,
                new VoidMethodSignature(MY_CLASS_SHARED_ENTITY_1, "accept", BIG_INTEGER, StorageTypes.PAYABLE_CONTRACT, OFFER),
                sharedEntity, StorageValues.bigIntegerOf(2), buyer, offer);
    }


    @Test
    @DisplayName("acceptance with different shareholder classes fails in MyClassSharedEntity2")
    void MyClassSharedEntity2DifferentShareholderClassesFails() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create the MyClass contract from the seller
        StorageReference sellerContractMyClass = addConstructorCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath, MY_CLASS_CONSTRUCTOR);

        // create a shared entity contract (v3)
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath,
                MY_CLASS_SHARED_ENTITY_2_CONSTRUCTOR, sellerContractMyClass, StorageValues.bigIntegerOf(10));

        // create an offer (v3) by the seller using his contract
        StorageReference offer = (StorageReference) addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath,
                new NonVoidMethodSignature(MY_CLASS, "createOffer", OFFER, BIG_INTEGER, BIG_INTEGER, LONG),
                sellerContractMyClass, StorageValues.bigIntegerOf(2), StorageValues.bigIntegerOf(2), StorageValues.longOf(1893456000));

        // the seller places his offer using his contract
        addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath,
                new VoidMethodSignature(MY_CLASS, "placeOffer", SHARED_ENTITY, BIG_INTEGER, OFFER),
                sellerContractMyClass, sharedEntity, StorageValues.bigIntegerOf(0), offer);

        // the buyer is an account (EOA) and he accepts the offer
        // case 1: ClassCastException
        throwsTransactionExceptionWithCause("java.lang.ClassCastException", () ->
                addInstanceMethodCallTransaction(privateKey(2), buyer, _200_000, panarea(1), classpath,
                        new VoidMethodSignature(MY_CLASS_SHARED_ENTITY_2, "accept", BIG_INTEGER, StorageTypes.PAYABLE_CONTRACT, OFFER),
                        sharedEntity, StorageValues.bigIntegerOf(2), buyer, offer)
        );

        // case 2: IllegalArgumentException
        throwsTransactionExceptionWithCause("java.lang.IllegalArgumentException", () ->
                addInstanceMethodCallTransaction(privateKey(2), buyer, _200_000, panarea(1), classpath,
                        new VoidMethodSignature(MY_CLASS_SHARED_ENTITY_2, "accept", BIG_INTEGER, MY_CLASS, OFFER),
                        sharedEntity, StorageValues.bigIntegerOf(2), buyer, offer)
        );
    }
}
