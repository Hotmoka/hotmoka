package io.hotmoka.examples.tokens;

import java.math.BigInteger;

import io.takamaka.code.lang.Account;
import io.takamaka.code.lang.Accounts;
import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.tokens.IERC20;

/**
 * The account used in some tests as the creator of an ERC token.
 * It has a special method to simplify the distribution of a given amount of tokens
 * to each account of a given container.
 */
public class ExampleCoinCreator extends ExternallyOwnedAccount {

	/**
	 * Creates the account.
	 * 
	 * @param amount the initial balance of the account
	 * @param publicKey the public key of the account
	 */
	@FromContract @Payable
	public ExampleCoinCreator(BigInteger amount, String publicKey) {
		super(amount, publicKey);
	}

	/**
	 * Distributes some coins to the given accounts, inside the given token.
	 * 
	 * @param accounts the accounts
	 * @param token the token
	 * @param howMuch how much coins to distribute to each account
	 */
	public void distribute(Accounts<?> accounts, IERC20 token, int howMuch) {
		for (Account account: accounts)
			token.transfer((Contract) account, howMuch);
	}
}