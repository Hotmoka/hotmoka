package io.takamaka.code.tokens;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.*;
import io.takamaka.code.math.UnsignedBigInteger;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageMapView;
import io.takamaka.code.util.StorageTreeMap;

/**
 * Implementation inspired by OpenZeppelin's <a href="https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/token/ERC20/ERC20.sol">ERC20.sol</a>
 *
 * OpenZeppelin: Implementation of the {@link IERC20} interface.
 *
 *  This implementation is agnostic to the way tokens are created. This means that a supply mechanism has to be added in
 *  a derived contract using {@link #_mint(Contract, UnsignedBigInteger)}.
 *
 *  TIP: For a detailed writeup see our guide <a href="https://forum.zeppelin.solutions/t/how-to-implement-erc20-supply-mechanisms/226">[How to implement supply mechanisms]</a>.
 *
 *  We have followed general OpenZeppelin guidelines: functions revert instead of returning false on failure. This
 *  behavior is nonetheless conventional and does not conflict with the expectations of ERC20 applications.
 *
 *  Additionally, an {@link IERC20.Approval} event is emitted on calls to {@link #transferFrom(Contract, Contract, UnsignedBigInteger)}.
 *  This allows applications to reconstruct the allowance for all accounts just by listening to said events.
 *  Other implementations of the EIP may not emit these events, as it isn't required by the specification.
 *
 *  Finally, the non-standard {@link #decreaseAllowance(Contract, UnsignedBigInteger)} and {@link #increaseAllowance(Contract, UnsignedBigInteger)}
 *  functions have been added to mitigate the well-known issues around setting allowances.
 *  See {@link IERC20#approve(Contract, UnsignedBigInteger)}.
 */
public class ERC20 extends Contract implements IERC20 {
    public final UnsignedBigInteger ZERO = new UnsignedBigInteger("0");

    private final StorageMap<Contract, UnsignedBigInteger> _balances = new StorageTreeMap<>();
    private final StorageMap<Contract, StorageMap<Contract, UnsignedBigInteger>> _allowances = new StorageTreeMap<>();
    private UnsignedBigInteger _totalSupply = ZERO;

    private final String _name;
    private final String _symbol;
    private short _decimals;

    /**
     * OpenZeppelin: Sets the values for {@code name} and {@code symbol}, initializes {@code _decimals} with a default
     *  value of 18. To select a different value for {@code _decimals}, use {@link #_setupDecimals(short)}.
     *  All three of these values are immutable: they can only be set once during construction.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     */
    public ERC20(String name, String symbol) {
        _name = name;
        _symbol = symbol;
        _decimals = 18;
    }

    /**
     * OpenZeppelin: Returns the name of the token.
     *
     * @return the name of the token
     */
    public final @View String name() {
        return _name;
    }

    /**
     * OpenZeppelin: Returns the symbol of the token, usually a shorter version of the name.
     *
     * @return the symbol of the token
     */
    public final @View String symbol() {
        return _symbol;
    }

    /**
     * OpenZeppelin: Returns the number of decimals used to get its user representation.
     *  For example, if {@code _decimals} equals `2`, a balance of `505` tokens should be displayed to a user as `5,05`
     *  (`505 / 10 ** 2`).
     *
     *  Tokens usually opt for a value of 18, imitating the relationship between Ether and Wei. This is the value
     *  {@link ERC20} uses, unless {@link #_setupDecimals(short)} is called.
     *
     *  NOTE: This information is only used for _display_ purposes: it in no way affects any of the arithmetic of the
     *  contract, including {@link IERC20#balanceOf(Contract)} and {@link IERC20#transfer(Contract, UnsignedBigInteger)}.
     *
     * @return the number of decimals used to get its user representation
     */
    public final @View short decimals() {
        return _decimals;
    }

    /**
     * OpenZeppelin: See {@link IERC20View#totalSupply()}.
     *
     * @return the amount of tokens in existence
     */
    @Override
    public final @View UnsignedBigInteger totalSupply() {
        return _totalSupply;
    }

    /**
     * OpenZeppelin: See {@link IERC20View#balanceOf(Contract)}.
     *
     * @param account account whose balance you want to check
     * @return the amount of tokens owned by {@code account}
     */
    @Override
    public final @View UnsignedBigInteger balanceOf(Contract account) {
        return _balances.getOrDefault(account, ZERO);
    }

    /**
     * OpenZeppelin: See {@link IERC20#transfer(Contract, UnsignedBigInteger)}.
     *
     *  Requirements:
     *  - the caller must have a balance of at least {@code amount}.
     *
     * @param recipient recipient of the transfer (it cannot be the null account)
     * @param amount number of tokens to transfer (it cannot be null)
     * @return true if the operation is successful
     */
    @Override
    public @FromContract boolean transfer(Contract recipient, UnsignedBigInteger amount) {
        _transfer(caller(), recipient, amount);
        return true;
    }

    /**
     * OpenZeppelin: See {@link IERC20#allowance(Contract, Contract)}.
     *
     * @param owner account that allows {@code spender} to spend its tokens
     * @param spender account authorized to spend on behalf of {@code owner}
     * @return the remaining number of tokens that {@code spender} will be allowed to spend on behalf of {@code owner}
     */
    @Override
    public @View UnsignedBigInteger allowance(Contract owner, Contract spender) {
        return _allowances.getOrDefault(owner, StorageTreeMap::new).getOrDefault(spender, ZERO);
    }

    /**
     * OpenZeppelin: See {@link IERC20#approve(Contract, UnsignedBigInteger)}.
     *
     * @param spender account authorized to spend on behalf of caller (it cannot be the null account)
     * @param amount amount of tokens that {@code spender} can spend on behalf of the caller (it cannot be null)
     * @return true if the operation is successful
     */
    @Override
    public @FromContract boolean approve(Contract spender, UnsignedBigInteger amount) {
        _approve(caller(), spender, amount);
        return true;
    }

    /**
     * OpenZeppelin: See {@link IERC20#transferFrom(Contract, Contract, UnsignedBigInteger)}.
     *  Emits an {@link IERC20.Approval} event indicating the updated allowance. This is not required by the EIP.
     *  See the note at the beginning of {@link ERC20};
     *
     *  Requirements:
     *  - the caller must have allowance for {@code sender}'s tokens of at least {@code amount}.
     *
     * @param sender origin of the transfer (it cannot be the null account, it must have a balance of at least
     *               {@code amount})
     * @param recipient recipient of the transfer (it cannot be the null account)
     * @param amount number of tokens to transfer (it cannot be null)
     * @return true if the operation is successful
     */
    @Override
    public @FromContract boolean transferFrom(Contract sender, Contract recipient, UnsignedBigInteger amount) {
        _transferFrom(caller(), sender, recipient, amount);
        return true;
    }

    /**
     * Internal implementation of the {@link #_transferFrom(Contract, Contract, Contract, UnsignedBigInteger)} method.
     */
	protected final void _transferFrom(Contract caller, Contract sender, Contract recipient, UnsignedBigInteger amount) {
		_transfer(sender, recipient, amount);
        _approve(sender, caller, _allowances.getOrDefault(sender, StorageTreeMap::new)
                .getOrDefault(caller, ZERO)
                .subtract(amount, "Transfer Rejected: transfer amount exceeds allowance"));
	}

    
    /**
     * Creates a new snapshot and returns it.
     * See {@link IERC20View#snapshot()}.
     *
     * @return the snapshot created
     */
    @Override
	public @FromContract IERC20View snapshot() {

        @Exported
    	class SnapshotImpl extends Storage implements IERC20View {
    		private final UnsignedBigInteger _totalSupply = ERC20.this._totalSupply;
    		private final StorageMapView<Contract, UnsignedBigInteger> _balances = ERC20.this._balances.snapshot(); 

    		@Override
			public @View UnsignedBigInteger totalSupply() {
				return _totalSupply;
			}

			@Override
			public @View UnsignedBigInteger balanceOf(Contract account) {
                return _balances.getOrDefault(account, ZERO);
			}

			@Override
			public @FromContract IERC20View snapshot() {
				return this;
			}
    	}

    	return new SnapshotImpl();
	}

	/**
     * OpenZeppelin: Atomically increases the allowance granted to {@code spender} by the caller. This is an alternative
     *  to {@link ERC20#approve(Contract, UnsignedBigInteger)} that can be used as a mitigation for problems described
     *  in {@link IERC20#approve(Contract, UnsignedBigInteger)}.
     *  Emits an {@link IERC20.Approval} event indicating the updated allowance.
     *
     * @param spender account authorized to spend on behalf of {@code owner}, it allowance will be increased (it cannot
     *                be the null account)
     * @param addedValue number of tokens to add from those {@code spender} can spend
     * @return true if the operation is successful
     */
    public @FromContract boolean increaseAllowance(Contract spender, UnsignedBigInteger addedValue) {
        _approve(caller(), spender, _allowances.getOrDefault(caller(), StorageTreeMap::new)
                .getOrDefault(spender, ZERO).add(addedValue));
        return true;
    }

    /**
     * OpenZeppelin: Atomically decreases the allowance granted to {@code spender} by the caller. This is an alternative
     *  to {@link ERC20#approve(Contract, UnsignedBigInteger)} that can be used as a mitigation for problems described
     *  in {@link IERC20#approve(Contract, UnsignedBigInteger)}.
     *  Emits an {@link IERC20.Approval} event indicating the updated allowance.
     *
     * @param spender account authorized to spend on behalf of {@code owner}, it allowance will be decreased (it cannot
     *                be the null account, it must have allowance for the caller of at least {@code subtractedValue})
     * @param subtractedValue number of tokens to remove from those {@code spender} can spend
     * @return true if the operation is successful
     */
    public @FromContract boolean decreaseAllowance(Contract spender, UnsignedBigInteger subtractedValue) {
        _approve(caller(), spender, _allowances.getOrDefault(caller(), StorageTreeMap::new)
                .getOrDefault(spender, ZERO)
                .subtract(subtractedValue, "Approve rejected: decreased allowance below zero"));
        return true;
    }

    /**
     * OpenZeppelin: Moves tokens {@code amount} from {@code sender} to {@code recipient}. This is internal function is
     *  equivalent to {@link #transfer(Contract, UnsignedBigInteger)}, and can be used to. E.g. implement automatic
     *  token fees, slashing mechanisms, etc.
     *  Emits a {@link IERC20.Transfer} event.
     *
     * @param sender origin of the transfer (it cannot be the null account, it must have a balance of at least
     *               {@code amount})
     * @param recipient recipient of the transfer (it cannot be the null account)
     * @param amount number of tokens to transfer (it cannot be null)
     */
    protected void _transfer(Contract sender, Contract recipient, UnsignedBigInteger amount) {
        require(sender != null, "Transfer rejected: transfer from the null account");
        require(recipient != null, "Transfer rejected: transfer to the null account");
        require(amount != null, "Transfer rejected: amount cannot be null");

        _beforeTokenTransfer(sender, recipient, amount);

        _balances.put(sender, _balances.getOrDefault(sender, ZERO)
                .subtract(amount, "Transfer rejected: transfer amount exceeds balance"));
        _balances.put(recipient, _balances.getOrDefault(recipient, ZERO).add(amount));

        event(new Transfer(sender, recipient, amount));
    }

    /**
     * OpenZeppelin: Creates {@code amount} tokens and assigns them to {@code account}, increasing the total supply.
     *  Emits a {@link IERC20.Transfer} event with {@code from} set to the null account.
     *
     * @param account recipient of the created tokens (it cannot be the null account)
     * @param amount number of tokens to create (it cannot be null)
     */
    protected void _mint(Contract account, UnsignedBigInteger amount) {
        require(account != null, "Mint rejected: mint to the null account");
        require(amount != null, "Mint rejected: amount cannot be null");

        _beforeTokenTransfer(null, account, amount);

        _totalSupply = _totalSupply.add(amount);
        _balances.put(account, _balances.getOrDefault(account, ZERO).add(amount));

        event(new Transfer(null, account, amount));
    }

    /**
     * OpenZeppelin: Destroys {@code amount} tokens from {@code account}, reducing the total supply.
     * Emits a {@link IERC20.Transfer} event with {@code to} set to the null account.
     *
     * @param account source of tokens to burn (it cannot be the null account and must have at least {@code amount}
     *                tokens)
     * @param amount number of tokens to burn (it cannot be null)
     */
    protected void _burn(Contract account, UnsignedBigInteger amount) {
        require(account != null, "Burn rejected: burn to the null account");
        require(amount != null, "Burn rejected: amount cannot be null");

        _beforeTokenTransfer(account, null, amount);

        _balances.put(account, _balances.getOrDefault(account, ZERO)
                .subtract(amount, "Burn rejected: burn amount exceeds balance"));
        _totalSupply = _totalSupply.subtract(amount);

        event(new Transfer(account, null, amount));
    }

    /**
     * OpenZeppelin: Sets {@code amount} as the allowance of {@code spender} over the {@code owner}s tokens.
     *  This is internal function is equivalent to {@link #approve(Contract, UnsignedBigInteger)}, and can be used to.
     *  E.g. set automatic allowances for certain subsystems, etc.
     *  Emits an {@link IERC20.Approval} event.
     *
     * @param owner account that authorizes to spend (it cannot be the null account)
     * @param spender account authorized to spend on behalf of {@code owner} (it cannot be the null account)
     * @param amount amount of tokens that {@code spender} can spend on behalf of {@code owner} (it cannot be null)
     */
    protected void _approve(Contract owner, Contract spender, UnsignedBigInteger amount) {
        require(owner != null, "Approve rejected: approve from the null account");
        require(spender != null, "Approve rejected: approve to the null account");
        require(amount != null, "Approve rejected: amount cannot be null");

        StorageMap<Contract, UnsignedBigInteger> ownerAllowances = _allowances.getOrDefault(owner, StorageTreeMap::new);
        ownerAllowances.put(spender, amount);
        _allowances.put(owner, ownerAllowances);

        event(new Approval(owner, spender, amount));
    }

    /**
     * OpenZeppelin: Sets {@code _decimals} to a value other than the default one of 18. WARNING: This function should
     *  only be called from the constructor. Most applications that interact with token contracts will not expect
     *  {@code _decimals} to ever change, and may work incorrectly if it does.
     *
     * @param decimals_ number of decimals used to get token user representation
     */
    protected final void _setupDecimals(short decimals_) {
        _decimals = decimals_;
    }

    /**
     * OpenZeppelin: Hook that is called before any transfer of tokens. This includes minting and burning.
     *
     * Calling conditions:
     * - when {@code from} and {@code to} are both non-null, {@code amount} of {@code from}'s tokens will be to
     *   transferred to {@code to}.
     * - when {@code from} is null, {@code amount} tokens will be minted for {@code to}.
     * - when {@code to} is null, {@code amount} of {@code from}'s tokens will be burned.
     * - {@code from} and {@code to} are never both null.
     *
     * @param from token transfer source account
     * @param to token transfer recipient account
     * @param amount amount of tokens transferred
     */
    protected void _beforeTokenTransfer(Contract from, Contract to, UnsignedBigInteger amount) { }
}