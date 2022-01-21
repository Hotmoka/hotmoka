package io.hotmoka.examples.wine.staff;

import io.takamaka.code.lang.ExternallyOwnedAccount;

public final class Administrator extends Staff {

    public Administrator(ExternallyOwnedAccount owner, String name) {
        super(owner, name);
    }
}