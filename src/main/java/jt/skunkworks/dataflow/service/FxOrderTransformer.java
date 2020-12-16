package jt.skunkworks.dataflow.service;

import jt.skunkworks.dataflow.message.FxOrder;
import jt.skunkworks.fx.Order;
import org.springframework.integration.transformer.GenericTransformer;

import java.util.UUID;

public class FxOrderTransformer implements GenericTransformer<Order, FxOrder> {
    @Override
    public FxOrder transform(Order order) {
        return FxOrder.builder()
                .amount(order.getAmount())
                .ccyPair(order.getCcyPair())
                .orderId(UUID.fromString(order.getOrdertId()))
                .position(order.getPosition())
                .price(order.getPrice())
                .type(order.getType())
                .accountId(UUID.fromString(order.getAccountId()))
                .build()
                ;
    }
}
