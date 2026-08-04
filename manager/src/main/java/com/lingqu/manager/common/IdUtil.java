package com.lingqu.manager.common;

import java.util.UUID;

/**
 * ID 生成：32 位无横线 UUID 字符串。
 */
public final class IdUtil {

    private IdUtil() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
