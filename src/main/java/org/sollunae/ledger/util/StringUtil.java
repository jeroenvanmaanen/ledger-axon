package org.sollunae.ledger.util;

import org.springframework.util.StringUtils;

public class StringUtil {

    private StringUtil() {}

    public static boolean isNotEmpty(String s) {
        return !StringUtils.isEmpty(s);
    }
}
