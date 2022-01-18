package io.hotmoka.examples.wine.resources;

import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.View;
import io.hotmoka.examples.wine.staff.Authority;
import io.hotmoka.examples.wine.staff.Role;
import io.hotmoka.examples.wine.staff.SupplyChain;
import io.hotmoka.examples.wine.staff.Worker;

import static io.takamaka.code.lang.Takamaka.require;

public final class Grape extends Resource {
    private GrapeState state;

    @FromContract
    public Grape(SupplyChain chain, String name, String description, int amount, Resource origin) {
        super(chain, name, description, amount, origin);
        require(((Worker) caller()).getRole() == Role.PRODUCER,
                "Only a Producer can create a new object Grape.");
    }

    @View
    public GrapeState getState() {
        return state;
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void setState(GrapeState newState, Authority authority) {
        require(chain.getAuthorities().contains(authority), "Only an Authority can change state to Grape.");
        state = newState;
    }
}
