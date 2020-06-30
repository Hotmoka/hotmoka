package io.hotmoka.network.rest;

import io.hotmoka.network.service.NodeRestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("node")
public class NodeRestController {

    @Autowired
    private NodeRestService nodeRestService;


    @RequestMapping("/takamakaCode")
    public Object getTakamakaCode() {
        return this.nodeRestService.getTakamakaCode();
    }

    @RequestMapping("/manifest")
    public Object getManifest() {
        return this.nodeRestService.getManifest();
    }

    @RequestMapping("/state")
    public Object getState() {
        return this.nodeRestService.getState();
    }
}
