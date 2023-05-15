/*
Copyright 2021 Marco Crosara and Fausto Spoto

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

package io.takamaka.code.tokens;

import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.Takamaka;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.UnsignedBigInteger;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageMapView;
import io.takamaka.code.util.StorageTreeMap;

/**
 * Implementation inspired by OpenZeppelin's <a href="https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/token/ERC20/ERC20.sol">ERC20.sol</a>
 *
 * Implementation of the {@link IERC20} interface.
 *
 * This implementation is agnostic to the way tokens are created. This means that a supply mechanism has to be added in
 * a derived contract using {@link #_mint(Contract, UnsignedBigInteger)}.
 *
 * TIP: For a detailed writeup see OepnZeppelin's guide <a href="https://forum.zeppelin.solutions/t/how-to-implement-erc20-supply-mechanisms/226">[How to implement supply mechanisms]</a>.
 *
 * We have followed the general OpenZeppelin guidelines: functions revert instead of returning false on failure. This
 * behavior is nonetheless conventional and does not conflict with the expectations of ERC20 applications.
 *
 * Additionally, an {@link IERC20.Approval} event is emitted on calls to {@link #transferFrom(Contract, Contract, UnsignedBigInteger)}.
 * This allows applications to reconstruct the allowance for all accounts just by listening to said events.
 * Other implementations of the EIP may not emit these events, as it isn't required by the specification.
 *
 * Finally, the non-standard {@link #decreaseAllowance(Contract, UnsignedBigInteger)} and {@link #increaseAllowance(Contract, UnsignedBigInteger)}
 * functions have been added to mitigate the well-known issues around setting allowances.
 * See {@link IERC20#approve(Contract, UnsignedBigInteger)}.
 */
public class ERC20 extends Contract implements IERC20 {
	
	/**
	 * The name of the token.
	 */
    public final String name;

    /**
     * The symbol used for the token.
     */
	public final String symbol;

	/**
	 * The constant 0.
	 */
	public final UnsignedBigInteger ZERO = new UnsignedBigInteger("0");

	private final StorageMap<Contract, UnsignedBigInteger> balances = new StorageTreeMap<>();
    private final StorageMap<Contract, StorageMap<Contract, UnsignedBigInteger>> allowances = new StorageTreeMap<>();
    private UnsignedBigInteger totalSupply = ZERO;
    private short decimals;
    private final boolean generateEvents;

    /**
     * The latest snapshot of this contract. This must be updated
     * after every modification of {@link #balances} and {@link #totalSupply}.
     */
    private IERC20View snapshot;

    /**
     * Sets the values for {@code name} and {@code symbol}, initializes {@code decimals} with a default
     * value of 18. To select a different value for {@code decimals}, use {@link #setDecimals(short)}.
     * The first two of these values are immutable: they can only be set once during construction.
     * Creates a token that does not generate events.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     */
    public ERC20(String name, String symbol) {
        this(name, symbol, (short) 18, false);
    }

    /**
     * Sets the values for {@code name} and {@code symbol}, initializes {@code decimals} with a default
     * value of 18. To select a different value for {@code decimals}, use {@link #setDecimals(short)}.
     * The first two of these values are immutable: they can only be set once during construction.
     * Creates a token that does not generate events, with the given {@code initialSupply}
     * assigned to the caller of this constructor.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     * @param initialSupply the initial supply assigned to the caller of this constructor
     */
    public @FromContract ERC20(String name, String symbol, int initialSupply) {
    	this(name, symbol, new UnsignedBigInteger(initialSupply));
    }

    /**
     * Sets the values for {@code name} and {@code symbol}, initializes {@code decimals} with a default
     * value of 18. To select a different value for {@code decimals}, use {@link #setDecimals(short)}.
     * The first two of these values are immutable: they can only be set once during construction.
     * Creates a token that does not generate events, with the given {@code initialSupply}
     * assigned to the caller of this constructor.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     * @param initialSupply the initial supply assigned to the caller of this constructor
     */
    public @FromContract ERC20(String name, String symbol, long initialSupply) {
    	this(name, symbol, new UnsignedBigInteger(initialSupply));
    }

    /**
     * Sets the values for {@code name} and {@code symbol}, initializes {@code decimals} with a default
     * value of 18. To select a different value for {@code decimals}, use {@link #setDecimals(short)}.
     * The first two of these values are immutable: they can only be set once during construction.
     * Creates a token that does not generate events, with the given {@code initialSupply}
     * assigned to the caller of this constructor.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     * @param initialSupply the initial supply assigned to the caller of this constructor
     */
    public @FromContract ERC20(String name, String symbol, UnsignedBigInteger initialSupply) {
    	this(name, symbol, (short) 18, false);

    	_mint(caller(), initialSupply);
    }

    /**
     * Sets the values for {@code name}, {@code symbol} and {@code decimals}.
     * To select a different value for {@code decimals}, use {@link #setDecimals(short)}.
     * The first two of these values are immutable: they can only be set once during construction.
     *
     * @param name the name of the token
     * @param symbol the symbol of the token
     * @param decimals the decimals to use
     * @param generateEvents true if and only if the token generates events
     */
    public ERC20(String name, String symbol, short decimals, boolean generateEvents) {
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
        this.generateEvents = generateEvents;
    }

    /**
     * Returns the name of the token.
     *
     * @return the name of the token
     */
    public final @View String name() {
        return name;
    }

    /**
     * Returns the symbol of the token, usually a shorter version of the name.
     *
     * @return the symbol of the token
     */
    public final @View String symbol() {
        return symbol;
    }

    /**
     * Returns the number of decimals used to get its user representation.
     * For example, if {@code decimals} equals `2`, a balance of `505` tokens should be displayed to a user as `5,05`
     * (`505 / 10 ** 2`).
     *
     * Tokens usually opt for a value of 18, imitating the relationship between Ether and Wei. This is the value
     * {@link ERC20} uses, unless {@link #setDecimals(short)} is called.
     *
     * NOTE: This information is only used for _display_ purposes: it in no way affects any of the arithmetic of the
     * contract, including {@link IERC20#balanceOf(Contract)} and {@link IERC20#transfer(Contract, UnsignedBigInteger)}.
     *
     * @return the number of decimals used to get its user representation
     */
    public final @View short decimals() {
        return decimals;
    }

    @Override
    public final @View UnsignedBigInteger totalSupply() {
        return totalSupply;
    }

    @Override
    public final @View UnsignedBigInteger balanceOf(Contract account) {
        return balances.getOrDefault(account, ZERO);
    }

    @Override
	public final @View int size() {
		return balances.size();
	}

	@Override
	public final @View Contract select(int k) {
		return balances.select(k);
	}

	/**
     * See {@link IERC20#transfer(Contract, UnsignedBigInteger)}.
     * The caller must have a balance of at least {@code amount}.
     *
     * @param recipient recipient of the transfer (it cannot be the null account)
     * @param amount number of tokens to transfer (it cannot be null)
     * @return true if the operation is successful
     */
    @Override
    public final @FromContract boolean transfer(Contract recipient, UnsignedBigInteger amount) {
        _transfer(caller(), recipient, amount);
        return true;
    }

    @Override
	public final @FromContract boolean transfer(Contract recipient, int amount) {
		return this.transfer(recipient, new UnsignedBigInteger(amount));
	}

    @Override
	public final @FromContract boolean transfer(Contract recipient, long amount) {
		return this.transfer(recipient, new UnsignedBigInteger(amount));
	}

    @Override
    public final @View UnsignedBigInteger allowance(Contract owner, Contract spender) {
        return allowances.getOrDefault(owner, StorageTreeMap::new).getOrDefault(spender, ZERO);
    }

    @Override
    public final @FromContract boolean approve(Contract spender, UnsignedBigInteger amount) {
        _approve(caller(), spender, amount);
        return true;
    }

    /**
     * OpenZeppelin: See {@link IERC20#transferFrom(Contract, Contract, UnsignedBigInteger)}.
     * The caller must have allowance for {@code sender}'s tokens of at least {@code amount}.
     *
     * @param sender origin of the transfer (it cannot be the null account, it must have a balance of at least {@code amount})
     * @param recipient recipient of the transfer (it cannot be the null account)
     * @param amount number of tokens to transfer (it cannot be null)
     * @return true if the operation is successful
     */
    @Override
    public final @FromContract boolean transferFrom(Contract sender, Contract recipient, UnsignedBigInteger amount) {
        transferFrom(caller(), sender, recipient, amount);
        return true;
    }

    @Override @View
	public final IERC20View snapshot() {
    	return snapshot;
	}

    private void updateShapshot() {
    	this.snapshot = new SnapshotImpl();
    }

    /**
     * Implementation of a snapshot of an ERC20 token contract.
     */
    @Exported
	protected class SnapshotImpl extends Storage implements IERC20View {
		private final UnsignedBigInteger totalSupply = ERC20.this.totalSupply;
		private final StorageMapView<Contract, UnsignedBigInteger> balances = ERC20.this.balances.snapshot(); 

		/**
		 * Creates the snapshot.
		 */
		protected SnapshotImpl() {}

		@Override
		public @View UnsignedBigInteger totalSupply() {
			return totalSupply;
		}

		@Override
		public @View UnsignedBigInteger balanceOf(Contract account) {
            return balances.getOrDefault(account, ZERO);
		}

		@Override
		public @View int size() {
			return balances.size();
		}

		@Override
		public @View Contract select(int k) {
			return balances.select(k);
		}

		@Override
		public @View IERC20View snapshot() {
			return this;
		}
	}

    /**
     * Implementation of a view of an ERC20 token contract.
     */
    @Exported
	protected class IERC20ViewImpl extends Storage implements IERC20View {

    	/**
    	 * Creates the view.
    	 */
    	protected IERC20ViewImpl() {}

    	@Override
		public @View UnsignedBigInteger totalSupply() {
			return ERC20.this.totalSupply();
		}

		@Override
		public @View UnsignedBigInteger balanceOf(Contract account) {
			return ERC20.this.balanceOf(account);
		}

		@Override
		public IERC20View snapshot() {
			return ERC20.this.snapshot();
		}

		@Override
		public @View int size() {
			return ERC20.this.size();
		}

		@Override
		public @View Contract select(int k) {
			return ERC20.this.select(k);
		}
	};

	@Override
	public IERC20View view() {
    	return new IERC20ViewImpl();
    }

    /**
     * Atomically increases the allowance granted to {@code spender} by the caller. This is an alternative
     * to {@link ERC20#approve(Contract, UnsignedBigInteger)} that can be used as a mitigation for problems described
     * in {@link IERC20#approve(Contract, UnsignedBigInteger)}.
     * Emits an {@link IERC20.Approval} event indicating the updated allowance.
     *
     * @param spender account authorized to spend on behalf of {@code owner}, it allowance will be increased (it cannot
     *                be the null account)
     * @param addedValue number of tokens to add from those {@code spender} can spend
     * @return true if the operation is successful
     */
    public final @FromContract boolean increaseAllowance(Contract spender, UnsignedBigInteger addedValue) {
    	Contract caller = caller();
        _approve(caller, spender, allowance(caller, spender).add(addedValue));
        return true;
    }

    /**
     * Atomically decreases the allowance granted to {@code spender} by the caller. This is an alternative
     * to {@link ERC20#approve(Contract, UnsignedBigInteger)} that can be used as a mitigation for problems described
     * in {@link IERC20#approve(Contract, UnsignedBigInteger)}.
     * Emits an {@link IERC20.Approval} event indicating the updated allowance.
     *
     * @param spender account authorized to spend on behalf of {@code owner}, it allowance will be decreased (it cannot
     *                be the null account, it must have allowance for the caller of at least {@code subtractedValue})
     * @param subtractedValue number of tokens to remove from those {@code spender} can spend
     * @return true if the operation is successful
     */
    public final @FromContract boolean decreaseAllowance(Contract spender, UnsignedBigInteger subtractedValue) {
        Contract caller = caller();
		_approve(caller, spender, allowance(caller, spender).subtract(subtractedValue, "approve rejected: allowance decreased below zero"));
        return true;
    }

    /**
	 * Sets {@code decimals} to a value other than the default one of 18. WARNING: This function should
	 * only be called from the constructor. Most applications that interact with token contracts will not expect
	 * {@code decimals} to ever change, and may work incorrectly if it does.
	 *
	 * @param decimals number of decimals used to get token user representation
	 */
	protected final void setDecimals(short decimals) {
		this.decimals = decimals;
	}

	/**
	 * Generates the given event if events are allowed for this token.
	 * 
	 * @param event the event to generate
	 */
	protected final void event(Event event) {
		if (generateEvents)
			Takamaka.event(event);
	}

	/**
	 * Internal implementation of the {@link #transferFrom(Contract, Contract, Contract, UnsignedBigInteger)} method.
	 * 
	 * @param caller the caller of the transfer
	 * @param sender origin of the transfer (it cannot be the null account, it must have a balance of at least {@code amount})
     * @param recipient recipient of the transfer (it cannot be the null account)
     * @param amount number of tokens to transfer (it cannot be null)
	 */
	protected final void transferFrom(Contract caller, Contract sender, Contract recipient, UnsignedBigInteger amount) {
		_transfer(sender, recipient, amount);
	    _approve(sender, caller, allowance(sender, caller).subtract(amount, "transfer rejected: transfer amount exceeds allowance"));
	}

	/**
	 * OpenZeppelin: Hook that is called before any transfer of tokens. This includes minting and burning.
	 *
	 * Calling conditions:
	 *
	 * <ol>
	 * <li> when {@code from} and {@code to} are both non-null, {@code amount} of {@code from}'s tokens will be to
	 *      transferred to {@code to}</li>
	 * <li> when {@code from} is null, {@code amount} tokens will be minted for {@code to}</li>
	 * <li> when {@code to} is null, {@code amount} of {@code from}'s tokens will be burned</li>
	 * <li> {@code from} and {@code to} are never both null</li>
	 * </ol>
	 *
	 * @param from token transfer source account
	 * @param to token transfer recipient account
	 * @param amount amount of tokens transferred
	 */
	protected void beforeTokenTransfer(Contract from, Contract to, UnsignedBigInteger amount) { }

	/**
     * Moves tokens {@code amount} from {@code sender} to {@code recipient}. This is internal function is
     * equivalent to {@link #transfer(Contract, UnsignedBigInteger)}, and can be used to, e.g., implement automatic
     * token fees, slashing mechanisms, etc. Emits a {@link IERC20.Transfer} event.
     *
     * @param sender origin of the transfer (it cannot be the null account, it must have a balance of at least {@code amount})
     * @param recipient recipient of the transfer (it cannot be the null account)
     * @param amount number of tokens to transfer (it cannot be null)
     */
    protected void _transfer(Contract sender, Contract recipient, UnsignedBigInteger amount) {
        require(sender != null, "transfer rejected: transfer from the null account");
        require(recipient != null, "transfer rejected: transfer to the null account");
        require(amount != null, "transfer rejected: amount cannot be null");

        beforeTokenTransfer(sender, recipient, amount);

        balances.put(sender, balanceOf(sender).subtract(amount, "transfer rejected: transfer amount exceeds balance"));
        balances.put(recipient, balanceOf(recipient).add(amount));
        updateShapshot();

        event(new Transfer(sender, recipient, amount));
    }

    /**
	 * Sets {@code amount} as the allowance of {@code spender} over the {@code owner}s tokens.
	 * This is internal function is equivalent to {@link #approve(Contract, UnsignedBigInteger)}, and can be used to,
	 * e.g., set automatic allowances for certain subsystems, etc.
	 * Emits an {@link IERC20.Approval} event.
	 *
	 * @param owner account that authorizes to spend (it cannot be the null account)
	 * @param spender account authorized to spend on behalf of {@code owner} (it cannot be the null account)
	 * @param amount amount of tokens that {@code spender} can spend on behalf of {@code owner} (it cannot be null)
	 */
	protected void _approve(Contract owner, Contract spender, UnsignedBigInteger amount) {
	    require(owner != null, "approve rejected: approve from the null account");
	    require(spender != null, "approve rejected: approve to the null account");
	    require(amount != null, "approve rejected: amount cannot be null");
	
	    StorageMap<Contract, UnsignedBigInteger> ownerAllowances = allowances.getOrDefault(owner, StorageTreeMap::new);
	    ownerAllowances.put(spender, amount);
	    allowances.put(owner, ownerAllowances);
	
	    event(new Approval(owner, spender, amount));
	}

	/**
     * Creates {@code amount} tokens and assigns them to {@code account}, increasing the total supply.
     * Emits a {@link IERC20.Transfer} event with {@code from} set to the null account.
     *
     * @param account recipient of the created tokens (it cannot be the null account)
     * @param amount number of tokens to create (it cannot be null)
     */
    protected void _mint(Contract account, UnsignedBigInteger amount) {
        require(account != null, "Mint rejected: mint to the null account");
        require(amount != null, "Mint rejected: amount cannot be null");

        beforeTokenTransfer(null, account, amount);

        totalSupply = totalSupply.add(amount);
        balances.put(account, balanceOf(account).add(amount));
        updateShapshot();

        event(new Transfer(null, account, amount));
    }

    /**
     * Destroys {@code amount} tokens from {@code account}, reducing the total supply.
     * Emits a {@link IERC20.Transfer} event with {@code to} set to the null account.
     *
     * @param account source of tokens to burn (it cannot be the null account and must have at least {@code amount} tokens)
     * @param amount number of tokens to burn (it cannot be null)
     */
    protected void _burn(Contract account, UnsignedBigInteger amount) {
        require(account != null, "burn rejected: burn to the null account");
        require(amount != null, "burn rejected: amount cannot be null");

        beforeTokenTransfer(account, null, amount);

        balances.put(account, balanceOf(account).subtract(amount, "burn rejected: burn amount exceeds balance"));
        totalSupply = totalSupply.subtract(amount);
        updateShapshot();

        event(new Transfer(account, null, amount));
    }
}