package jt.skunkworks.dataflow.message.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

class UtcDateTimeConverterTest {

    @Test
    void testConversion() {
        ZonedDateTime input = ZonedDateTime.now();
        String converted = UtcDateTimeConverter.print(input);
        ZonedDateTime parsed = UtcDateTimeConverter.parse(converted);

        Assertions.assertEquals(
                input.withZoneSameInstant(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS),
                parsed.withZoneSameInstant(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS)
        );
    }

    @Test
    void testNullSafety() {
        Assertions.assertDoesNotThrow(() -> {
            UtcDateTimeConverter.print(null);
            UtcDateTimeConverter.parse(null);
        });
    }

}