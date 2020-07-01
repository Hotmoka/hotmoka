package io.hotmoka.network.rest;

import io.hotmoka.network.service.run.NodeRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("run")
public class RunController {

    @Autowired
    private NodeRunService nodeRunService;

    @RequestMapping("/instanceMethodCallTransaction")
    public Object instanceMethodCallTransaction() {
        return this.nodeRunService.runInstanceMethodCallTransaction();
    }

    @RequestMapping("/staticMethodCallTransaction")
    public Object staticMethodCallTransaction() {
        return this.nodeRunService.runStaticMethodCallTransaction();
    }
}