package io.hotmoka.service.internal.http.controllers;

import io.hotmoka.service.internal.services.RunService;
import io.hotmoka.service.models.requests.InstanceMethodCallTransactionRequestModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.hotmoka.service.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.service.models.values.StorageValueModel;

@RestController
@RequestMapping("run")
public class HTTP_RunController {

    @Autowired
    private RunService nodeRunService;

    @PostMapping("/instanceMethodCallTransaction")
    public @ResponseBody StorageValueModel instanceMethodCallTransaction(@RequestBody InstanceMethodCallTransactionRequestModel request) {
        return nodeRunService.runInstanceMethodCallTransaction(request);
    }

    @PostMapping("/staticMethodCallTransaction")
    public @ResponseBody StorageValueModel staticMethodCallTransaction(@RequestBody StaticMethodCallTransactionRequestModel request) {
        return nodeRunService.runStaticMethodCallTransaction(request);
    }
}