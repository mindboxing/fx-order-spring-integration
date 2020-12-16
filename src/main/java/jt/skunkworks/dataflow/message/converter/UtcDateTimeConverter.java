package jt.skunkworks.dataflow.message.converter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static java.util.Optional.ofNullable;

public class UtcDateTimeConverter {

    private static final DateTimeFormatter TO_STRING = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeFormatter TO_OBJECT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    public static String print(ZonedDateTime dateTime) {
        return ofNullable(dateTime)
                .map(s -> dateTime.withZoneSameInstant(ZoneId.of("UTC"))
                        .truncatedTo(ChronoUnit.SECONDS)
                        .format(TO_STRING))
                .orElse(null);
    }

    public static ZonedDateTime parse(String dateTime) {
        return ofNullable(dateTime)
                .map(s -> s.endsWith("Z") ? s.replace("Z", "+0000") : s)
                .map(s -> ZonedDateTime.parse(s, TO_OBJECT))
                .orElse(null);
    }

}
