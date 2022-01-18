package io.hotmoka.examples.wine.resources;

import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;
import io.hotmoka.examples.wine.staff.Role;
import io.hotmoka.examples.wine.staff.SupplyChain;
import io.hotmoka.examples.wine.staff.Worker;

import static io.takamaka.code.lang.Takamaka.require;

public final class Vine extends Resource {
    private StorageList<String> fertilizers = new StorageLinkedList<>();
    private StorageList<String> pesticides = new StorageLinkedList<>();
    private String harvest; // FIXME: LocalDate not supported in Storage class

    @FromContract
    public Vine(SupplyChain chain, String name, String description, int amount, Resource origin) {
        super(chain, name, description, amount, origin);
        require(((Worker) caller()).getRole() == Role.PRODUCER,
                "Only a Producer can create a new object Vine.");
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void addFertilizer(String fertilizer) {
        fertilizers.add(fertilizer);
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void addPesticide(String pesticide) {
        fertilizers.add(pesticide);
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void setHarvestDate(String date) {
        harvest = date;
    } // TODO: Use LocalDate.parse() if possible
}
