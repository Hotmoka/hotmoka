package io.takamaka.code.tokens;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.UnsignedBigInteger;

/**
 * The read operations on the balance and total supply of an ERC20 token.
 */
public interface IERC20View {

	/**
     * Yields the amount of tokens in existence.
     *
     * @return the amount of tokens in existence
     */
	@View UnsignedBigInteger totalSupply();

    /**
     * Yields the amount of tokens owned by {@code account}.
     *
     * @param account account whose balance you want to check
     * @return the amount of tokens owned by {@code account}
     */
	@View UnsignedBigInteger balanceOf(Contract account);

	/**
	 * Yields a snapshot of this ERC20 token. The snapshot is an immutable
	 * view of the current total supply and balances.
	 * 
	 * @return the snapshot
	 */
	IERC20View snapshot();
}