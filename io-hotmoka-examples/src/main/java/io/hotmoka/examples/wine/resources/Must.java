package io.hotmoka.examples.wine.resources;

import io.takamaka.code.lang.FromContract;
import io.hotmoka.examples.wine.staff.Role;
import io.hotmoka.examples.wine.staff.SupplyChain;
import io.hotmoka.examples.wine.staff.Worker;

import static io.takamaka.code.lang.Takamaka.require;

public final class Must extends Resource {

    @FromContract
    public Must(SupplyChain chain, String name, String description, int amount, Resource origin) {
        super(chain, name, description, amount, origin);
        require(((Worker) caller()).getRole() == Role.WINE_MAKING_CENTRE,
                "Only a Wine-Making Centre can create a new object Must.");
    }
}
