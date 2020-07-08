package io.hotmoka.network.internal.rest;

import io.hotmoka.network.internal.models.storage.StorageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.hotmoka.network.internal.services.NodeGetService;

@RestController
@RequestMapping("get")
public class GetController {

    @Autowired
    private NodeGetService nodeGetService;

    @GetMapping("/takamakaCode")
    public Object getTakamakaCode() {
        return this.nodeGetService.getTakamakaCode();
    }

    @GetMapping("/manifest")
    public Object getManifest() {
        return this.nodeGetService.getManifest();
    }

    @PostMapping("/state")
    public Object getState(@RequestBody StorageModel request) {
        return this.nodeGetService.getState(request);
    }

    @PostMapping("/classTag")
    public Object getClassTag(@RequestBody StorageModel request) {
        return this.nodeGetService.getClassTag(request);
    }
}