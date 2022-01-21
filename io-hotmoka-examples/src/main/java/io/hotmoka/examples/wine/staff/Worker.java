package io.hotmoka.examples.wine.staff;

import io.hotmoka.examples.wine.resources.*;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;

import static io.takamaka.code.lang.Takamaka.require;

public final class Worker extends Staff {
    private final Role role;
    private int max_products;
    private StorageList<Resource> products = new StorageLinkedList<>();
    private boolean waiting = false;

    public Worker(ExternallyOwnedAccount owner, String name, Role role, Integer max_products) {
        super(owner, name);
        this.role = role;
        this.max_products = max_products;
    }

    @View
    public Role getRole() {
        return role;
    }

    @View
    public StorageList<Resource> getProducts() {
        return products;
    }

    @View
    public boolean isAvailable() {
        return products.size() < max_products;
    }

    @FromContract(ExternallyOwnedAccount.class)
    public Resource addProduct(SupplyChain chain, String name, String description, Integer amount,
                               Resource prevProduct) {
        require(caller() == owner,
                "Only this worker can add a new product to his own products.");
        require(chain != null, "The SupplyChain must exist.");
        require(chain.getWorkers().contains(this), "The Worker must be part of the supply chain reported.");
        require(amount > 0, "The amount must be greater than zero.");
        require(isAvailable(), "This Worker cannot accept more products.");
        // Except for Vine, every product in this phase is completely transformed in the next one,
        // so it is removed from the ones possessed
        Resource product = null;
        if (prevProduct == null)
            product = new Vine(chain, name, description, amount, null);
        else if (prevProduct instanceof Vine)
            product = new Grape(chain, name, description, amount, prevProduct);
        else if (prevProduct instanceof Grape) {
            product = new Must(chain, name, description, amount, prevProduct);
            products.remove(prevProduct);
        } else if (prevProduct instanceof Must) {
            product = new Wine(chain, name, description, amount, prevProduct);
            products.remove(prevProduct);
        } else if (prevProduct instanceof Wine) {
            product = new Bottle(chain, name, description, amount, prevProduct);
            products.remove(prevProduct);
        }
        products.add(product);
        checkNewProducts();
        return product;
    }

    @FromContract
    public void addProduct(Resource product) {
        require(caller() == owner || caller() instanceof SupplyChain,
                "Only the current worker or the supply chain can add products.");
        require(isAvailable(), "This Worker cannot accept more products.");
        products.add(product);
    }

    public void removeProduct(Resource product) {
        products.remove(product);
    }

    @FromContract(SupplyChain.class)
    public void notifyNewProduct() {
        waiting = true;
    }

    // Check if there are still new products waiting to be processed
    private void checkNewProducts() {
        boolean newProducts = false;
        if (role == Role.WINE_MAKING_CENTRE) {
            for (Resource resource : products) {
                if (resource instanceof Grape) {
                    newProducts = true;
                    break;
                }
            }
        } else if (role != Role.PRODUCER) {
            for (Resource resource : products) {
                if (resource instanceof Bottle) {
                    newProducts = true;
                    break;
                }
            }
        }
        waiting = newProducts;
    }
}