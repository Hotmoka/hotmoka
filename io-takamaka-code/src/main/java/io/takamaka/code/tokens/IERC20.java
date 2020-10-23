package io.takamaka.code.tokens;

import io.takamaka.code.util.UnsignedBigInteger;
import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.View;

/**
 * Implementation inspired by OpenZeppelin:
 * https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/token/ERC20/IERC20.sol
 *
 * OpenZeppelin: Interface of the ERC20 standard as defined in the EIP.
 */
public interface IERC20 {
    /**
     * OpenZeppelin: Returns the amount of tokens in existence.
     *
     * @return the amount of tokens in existence
     */
    public @View UnsignedBigInteger totalSupply();

    /**
     * OpenZeppelin: Returns the amount of tokens owned by `account`.
     *
     * @param account account whose balance you want to check
     * @return he amount of tokens owned by `account`
     */
    public @View UnsignedBigInteger balanceOf(Contract account);

    /**
     * OpenZeppelin: Moves `amount` tokens from the caller's account to `recipient`.
     *  Returns a boolean value indicating whether the operation succeeded.
     *  Emits a {Transfer} event.
     *
     * @param recipient recipient of the transfer (it cannot be the null account)
     * @param amount number of tokens to transfer (it cannot be null)
     * @return true if the operation is successful
     */
    public @Entry boolean transfer(Contract recipient, UnsignedBigInteger amount);

    /**
     * OpenZeppelin: Returns the remaining number of tokens that `spender` will be allowed to spend on behalf of
     *  `owner` through {transferFrom}. This is zero by default.
     *  This value changes when {approve} or {transferFrom} are called.
     *
     * @param owner account that allows `spender` to spend its tokens
     * @param spender account authorized to spend on behalf of `owner`
     * @return the remaining number of tokens that `spender` will be allowed to spend on behalf of `owner`
     */
    public @View UnsignedBigInteger allowance(Contract owner, Contract spender);

    /**
     * OpenZeppelin: Sets `amount` as the allowance of `spender` over the caller's tokens.
     *  Returns a boolean value indicating whether the operation succeeded.
     *
     *  IMPORTANT: Beware that changing an allowance with this method brings the risk that someone may use both the old
     *  and the new allowance by unfortunate transaction ordering. One possible solution to mitigate this race condition
     *  is to first reduce the spender's allowance to 0 and set the desired value afterwards:
     *  https://github.com/ethereum/EIPs/issues/20#issuecomment-263524729
     *
     *  Emits an {Approval} event.
     *
     * @param spender account authorized to spend on behalf of caller (it cannot be the null account)
     * @param amount amount of tokens that `spender` can spend on behalf of the caller (it cannot be null)
     * @return true if the operation is successful
     */
    public @Entry boolean approve(Contract spender, UnsignedBigInteger amount);

    /**
     * OpenZeppelin: Moves `amount` tokens from `sender` to `recipient` using the allowance mechanism. `amount` is then
     * deducted from the caller's allowance.
     *
     * Returns a boolean value indicating whether the operation succeeded.
     *
     * Emits a {Transfer} event.
     *
     * @param sender origin of the transfer (it cannot be the null account, it must have a balance of at least `amount`)
     * @param recipient recipient of the transfer (it cannot be the null account)
     * @param amount number of tokens to transfer (it cannot be null)
     * @return true if the operation is successful
     */
    public @Entry boolean transferFrom(Contract sender, Contract recipient, UnsignedBigInteger amount);

    /**
     * OpenZeppelin: Emitted when `value` tokens are moved from one account (`from`) to another (`to`).
     *  Note that `value` may be zero.
     */
    public static class Transfer extends Event {
        public final Contract from;
        public final Contract to;
        public final UnsignedBigInteger value;

        /**
         * Allows the Transfer event to be issued.
         *
         * @param key the key of the event
         * @param from origin of the tokens transfer
         * @param to recipient of the tokens transfer
         * @param value number of tokens that have been transferred from `from` to `to`
         */
        Transfer(Contract key, Contract from, Contract to, UnsignedBigInteger value) {
            super(key);

            this.from = from;
            this.to = to;
            this.value = value;
        }
    }

    /**
     * OpenZeppelin: Emitted when the allowance of a `spender` for an `owner` is set by a call to {approve}. `value`
     *  is the new allowance.
     */
    public static class Approval extends Event {
        public final Contract owner;
        public final Contract spender;
        public final UnsignedBigInteger value;

        /**
         * Allows the Approval event to be issued.
         *
         * @param key the key of the event
         * @param owner account that authorizes to spend
         * @param spender account authorized to spend on behalf of `owner`
         * @param value amount of tokens that `spender` can spend on behalf of `owner`
         */
        Approval(Contract key, Contract owner, Contract spender, UnsignedBigInteger value) {
            super(key);

            this.owner = owner;
            this.spender = spender;
            this.value = value;
        }
    }
}