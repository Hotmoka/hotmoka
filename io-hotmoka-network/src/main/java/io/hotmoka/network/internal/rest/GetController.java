package io.hotmoka.network.internal.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/state")
    public Object getState() {
        return this.nodeGetService.getState();
    }

    @GetMapping("/classTag")
    public Object getClassTag() {
        return this.nodeGetService.getClassTag();
    }
}