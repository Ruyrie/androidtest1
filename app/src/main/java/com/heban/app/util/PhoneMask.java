package com.heban.app.util;

import androidx.annotation.Nullable;

/** 手机号校验与脱敏：13800005678 → 138****5678（UI 设计文档；技术准备文档表 6-2）。 */
public final class PhoneMask {
    private PhoneMask() {
    }

    /** 中国大陆手机号：1 开头、第二位 3–9、共 11 位 */
    public static boolean isValid(@Nullable String phone) {
        return phone != null && phone.matches("1[3-9]\\d{9}");
    }

    public static String mask(@Nullable String phone) {
        if (phone == null || phone.length() != 11) return phone == null ? "" : phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
