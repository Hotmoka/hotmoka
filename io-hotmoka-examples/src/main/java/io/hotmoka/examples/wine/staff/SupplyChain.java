package io.hotmoka.examples.wine.staff;

import io.hotmoka.examples.wine.resources.Grape;
import io.hotmoka.examples.wine.resources.GrapeState;
import io.hotmoka.examples.wine.resources.Resource;
import io.hotmoka.examples.wine.resources.Wine;
import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;
import io.takamaka.code.util.StorageListView;

import static io.takamaka.code.lang.Takamaka.require;

public class SupplyChain extends Contract {
    private final ExternallyOwnedAccount owner;
    private StorageList<Worker> workers = new StorageLinkedList<>();
    private StorageList<Administrator> administrators = new StorageLinkedList<>();
    private StorageList<Authority> authorities = new StorageLinkedList<>();

    public SupplyChain(ExternallyOwnedAccount owner) {
        this.owner = owner;
    }

    public StorageListView<Worker> getWorkers() {
        return workers.snapshot();
    }

    public StorageListView<Administrator> getAdministrators() {
        return administrators.snapshot();
    }

    public StorageListView<Authority> getAuthorities() {
        return authorities.snapshot();
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void add(Staff staff, Administrator administrator) {
        require(caller() == owner || (administrators.contains(administrator) && administrator != staff),
                "Only the owner and the administrators can add new Staff.");
        if (staff instanceof Worker)
            workers.add((Worker) staff);
        else if (staff instanceof Administrator)
            administrators.add((Administrator) staff);
        else
            authorities.add((Authority) staff);
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void remove(Staff staff, Administrator administrator) {
        require(caller() == owner || (administrators.contains(administrator) && administrator != staff),
                "Only the owner and the administrators can remove Staff.");
        if (staff instanceof Worker)
            workers.remove(staff);
        else if (staff instanceof Administrator)
            administrators.remove(staff);
        else
            authorities.remove(staff);
    }

    @FromContract(ExternallyOwnedAccount.class)
    public Worker transferProduct(Resource product, Worker origin) {
        require(workers.contains(origin), "The Worker has to operate in this chain.");
        require(origin.getRole() != Role.RETAILER, "Retailers are the last Workers in the supply chain.");
        require(origin.getProducts().contains(product),
                "The Resource must be one of the products possessed by the Worker.");
        if (product instanceof Grape)
            require(((Grape) product).getState() == GrapeState.ELIGIBLE, "Grape must be eligible to be transferred.");
        if (product instanceof Wine)
            require(((Wine) product).isEligible(), "Wine must be eligible to be transferred.");
        Worker next = null;
        for (Worker worker : workers) {
            if (worker.getRole().ordinal() == origin.getRole().ordinal() + 1 && worker.isAvailable()) {
                next = worker;
                next.addProduct(product);
                next.notifyNewProduct();
                origin.removeProduct(product);
                product.addProducer(next);
                break;
            }
        }
        require(next != null, "There are no next Workers available in the supply chain.");
        return next;
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void callAuthority(Resource product, Worker origin) {
        require(workers.contains(origin), "The Worker has to operate in this chain.");
        require((product instanceof Grape && origin.getRole() == Role.PRODUCER) ||
                        (product instanceof Wine && origin.getRole() == Role.WINE_MAKING_CENTRE),
                "Only resources to be checked are Grape and Wine.");
        if (product instanceof Grape)
            require(((Grape) product).getState() != GrapeState.ELIGIBLE,
                    "If Grape is already eligible it can be transferred.");
        if (product instanceof Wine)
            require(!((Wine) product).isEligible(), "If Wine is already eligible it can be transferred.");
        boolean found = false;
        for (Authority authority : authorities) {
            authority.checkProduct(product);
            found = true;
            break;
        }
        require(found, "There is no Authority available in the supply chain.");
    }
}