package jt.skunkworks.dataflow.message;

import jt.skunkworks.fx.Position;
import jt.skunkworks.fx.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FxOrder {
    private UUID orderId;
    private ZonedDateTime create;
    private String ccyPair;
    private BigInteger amount;
    private Position position;
    private Type type;
    private BigDecimal price;
    private UUID accountId;
    private boolean funded;
}
