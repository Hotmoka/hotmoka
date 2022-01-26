package io.hotmoka.examples.wine.staff;

import io.hotmoka.examples.wine.resources.Resource;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;
import io.takamaka.code.util.StorageListView;

public final class Authority extends Staff {
    private StorageList<Resource> products = new StorageLinkedList<>();

    public Authority(ExternallyOwnedAccount owner, String name) {
        super(owner, name);
    }

    public StorageListView<Resource> getProducts() {
        return products.snapshot();
    }

    @FromContract(SupplyChain.class)
    public void checkProduct(Resource product) {
        products.add(product);
    }
}
