package io.hotmoka.network.rest;

import io.hotmoka.network.service.get.NodeGetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("get")
public class GetController {

    @Autowired
    private NodeGetService nodeGetService;

    @RequestMapping("/takamakaCode")
    public Object getTakamakaCode() {
        return this.nodeGetService.getTakamakaCode();
    }

    @RequestMapping("/manifest")
    public Object getManifest() {
        return this.nodeGetService.getManifest();
    }

    @RequestMapping("/state")
    public Object getState() {
        return this.nodeGetService.getState();
    }

    @RequestMapping("/classTag")
    public Object getClassTag() {
        return this.nodeGetService.getClassTag();
    }
}