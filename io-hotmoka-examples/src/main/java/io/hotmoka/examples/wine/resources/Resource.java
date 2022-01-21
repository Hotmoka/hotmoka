package io.hotmoka.examples.wine.resources;

import io.takamaka.code.lang.*;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;
import io.hotmoka.examples.wine.staff.Staff;
import io.hotmoka.examples.wine.staff.SupplyChain;
import io.hotmoka.examples.wine.staff.Worker;

import static io.takamaka.code.lang.Takamaka.require;

@Exported
public abstract
class Resource extends Storage {
    protected final SupplyChain chain;
    protected final String name;
    protected String description;
    protected int amount;
    protected final Resource origin;
    protected StorageList<Resource> produced = new StorageLinkedList<>();
    protected StorageList<Worker> producers = new StorageLinkedList<>();
    protected String analysis;

    @FromContract(Worker.class)
    public Resource(SupplyChain chain, String name, String description, int amount, Resource origin) {
        this.chain = chain;
        this.name = name;
        this.description = description;
        this.amount = amount;
        this.origin = origin;
        if (origin != null)
            this.origin.produced.add(this);
        this.producers.add((Worker) caller());
    }

    @View
    public SupplyChain getSupplyChain() {
        return chain;
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void addAnalysisResults(String url, Staff staff) {
        require((chain.getWorkers().contains(staff) && ((Worker) staff).getProducts().contains(this))
                        || chain.getAuthorities().contains(staff),
                "Only the Worker who currently possess the resource or the Authority can add a new analysis.");
        analysis = url;
    }
}
