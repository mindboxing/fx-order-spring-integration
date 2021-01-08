package jt.skunkworks.dataflow.message.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private String aggregateId;
    private EventType type;

    public static  enum EventType {
        ORDER_ACCEPTED,
        ORDER_REJECTED
    }
}

