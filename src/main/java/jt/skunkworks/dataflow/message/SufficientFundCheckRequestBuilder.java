package jt.skunkworks.dataflow.message;

import jt.skunkworks.dataflow.mock.MockMessageBuilder;
import jt.skunkworks.fx.Order;

public class SufficientFundCheckRequestBuilder {
    private final Order order;

    public SufficientFundCheckRequestBuilder() {
        this(new MockMessageBuilder().buildOrder());
    }

    public SufficientFundCheckRequestBuilder(Order order) {
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
