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

package io.hotmoka.tutorial.examples.ponzi;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.math.BigIntegerSupport;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;

/**
 * A gradual Ponzi contract. Once a payment arrives, the previous payment gets
 * distributed to the participants.
 */
public class GradualPonzi extends Contract {

  /**
   * The minimum bet that must be paid to take part in the game.
   */
  public final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(1_000L);

  /**
   * All investors up to now. This list might contain the same investor many times,
   * which is important to pay him back more than investors who only invested once.
   */
  private final StorageList<PayableContract> investors = new StorageLinkedList<>();

  /**
   * Creates the contract.
   */
  public @FromContract(PayableContract.class) GradualPonzi() {
    investors.add((PayableContract) caller());
  }

  /**
   * Allows the caller to take part in the game. It must pay an amount
   * that must be at at least {@link #MINIMUM_INVESTMENT}. The previous
   * investment, if any, gets distributed to the current participants.
   * 
   * @param amount the amount paid to take part in the game
   */
  public @Payable @FromContract(PayableContract.class) void invest(BigInteger amount) {
	// new investments must be at least 10% greater than current
    require(BigIntegerSupport.compareTo(amount, MINIMUM_INVESTMENT) >= 0,
      () -> StringSupport.concat("you must invest at least ", MINIMUM_INVESTMENT));
    BigInteger eachInvestorGets = BigIntegerSupport.divide(amount, BigInteger.valueOf(investors.size()));
    investors.forEach(investor -> investor.receive(eachInvestorGets));
    investors.add((PayableContract) caller());
  }
}