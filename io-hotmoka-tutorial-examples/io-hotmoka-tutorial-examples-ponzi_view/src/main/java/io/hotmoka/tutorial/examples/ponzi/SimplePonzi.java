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
import io.takamaka.code.lang.View;
import io.takamaka.code.math.BigIntegerSupport;

/**
 * A simple Ponzi scheme. Once an investor arrives, the previous investor gets refunded.
 */
public class SimplePonzi extends Contract {
  private final BigInteger _10 = BigInteger.valueOf(10L);
  private final BigInteger _11 = BigInteger.valueOf(11L);
  private PayableContract currentInvestor;
  private BigInteger currentInvestment = BigInteger.ZERO;

  /**
   * Creates the contract.
   */
  public SimplePonzi() {}

  /**
   * Allows the caller to take part in the game.
   * 
   * @param amount the coins paid by the caller to take part in the game; it must
   *               be at least 10% more than what has been paid by the previous investor
   */
  public @Payable @FromContract(PayableContract.class) void invest(BigInteger amount) {
    // new investments must be at least 10% greater than current
    BigInteger minimumInvestment = BigIntegerSupport.divide
      (BigIntegerSupport.multiply(currentInvestment, _11), _10);
    require(BigIntegerSupport.compareTo(amount, minimumInvestment) > 0,
      () -> StringSupport.concat("you must invest more than ", minimumInvestment));

    // document new investor
    if (currentInvestor != null)
      currentInvestor.receive(amount);

    currentInvestor = (PayableContract) caller();
    currentInvestment = amount;
  }

  /**
   * Yields the current investment. The next investor must pay at least
   * 10% more than this to take part in the game.
   * 
   * @return the current investment
   */
  public @View BigInteger getCurrentInvestment() {
    return currentInvestment;
  }
}