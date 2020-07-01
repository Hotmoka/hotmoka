package io.hotmoka.network.rest;

import io.hotmoka.network.service.NodeRestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("add")
public class AddController {

    @Autowired
    private NodeRestService nodeRestService;

    @RequestMapping("/jarStoreInitialTransaction")
    public Object jarStoreInitialTransaction() {
        return null; // TODO
    }

    // TODO: all other add methods
}