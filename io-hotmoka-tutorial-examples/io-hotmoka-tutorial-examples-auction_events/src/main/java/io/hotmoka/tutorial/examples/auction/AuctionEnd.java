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
 * An event stating that the auction has ended.
 */
public class AuctionEnd extends Event {

  /**
   * The participant that provided the highest bid.
   */
  public final PayableContract highestBidder;

  /**
   * The value of the highest bid.
   */
  public final BigInteger highestBid;

  /**
   * Creates the event.
   * 
   * @param highestBidder the participant that provided the highest bid
   * @param highestBid the value of the highest bid
   */
  @FromContract AuctionEnd(PayableContract highestBidder, BigInteger highestBid) {
    this.highestBidder = highestBidder;
    this.highestBid = highestBid;
  }

  /**
   * Yields the participant that provided the highest bid.
   * 
   * @return the participant that provided the highest bid
   */
  public @View PayableContract getHighestBidder() {
    return highestBidder;
  }

  /**
   * Yields the value of the highest bid.
   * 
   * @return the value of the highest bid
   */
  public @View BigInteger getHighestBid() {
    return highestBid;
  }
}