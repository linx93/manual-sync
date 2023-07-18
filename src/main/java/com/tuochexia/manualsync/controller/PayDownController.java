package com.tuochexia.manualsync.controller;

import com.tuochexia.manualsync.service.IPayDown;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


/**
 * @author linx
 * @version 1.0.0
 * @date 2023-07-12 10:45:21
 * @describe 通过execl模板把向下付款的应收、应付同步到系统中
 */
@RestController
@RequestMapping(value = "/sync-data")
public class PayDownController {
    private final IPayDown iPayDown;

    public PayDownController(IPayDown iPayDown) {
        this.iPayDown = iPayDown;
    }


    @GetMapping("/pay-down")
    public Map<String, Object> syncPayDown(){
        Map<String, Object> result = new HashMap<>(8);
        long data = iPayDown.syncPayDown();
        result.put("code",20000);
        result.put("data",data);
        result.put("msg","success");
        return result;
    }

}
