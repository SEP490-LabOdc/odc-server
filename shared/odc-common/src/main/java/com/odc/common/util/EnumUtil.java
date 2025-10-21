package com.odc.common.util;

public class EnumUtil {

    public static boolean isEnumValueExist(String value, Class<? extends Enum<?>> enumClass) {
        if (value == null || enumClass == null) {
            return false;
        }

        String valueUpper = value.toUpperCase();

        for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.name().equalsIgnoreCase(valueUpper)) {
                return true;
            }
        }

        return false;
    }
}
