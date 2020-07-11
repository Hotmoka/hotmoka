package io.hotmoka.network.internal.rest;

import io.hotmoka.network.internal.models.ClassTagModel;
import io.hotmoka.network.internal.models.StateModel;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.services.NodeGetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("get")
public class GetController {

    @Autowired
    private NodeGetService nodeGetService;

    @GetMapping("/takamakaCode")
    public @ResponseBody StorageReferenceModel getTakamakaCode() {
        return this.nodeGetService.getTakamakaCode();
    }

    @GetMapping("/manifest")
    public @ResponseBody StorageReferenceModel getManifest() {
        return this.nodeGetService.getManifest();
    }

    @PostMapping("/state")
    public @ResponseBody StateModel getState(@RequestBody StorageReferenceModel request) {
        return this.nodeGetService.getState(request);
    }

    @PostMapping("/classTag")
    public @ResponseBody ClassTagModel getClassTag(@RequestBody StorageReferenceModel request) {
        return this.nodeGetService.getClassTag(request);
    }
}