package com.zbkj.overseas.service.impl;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Row conversion for the paint-formula module, mirroring the Node
 * backend's formulaRowOut: entity -> API map with camelCase keys (the
 * Java field names already are), the id serialised as a string (the
 * source data carries snowflake ids beyond a JSON number's exact
 * range), the enable char flag as a boolean, dates formatted
 * 'yyyy-MM-dd HH:mm:ss', and audit columns (isDeleted / createUserId /
 * updateUserId) hidden.
 */
final class FormulaRowUtil {

    private FormulaRowUtil() {
    }

    static String fmt(Date date) {
        return date == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    static String flag(Boolean value) {
        return Boolean.TRUE.equals(value) ? "1" : "0";
    }

    static Map<String, Object> rowOut(Object entity) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Field f : entity.getClass().getDeclaredFields()) {
            String name = f.getName();
            if (name.equals("serialVersionUID") || name.equals("isDeleted")
                    || name.equals("createUserId") || name.equals("updateUserId")) {
                continue;
            }
            f.setAccessible(true);
            Object v;
            try {
                v = f.get(entity);
            } catch (IllegalAccessException e) {
                continue;
            }
            if (name.equals("id")) {
                out.put(name, v == null ? null : String.valueOf(v));
            } else if (name.equals("enable")) {
                out.put(name, v != null && ("1".equals(String.valueOf(v)) || "true".equalsIgnoreCase(String.valueOf(v))));
            } else if (v instanceof Date) {
                out.put(name, fmt((Date) v));
            } else {
                out.put(name, v);
            }
        }
        return out;
    }
}
