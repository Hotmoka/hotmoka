package io.hotmoka.examples.wine.staff;

import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.FromContract;

public class ResourcePending extends Event {

    @FromContract
    public ResourcePending(Worker worker) {
        super();
        worker.addPending();
    }
}
