package com.tms.controller;

import com.tms.common.Results;
import com.tms.controller.vo.request.CreateOrderRequestVo;
import com.tms.service.CustomerOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(value = "/customerOrder", description = "用户订单API")
@RestController
@RequestMapping("customerOrder")
public class CustomerOrderController {
    @Autowired
    private CustomerOrderService customerOrderService;

    @ApiOperation(value = "创建订单", response = Results.class)
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public Results createOrder(@ApiParam(name = "创建订单参数", value = "传入json格式", required = true) @RequestBody CreateOrderRequestVo createOrderRequestVo) {
        String orderNo = customerOrderService.createCustomerOrder(createOrderRequestVo);
        return Results.setSuccessMessage(orderNo);
    }

    @ApiOperation(value = "取消订单", response = Results.class)
    @RequestMapping(value = "/cancel/{orderDetailNo}", method = RequestMethod.PUT)
    public Results cancelOrder(@ApiParam(name = "订单号", required = true) @PathVariable String orderDetailNo) {
        boolean result = customerOrderService.cancelCustomerOrderDetail(orderDetailNo);
        return Results.setSuccessMessage(result);
    }
}
