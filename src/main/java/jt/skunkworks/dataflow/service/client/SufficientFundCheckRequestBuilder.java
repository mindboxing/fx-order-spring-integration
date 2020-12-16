package jt.skunkworks.dataflow.service.client;

import jt.skunkworks.dataflow.message.FxOrder;

public class SufficientFundCheckRequestBuilder {
    private FxOrder order;

    public SufficientFundCheckRequestBuilder(FxOrder order) {
        this.order = order;
    }

    public SufficientFundCheckRequest build() {
        var request = new SufficientFundCheckRequest();
        request.setCcyPair(order.getCcyPair());
        request.setAmount(order.getAmount());
        request.setPosition(order.getPosition());
        request.setPrice(order.getPrice());
        request.setType(order.getType());
        return request;
    }
}
