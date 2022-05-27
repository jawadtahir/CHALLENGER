package de.tum.i13.datasets.airquality;

import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateConfig {

    private final LocalDate from;
    private final LocalDate to;
    private final DateTimeFormatter dateTimeFormatter;

    public DateConfig(LocalDate from, LocalDate to, DateTimeFormatter dateTimeFormatter) {

        this.from = from;
        this.to = to;
        this.dateTimeFormatter = dateTimeFormatter;
    }

    public boolean validFor(LocalDate d) {
        if(this.from.isEqual(d) || this.to.isEqual(d))
            return true;

        if(this.from.isBefore(d) && this.to.isAfter(d))
            return true;

        return false;
    }

    public LocalDateTime parse(String date) {
        if(this.dateTimeFormatter == null) {
            Instant instant = Instant.ofEpochMilli(Long.parseLong(date));
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        } else {
            return LocalDateTime.parse(date, this.dateTimeFormatter);
        }
    }

    public Timestamp asTimestamp(LocalDateTime dt) {
        return Timestamp.newBuilder().setSeconds(dt.toEpochSecond(ZoneOffset.UTC)).setNanos(dt.getNano()).build();
    }

    @Override
    public String toString() {
        return "DateConfig{" +
                "from=" + from +
                ", to=" + to +
                ", dateTimeFormatter=" + dateTimeFormatter +
                '}';
    }
}
