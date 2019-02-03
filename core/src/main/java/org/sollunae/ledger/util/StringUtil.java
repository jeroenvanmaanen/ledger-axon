package org.sollunae.ledger.util;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringUtil {

    private StringUtil() {}

    public static boolean isNotEmpty(String s) {
        return !StringUtils.isEmpty(s);
    }

    public static String asString(Object object) {
        return asStream(object).collect(Collectors.joining());
    }

    private static Stream<String> asStream(Object object) {
        if (object == null) {
            return Stream.of("null");
        } else if (object instanceof Stream) {
            return Stream.of(
                "[" + ((Stream<?>) object).map(String::valueOf).collect(Collectors.joining(", ")) + "]"
            );
        } else if (object instanceof Collection) {
            return asStream(((Collection<?>) object).stream());
        } else if (object.getClass().isArray()) {
            return asStream(Arrays.stream((Object[]) object));
        } else if (object instanceof Map) {
            return asStream(((Map<?,?>) object).entrySet());
        } else if (object instanceof Map.Entry<?,?>) {
            Map.Entry<?,?> entry = (Map.Entry<?,?>) object;
            return Stream.of(
                asStream(entry.getKey()),
                Stream.of(" -> "),
                asStream(entry.getValue())
            ).flatMap(Function.identity());
        } else {
            return Stream.of(String.valueOf(object));
        }
    }
}
