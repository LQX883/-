package edu.chenjin.library.util;

import java.util.UUID;

/**
 * 主键工具：生成 36 位 UUID 字符串。
 */
public final class IdUtil {

    private IdUtil() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }
}
