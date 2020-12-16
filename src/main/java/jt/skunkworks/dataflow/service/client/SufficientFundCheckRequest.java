package jt.skunkworks.dataflow.service.client;

import jt.skunkworks.fx.Position;
import jt.skunkworks.fx.Type;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@NoArgsConstructor
public class SufficientFundCheckRequest {
    private String ccyPair;
    private BigInteger amount;
    private Position position;
    private Type type;
    private BigDecimal price;
}
