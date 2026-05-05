package io.github.arthurhoch.kissjson.internal;

import io.github.arthurhoch.kissjson.DateFormat;
import io.github.arthurhoch.kissjson.JsonException;
import io.github.arthurhoch.kissjson.JsonMappingException;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

final class DateCodec {

    private static final DateTimeFormatter LOCAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter LOCAL_TIME = DateTimeFormatter.ISO_LOCAL_TIME;
    private static final DateTimeFormatter LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter OFFSET_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter ZONED_DATE_TIME = DateTimeFormatter.ISO_ZONED_DATE_TIME;
    private static final DateTimeFormatter INSTANT_FMT = DateTimeFormatter.ISO_INSTANT;

    private static final ConcurrentHashMap<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    private DateCodec() {
    }

    private static DateTimeFormatter getCachedFormatter(String pattern) {
        return FORMATTER_CACHE.computeIfAbsent(pattern, DateTimeFormatter::ofPattern);
    }

    static String serialize(Object value, DateFormat format, ZoneId zoneId, String pattern) {
        if (value == null) return null;

        try {
            if (pattern != null && !pattern.isEmpty()) {
                return serializeWithPattern(value, pattern);
            }

            if (format == DateFormat.EPOCH_MILLIS) {
                return serializeEpochMillis(value, zoneId);
            }
            if (format == DateFormat.EPOCH_SECONDS) {
                return serializeEpochSeconds(value, zoneId);
            }

            return serializeISO(value, zoneId);
        } catch (JsonException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonException("Failed to serialize date/time value of type " + value.getClass().getName() + ": " + e.getMessage(), e);
        }
    }

    static Object deserialize(Object value, Class<?> targetType, DateFormat format, ZoneId zoneId, String pattern) {
        if (value == null) return null;

        try {
            if (pattern != null && !pattern.isEmpty()) {
                return deserializeWithPattern(value, targetType, pattern, zoneId);
            }

            if (value instanceof Number) {
                if (!isEpochTarget(targetType) || format == DateFormat.ISO) {
                    throw new JsonMappingException(
                            "Expected ISO string for " + targetType.getName(),
                            null, targetType, null, targetType, value
                    );
                }
                long epoch = ((Number) value).longValue();
                if (format == DateFormat.EPOCH_SECONDS) {
                    epoch = epoch * 1000;
                }
                return fromEpoch(epoch, targetType, zoneId);
            }

            String str = value.toString();

            if (targetType == Duration.class) return Duration.parse(str);
            if (targetType == Period.class) return Period.parse(str);

            if (isEpochTarget(targetType)
                    && (format == DateFormat.EPOCH_MILLIS || format == DateFormat.EPOCH_SECONDS)) {
                try {
                    long epoch = Long.parseLong(str);
                    if (format == DateFormat.EPOCH_SECONDS) epoch *= 1000;
                    return fromEpoch(epoch, targetType, zoneId);
                } catch (NumberFormatException ignored) {
                }
            }

            return deserializeISO(str, targetType, zoneId);
        } catch (JsonException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonMappingException(
                    "Failed to deserialize value to " + targetType.getName() + ": " + e.getMessage(),
                    null, targetType, null, null, value
            );
        }
    }

    private static String serializeISO(Object value, ZoneId zoneId) {
        if (value instanceof LocalDate ld) return ld.toString();
        if (value instanceof Instant i) return i.toString();
        if (value instanceof LocalDateTime ldt) return ldt.format(LOCAL_DATE_TIME);
        if (value instanceof LocalTime lt) return lt.format(LOCAL_TIME);
        if (value instanceof OffsetDateTime odt) return odt.format(OFFSET_DATE_TIME);
        if (value instanceof ZonedDateTime zdt) return zdt.format(ZONED_DATE_TIME);
        if (value instanceof Duration d) return d.toString();
        if (value instanceof Period p) return p.toString();
        if (value instanceof Date) {
            Instant instant = ((Date) value).toInstant();
            return instant.atZone(zoneId).format(ZONED_DATE_TIME);
        }
        if (value instanceof Calendar) {
            return ((Calendar) value).toInstant().atZone(zoneId).format(ZONED_DATE_TIME);
        }
        throw new JsonException("Unsupported date/time type: " + value.getClass().getName());
    }

    private static String serializeEpochMillis(Object value, ZoneId zoneId) {
        if (value instanceof LocalDate ld) return ld.format(LOCAL_DATE);
        if (value instanceof LocalTime lt) return lt.format(LOCAL_TIME);
        if (value instanceof LocalDateTime ldt) return ldt.format(LOCAL_DATE_TIME);
        if (value instanceof Instant i) return String.valueOf(i.toEpochMilli());
        if (value instanceof OffsetDateTime odt) return String.valueOf(odt.toInstant().toEpochMilli());
        if (value instanceof ZonedDateTime zdt) return String.valueOf(zdt.toInstant().toEpochMilli());
        if (value instanceof Date d) return String.valueOf(d.getTime());
        if (value instanceof Calendar c) return String.valueOf(c.getTimeInMillis());
        if (value instanceof Duration d) return d.toString();
        if (value instanceof Period p) return p.toString();
        throw new JsonException("Unsupported date/time type: " + value.getClass().getName());
    }

    private static String serializeEpochSeconds(Object value, ZoneId zoneId) {
        if (value instanceof LocalDate ld) return ld.format(LOCAL_DATE);
        if (value instanceof LocalTime lt) return lt.format(LOCAL_TIME);
        if (value instanceof LocalDateTime ldt) return ldt.format(LOCAL_DATE_TIME);
        if (value instanceof Instant i) return String.valueOf(i.getEpochSecond());
        if (value instanceof OffsetDateTime odt) return String.valueOf(odt.toInstant().getEpochSecond());
        if (value instanceof ZonedDateTime zdt) return String.valueOf(zdt.toInstant().getEpochSecond());
        if (value instanceof Date d) return String.valueOf(d.getTime() / 1000);
        if (value instanceof Calendar c) return String.valueOf(c.getTimeInMillis() / 1000);
        if (value instanceof Duration d) return d.toString();
        if (value instanceof Period p) return p.toString();
        throw new JsonException("Unsupported date/time type: " + value.getClass().getName());
    }

    private static String serializeWithPattern(Object value, String pattern) {
        DateTimeFormatter fmt = getCachedFormatter(pattern);
        if (value instanceof LocalDate) return ((LocalDate) value).format(fmt);
        if (value instanceof LocalTime) return ((LocalTime) value).format(fmt);
        if (value instanceof LocalDateTime) return ((LocalDateTime) value).format(fmt);
        if (value instanceof OffsetDateTime) return ((OffsetDateTime) value).format(fmt);
        if (value instanceof ZonedDateTime) return ((ZonedDateTime) value).format(fmt);
        if (value instanceof Instant) return ((Instant) value).atZone(ZoneOffset.UTC).format(fmt);
        if (value instanceof Date) return ((Date) value).toInstant().atZone(ZoneOffset.UTC).format(fmt);
        if (value instanceof Calendar) return ((Calendar) value).toInstant().atZone(ZoneOffset.UTC).format(fmt);
        if (value instanceof Duration) return ((Duration) value).toString();
        if (value instanceof Period) return ((Period) value).toString();
        throw new JsonException("Unsupported date/time type: " + value.getClass().getName());
    }

    private static Object deserializeISO(String str, Class<?> targetType, ZoneId zoneId) {
        try {
            if (targetType == LocalDate.class) return LocalDate.parse(str, LOCAL_DATE);
            if (targetType == LocalTime.class) return LocalTime.parse(str, LOCAL_TIME);
            if (targetType == LocalDateTime.class) return LocalDateTime.parse(str, LOCAL_DATE_TIME);
            if (targetType == OffsetDateTime.class) return OffsetDateTime.parse(str, OFFSET_DATE_TIME);
            if (targetType == ZonedDateTime.class) return ZonedDateTime.parse(str, ZONED_DATE_TIME);
            if (targetType == Instant.class) return Instant.parse(str);
            if (targetType == Duration.class) return Duration.parse(str);
            if (targetType == Period.class) return Period.parse(str);
            if (targetType == Date.class) {
                Instant instant = parseInstant(str, zoneId);
                return Date.from(instant);
            }
            if (targetType == Calendar.class) {
                Instant instant = parseInstant(str, zoneId);
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTimeZone(TimeZone.getTimeZone(zoneId));
                cal.setTimeInMillis(instant.toEpochMilli());
                return cal;
            }
        } catch (DateTimeParseException e) {
            throw new JsonMappingException(
                    "Cannot parse date/time value '" + str + "' as " + targetType.getName() + ": " + e.getMessage(),
                    null, targetType, null, null, str
            );
        }
        throw new JsonMappingException("Unsupported date/time type: " + targetType.getName(), null, targetType, null, null, str);
    }

    private static Instant parseInstant(String str, ZoneId zoneId) {
        try {
            return Instant.parse(str);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(str, LOCAL_DATE_TIME).atZone(zoneId).toInstant();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(str, OFFSET_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return ZonedDateTime.parse(str, ZONED_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
        }
        throw new DateTimeParseException("Cannot parse as date/time", str, 0);
    }

    private static Object fromEpoch(long epochMillis, Class<?> targetType, ZoneId zoneId) {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        if (targetType == Instant.class) return instant;
        if (targetType == Date.class) return Date.from(instant);
        if (targetType == Calendar.class) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeZone(TimeZone.getTimeZone(zoneId));
            cal.setTimeInMillis(epochMillis);
            return cal;
        }
        if (targetType == OffsetDateTime.class) return instant.atOffset(ZoneOffset.UTC);
        if (targetType == ZonedDateTime.class) return instant.atZone(zoneId);
        if (targetType == LocalDateTime.class) return LocalDateTime.ofInstant(instant, zoneId);
        if (targetType == LocalDate.class) return LocalDateTime.ofInstant(instant, zoneId).toLocalDate();
        if (targetType == LocalTime.class) return LocalDateTime.ofInstant(instant, zoneId).toLocalTime();
        throw new JsonMappingException("Cannot convert epoch to " + targetType.getName(), null, targetType, null, null, epochMillis);
    }

    private static Object deserializeWithPattern(Object value, Class<?> targetType, String pattern, ZoneId zoneId) {
        DateTimeFormatter fmt = getCachedFormatter(pattern);
        String str = value.toString();

        try {
            if (targetType == LocalDate.class) return LocalDate.parse(str, fmt);
            if (targetType == LocalTime.class) return LocalTime.parse(str, fmt);
            if (targetType == LocalDateTime.class) return LocalDateTime.parse(str, fmt);
            if (targetType == OffsetDateTime.class) return OffsetDateTime.parse(str, fmt);
            if (targetType == ZonedDateTime.class) return ZonedDateTime.parse(str, fmt);
            if (targetType == Instant.class) return LocalDateTime.parse(str, fmt).atZone(zoneId).toInstant();
            if (targetType == Date.class) return Date.from(LocalDateTime.parse(str, fmt).atZone(zoneId).toInstant());
            if (targetType == Calendar.class) {
                Instant instant = LocalDateTime.parse(str, fmt).atZone(zoneId).toInstant();
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTimeZone(TimeZone.getTimeZone(zoneId));
                cal.setTimeInMillis(instant.toEpochMilli());
                return cal;
            }
            if (targetType == Duration.class) return Duration.parse(str);
            if (targetType == Period.class) return Period.parse(str);
        } catch (DateTimeParseException e) {
            throw new JsonMappingException(
                    "Cannot parse '" + str + "' with pattern '" + pattern + "' as " + targetType.getName(),
                    null, targetType, null, null, str
            );
        }
        throw new JsonMappingException("Unsupported date/time type: " + targetType.getName(), null, targetType, null, null, str);
    }

    static boolean isDateType(Class<?> type) {
        return type == LocalDate.class
                || type == LocalTime.class
                || type == LocalDateTime.class
                || type == OffsetDateTime.class
                || type == ZonedDateTime.class
                || type == Instant.class
                || type == Duration.class
                || type == Period.class
                || type == Date.class
                || Calendar.class.isAssignableFrom(type);
    }

    private static boolean isEpochTarget(Class<?> targetType) {
        return targetType == Instant.class
                || targetType == OffsetDateTime.class
                || targetType == ZonedDateTime.class
                || targetType == Date.class
                || Calendar.class.isAssignableFrom(targetType);
    }
}
