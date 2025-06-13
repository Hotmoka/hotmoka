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

package io.hotmoka.tutorial.examples.erc20;

import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.math.UnsignedBigInteger;
import io.takamaka.code.tokens.ERC20;

/**
 * An ERC20 token example that allows its creator only to mint or burn tokens.
 */
public class CryptoBuddy extends ERC20 {
  private final Contract owner;

  /**
   * Creates the token contract and sets the initial settings of the coin.
   */
  public @FromContract CryptoBuddy() {
    super("CryptoBuddy", "CB");
    owner = caller();
    var initialSupply = new UnsignedBigInteger("200000");
    var multiplier = new UnsignedBigInteger("10").pow(18);
    _mint(caller(), initialSupply.multiply(multiplier)); // 200'000 * 10 ^ 18
  }

  /**
   * Mints tokens.
   *
   * @param account the recipient of the created tokens
   * @param amount the amount of tokens to mint
   */
  public @FromContract void mint(Contract account, UnsignedBigInteger amount) {
    require(caller() == owner, "Lack of permission");
    _mint(account, amount);
  }

  /**
   * Burns tokens.
   *
   * @param account the owner of the tokens to burn
   * @param amount the amount of tokens to burn
   */
  public @FromContract void burn(Contract account, UnsignedBigInteger amount) {
    require(caller() == owner, "Lack of permission");
    _burn(account, amount);
  }
}