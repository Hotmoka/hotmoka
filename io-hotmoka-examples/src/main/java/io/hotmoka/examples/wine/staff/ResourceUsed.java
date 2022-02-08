package io.hotmoka.examples.wine.staff;

import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.FromContract;

public class ResourceUsed extends Event {

    @FromContract
    public ResourceUsed(Worker worker) {
        worker.removePending();
    }
}
