package io.hotmoka.examples.wine.staff;

import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;
import io.hotmoka.examples.wine.resources.Resource;

import static io.takamaka.code.lang.Takamaka.require;

public final class Authority extends Staff {
    private StorageList<Resource> products = new StorageLinkedList<>();

    public Authority(ExternallyOwnedAccount owner, String name) {
        super(owner, name);
    }

    @FromContract(SupplyChain.class)
    public void checkProduct(Resource product) {
        require(product.getSupplyChain() == caller(),
                "Worker calling is not part of the product supply chain.");
        products.add(product);
    }
}
