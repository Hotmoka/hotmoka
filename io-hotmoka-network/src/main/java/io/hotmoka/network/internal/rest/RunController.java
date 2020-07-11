package io.hotmoka.network.internal.rest;

import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.models.transactions.MethodCallTransactionRequestModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.hotmoka.network.internal.services.NodeRunService;

@RestController
@RequestMapping("run")
public class RunController {

    @Autowired
    private NodeRunService nodeRunService;

    @PostMapping("/instanceMethodCallTransaction")
    public @ResponseBody StorageValueModel instanceMethodCallTransaction(@RequestBody  MethodCallTransactionRequestModel request) {
        return this.nodeRunService.runInstanceMethodCallTransaction(request);
    }

    @PostMapping("/staticMethodCallTransaction")
    public @ResponseBody StorageValueModel staticMethodCallTransaction(@RequestBody MethodCallTransactionRequestModel request) {
        return this.nodeRunService.runStaticMethodCallTransaction(request);
    }
}