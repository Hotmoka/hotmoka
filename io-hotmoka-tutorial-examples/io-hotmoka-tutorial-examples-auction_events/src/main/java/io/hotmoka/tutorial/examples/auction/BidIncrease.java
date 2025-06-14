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

package io.hotmoka.tutorial.examples.auction;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;

/**
 * An event stating that the current highest bid has been increased.
 */
public class BidIncrease extends Event {

  /**
   * The current participant that provided the highest bid.
   */
  public final PayableContract bidder;

  /**
   * The value of the current highest bid.
   */
  public final BigInteger amount;

  /**
   * Creates the event.
   * 
   * @param bidder the current participant that provided the highest bid
   * @param amount the value of the current highest bid
   */
  @FromContract BidIncrease(PayableContract bidder, BigInteger amount) {
    this.bidder = bidder;
    this.amount = amount;
  }

  /**
   * Yields the current participant that provided the highest bid.
   * 
   * @return the current participant that provided the highest bid
   */
  public @View PayableContract getBidder() {
    return bidder;
  }

  /**
   * Yields the value of the current highest bid.
   * 
   * @return the value of the current highest bid
   */
  public @View BigInteger getAmount() {
    return amount;
  }
}