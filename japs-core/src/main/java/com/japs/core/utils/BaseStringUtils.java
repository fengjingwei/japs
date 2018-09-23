package com.japs.core.utils;

public abstract class BaseStringUtils extends org.apache.commons.lang3.StringUtils {

    public static String join(CharSequence... elements) {
        return String.join("/", elements);
    }
}
