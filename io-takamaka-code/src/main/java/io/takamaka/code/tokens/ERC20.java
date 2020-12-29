package io.takamaka.code.tokens;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;
import io.takamaka.code.util.UnsignedBigInteger;
import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.View;

/**
 * Implementation inspired by OpenZeppelin:
 * https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/token/ERC20/ERC20.sol
 *
 * OpenZeppelin: Implementation of the {IERC20} interface.
 *
 *  This implementation is agnostic to the way tokens are created. This means that a supply mechanism has to be added
 *  in a derived contract using {_mint}. For a generic mechanism see {ERC20MinterPauser}.
 *
 *  TIP: For a detailed writeup see our guide https://forum.zeppelin.solutions/t/how-to-implement-erc20-supply-
 *  mechanisms/226[How to implement supply mechanisms].
 *
 *  We have followed general OpenZeppelin guidelines: functions revert instead of returning `false` on failure. This
 *  behavior is nonetheless conventional and does not conflict with the expectations of ERC20 applications.
 *
 *  Additionally, an {Approval} event is emitted on calls to {transferFrom}. This allows applications to reconstruct
 *  the allowance for all accounts just by listening to said events. Other implementations of the EIP may not emit
 *  these events, as it isn't required by the specification.
 *
 *  Finally, the non-standard {decreaseAllowance} and {increaseAllowance} functions have been added to mitigate the
 *  well-known issues around setting allowances. See {IERC20-approve}.
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
     * OpenZeppelin: Sets the values for {name} and {symbol}, initializes {decimals} with a default value of 18.
     *  To select a different value for {decimals}, use {_setupDecimals}.
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
     *  For example, if `decimals` equals `2`, a balance of `505` tokens should be displayed to a user as `5,05`
     *  (`505 / 10 ** 2`).
     *
     *  Tokens usually opt for a value of 18, imitating the relationship between Ether and Wei. This is the value
     *  {ERC20} uses, unless {_setupDecimals} is called.
     *
     *  NOTE: This information is only used for _display_ purposes: it in no way affects any of the arithmetic of the
     *  contract, including {IERC20-balanceOf} and {IERC20-transfer}.
     *
     * @return the number of decimals used to get its user representation
     */
    public final @View short decimals() {
        return _decimals;
    }

    /**
     * OpenZeppelin: See {IERC20-totalSupply}.
     *
     * @return the amount of tokens in existence
     */
    @Override
    public final @View UnsignedBigInteger totalSupply() {
        return _totalSupply;
    }

    /**
     * OpenZeppelin: See {IERC20-balanceOf}.
     *
     * @param account account whose balance you want to check
     * @return the amount of tokens owned by `account`
     */
    @Override
    public final @View UnsignedBigInteger balanceOf(Contract account) {
        return _balances.getOrDefault(account, ZERO);
    }

    /**
     * OpenZeppelin: See {IERC20-transfer}.
     *
     * Requirements:
     * - the caller must have a balance of at least `amount`.
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
     * OpenZeppelin: See {IERC20-allowance}.
     *
     * @param owner account that allows `spender` to spend its tokens
     * @param spender account authorized to spend on behalf of `owner`
     * @return the remaining number of tokens that `spender` will be allowed to spend on behalf of `owner`
     */
    @Override
    public @View UnsignedBigInteger allowance(Contract owner, Contract spender) {
        return _allowances.getOrDefault(owner, StorageTreeMap::new).getOrDefault(spender, ZERO);
    }

    /**
     * OpenZeppelin: See {IERC20-approve}.
     *
     * @param spender account authorized to spend on behalf of caller (it cannot be the null account)
     * @param amount amount of tokens that `spender` can spend on behalf of the caller (it cannot be null)
     * @return true if the operation is successful
     */
    @Override
    public @FromContract boolean approve(Contract spender, UnsignedBigInteger amount) {
        _approve(caller(), spender, amount);
        return true;
    }

    /**
     * OpenZeppelin: See {IERC20-transferFrom}.
     *  Emits an {Approval} event indicating the updated allowance. This is not required by the EIP. See the note at the
     *  beginning of {ERC20};
     *
     *  Requirements:
     *  - the caller must have allowance for ``sender``'s tokens of at least `amount`.
     *
     * @param sender origin of the transfer (it cannot be the null account, it must have a balance of at least `amount`)
     * @param recipient recipient of the transfer (it cannot be the null account)
     * @param amount number of tokens to transfer (it cannot be null)
     * @return true if the operation is successful
     */
    @Override
    public @FromContract boolean transferFrom(Contract sender, Contract recipient, UnsignedBigInteger amount) {
        _transfer(sender, recipient, amount);
        _approve(sender, caller(), _allowances.getOrDefault(sender, StorageTreeMap::new)
                .getOrDefault(caller(), ZERO)
                .subtract(amount, "Transfer Rejected: transfer amount exceeds allowance"));
        return true;
    }

    /**
     * OpenZeppelin: Atomically increases the allowance granted to `spender` by the caller. This is an alternative to
     *  {approve} that can be used as a mitigation for problems described in {IERC20-approve}.
     *  Emits an {Approval} event indicating the updated allowance.
     *
     * @param spender account authorized to spend on behalf of `owner`, it allowance will be increased (it cannot be
     *                the null account)
     * @param addedValue number of tokens to add from those `spender` can spend
     * @return true if the operation is successful
     */
    public @FromContract boolean increaseAllowance(Contract spender, UnsignedBigInteger addedValue) {
        _approve(caller(), spender, _allowances.getOrDefault(caller(), StorageTreeMap::new)
                .getOrDefault(spender, ZERO).add(addedValue));
        return true;
    }

    /**
     * OpenZeppelin: Atomically decreases the allowance granted to `spender` by the caller. This is an alternative to
     *  {approve} that can be used as a mitigation for problems described in {IERC20-approve}.
     *  Emits an {Approval} event indicating the updated allowance.
     *
     * @param spender account authorized to spend on behalf of `owner`, it allowance will be decreased (it cannot be
     *                the null account, it must have allowance for the caller of at least `subtractedValue`)
     * @param subtractedValue number of tokens to remove from those `spender` can spend
     * @return true if the operation is successful
     */
    public @FromContract boolean decreaseAllowance(Contract spender, UnsignedBigInteger subtractedValue) {
        _approve(caller(), spender, _allowances.getOrDefault(caller(), StorageTreeMap::new)
                .getOrDefault(spender, ZERO)
                .subtract(subtractedValue, "Approve rejected: decreased allowance below zero"));
        return true;
    }

    /**
     * OpenZeppelin: Moves tokens `amount` from `sender` to `recipient`. This is internal function is equivalent to
     *  {transfer}, and can be used to. E.g. implement automatic token fees, slashing mechanisms, etc.
     *  Emits a {Transfer} event.
     *
     * @param sender origin of the transfer (it cannot be the null account, it must have a balance of at least `amount`)
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
     * OpenZeppelin: Creates `amount` tokens and assigns them to `account`, increasing the total supply.
     *
     * Emits a {Transfer} event with `from` set to the null account.
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
     * OpenZeppelin: Destroys `amount` tokens from `account`, reducing the total supply.
     *
     * Emits a {Transfer} event with `to` set to the null account.
     *
     * @param account source of tokens to burn (it cannot be the null account and must have at least `amount` tokens)
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
     * OpenZeppelin: Sets `amount` as the allowance of `spender` over the `owner`s tokens. This is internal function is
     *  equivalent to {approve}, and can be used to. E.g. set automatic allowances for certain subsystems, etc.
     *  Emits an {Approval} event.
     *
     * @param owner account that authorizes to spend (it cannot be the null account)
     * @param spender account authorized to spend on behalf of `owner` (it cannot be the null account)
     * @param amount amount of tokens that `spender` can spend on behalf of `owner` (it cannot be null)
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
     * OpenZeppelin: Sets {decimals} to a value other than the default one of 18. WARNING: This function should only be
     *  called from the constructor. Most applications that interact with token contracts will not expect {decimals} to
     *  ever change, and may work incorrectly if it does.
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
     * - when `from` and `to` are both non-null, `amount` of ``from``'s tokens will be to transferred to `to`.
     * - when `from` is null, `amount` tokens will be minted for `to`.
     * - when `to` is null, `amount` of ``from``'s tokens will be burned.
     * - `from` and `to` are never both null.
     *
     * @param from token transfer source account
     * @param to token transfer recipient account
     * @param amount amount of tokens transferred
     */
    protected void _beforeTokenTransfer(Contract from, Contract to, UnsignedBigInteger amount) { }
}