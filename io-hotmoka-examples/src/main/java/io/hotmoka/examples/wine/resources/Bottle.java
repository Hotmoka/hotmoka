package io.hotmoka.examples.wine.resources;

import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Takamaka;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;
import io.hotmoka.examples.wine.staff.Authority;
import io.hotmoka.examples.wine.staff.Role;
import io.hotmoka.examples.wine.staff.SupplyChain;
import io.hotmoka.examples.wine.staff.Worker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static io.takamaka.code.lang.Takamaka.require;

public final class Bottle extends Resource {
    private int aging;
    private String packaging;
    private String labelling;
    private Certification certification;
    private String creationDate; // FIXME: LocalData not supported in Storage classes
    private int sold = 0;
    private StorageList<String> saleDates = new StorageLinkedList<>(); // FIXME: LocalData not supported in Storage classes

    @FromContract
    public Bottle(SupplyChain chain, String name, String description, int amount, Resource origin) {
        super(chain, name, description, amount, origin);
        require(((Worker) caller()).getRole() == Role.BOTTLING_CENTRE,
                "Only a Bottling Centre can create a new object Bottle.");
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void setAttributes(int aging, String packaging, String labelling) {
        this.aging = aging;
        this.packaging = packaging;
        this.labelling = labelling;
        creationDate = LocalDate.ofInstant
                (Instant.ofEpochMilli(Takamaka.now()), ZoneId.of("Europe/Rome")).toString();
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void sell(int amount, Worker retailer) {
        require(retailer.getRole() == Role.RETAILER, "Only a retailer can sell bottles.");
        require(this.amount - sold > amount, "Check if there are enough bottles available.");
        sold += amount;
        LocalDate now = LocalDate.ofInstant
                (Instant.ofEpochMilli(Takamaka.now()), ZoneId.of("Europe/Rome")); // LocalDateTime could be better here?
        saleDates.add(now.toString());
        // FIXME: toString() cause following error:
        //  io.hotmoka.beans.TransactionException: io.hotmoka.nodes.NonWhiteListedCallException:
        //  cannot prove that toString() on this object is deterministic and terminating
        if (sold == amount)
            retailer.removeProduct(this);
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void certify(Certification name, Authority authority) {
        require(chain.getAuthorities().contains(authority), "Only an Authority can approve a certification.");
        certification = name;
        // TODO: Add link to actual certificate...
    }
}
