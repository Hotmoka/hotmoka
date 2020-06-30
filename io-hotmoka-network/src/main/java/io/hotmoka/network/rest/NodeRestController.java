package io.hotmoka.network.rest;

import io.hotmoka.network.service.NodeRestServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("node")
public class NodeRestController {

    @Autowired
    private NodeRestServiceImpl nodeRestService;


    @RequestMapping("/takamakaCode")
    public Object getTakamakaCode() {
        return this.nodeRestService.getTakamakaCode();
    }
}
