package com.lingqu.executor.common;

import java.util.UUID;

public final class IdUtil {

    private IdUtil() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
