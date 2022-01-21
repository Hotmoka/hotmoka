package io.hotmoka.examples.wine.staff;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.ExternallyOwnedAccount;

public abstract class Staff extends Contract {
    protected final ExternallyOwnedAccount owner;
    protected final String name;

    protected Staff(ExternallyOwnedAccount owner, String name) {
        this.owner = owner;
        this.name = name;
    }
}