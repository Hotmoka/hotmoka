package io.hotmoka.network.rest;

import io.hotmoka.network.service.NodeRestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("run")
public class runController {

    @Autowired
    private NodeRestService nodeRestService;

    @RequestMapping("/instanceMethodCallTransaction")
    public Object instanceMethodCallTransaction() {
        return null; // TODO
    }

    // TODO: all other post methods
}