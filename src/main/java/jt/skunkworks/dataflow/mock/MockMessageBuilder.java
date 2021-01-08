package jt.skunkworks.dataflow.mock;

import jt.skunkworks.fx.Order;
import jt.skunkworks.fx.Position;
import jt.skunkworks.fx.Type;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.UUID;

import static javax.xml.bind.JAXB.marshal;
import static jt.skunkworks.fx.Position.BUY;
import static jt.skunkworks.fx.Type.LIMIT;

public class MockMessageBuilder {

    public final static String ACCOUNT_ID = "00000000-0000-0000-0000-000000000000";

    private BigInteger amount = new BigInteger("100000");;
    private String ccyPair = "USDJPY";
    private Position position = BUY;
    private BigDecimal price = new BigDecimal("1.121");
    private Type type = LIMIT;
    private String orderId = UUID.randomUUID().toString();
    private String accountId = ACCOUNT_ID;

    public MockMessageBuilder amount(BigInteger amount) {
        this.amount = amount;
        return this;
    }

    public MockMessageBuilder ccyPair(String ccyPair) {
        this.ccyPair = ccyPair;
        return this;
    }

    public MockMessageBuilder position(Position position) {
        this.position = position;
        return this;
    }

    public MockMessageBuilder price(BigDecimal price) {
        this.price = price;
        return this;
    }

    public MockMessageBuilder type(Type type) {
        this.type = type;
        return this;
    }

    public MockMessageBuilder accountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public String build() {
        var writer = new StringWriter();
        marshal(buildOrder(), writer);
        return writer.toString();
    }

    public Order buildOrder() {
        var orderDetail = new Order();
        orderDetail.setOrderId(orderId);
        orderDetail.setAmount(amount);
        orderDetail.setCcyPair(ccyPair);
        orderDetail.setPosition(position);
        orderDetail.setPrice(price);
        orderDetail.setType(type);
        orderDetail.setAccountId(accountId);
        orderDetail.setCreate(ZonedDateTime.now());
        orderDetail.setOrderId(orderId);
        return orderDetail;
    }

    public String getOrderId() {
        return orderId;
    }
}
