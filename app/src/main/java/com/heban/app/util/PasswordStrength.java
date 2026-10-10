package com.heban.app.util;

import androidx.annotation.Nullable;

/** 密码规则与强度：8–20 位、同时包含字母和数字（A05 / A08 / M07）。 */
public final class PasswordStrength {
    public enum Level { NONE, WEAK, MEDIUM, STRONG }

    public static final class Result {
        public final boolean lengthOk;
        public final boolean hasLetter;
        public final boolean hasDigit;
        public final Level level;

        Result(boolean lengthOk, boolean hasLetter, boolean hasDigit, Level level) {
            this.lengthOk = lengthOk;
            this.hasLetter = hasLetter;
            this.hasDigit = hasDigit;
            this.level = level;
        }

        /** 满足全部规则才能提交 */
        public boolean valid() {
            return lengthOk && hasLetter && hasDigit;
        }

        /** 「强度：中」中的文字：未输入 / 弱 / 中 / 强 */
        public String label() {
            switch (level) {
                case WEAK: return "弱";
                case MEDIUM: return "中";
                case STRONG: return "强";
                default: return "未输入";
            }
        }

        /** 强度条进度 0–100 */
        public int progress() {
            switch (level) {
                case WEAK: return 33;
                case MEDIUM: return 66;
                case STRONG: return 100;
                default: return 0;
            }
        }
    }

    private PasswordStrength() {
    }

    public static Result evaluate(@Nullable String pwd) {
        if (pwd == null || pwd.isEmpty()) {
            return new Result(false, false, false, Level.NONE);
        }
        boolean lengthOk = pwd.length() >= 8 && pwd.length() <= 20;
        boolean hasLetter = pwd.matches(".*[A-Za-z].*");
        boolean hasDigit = pwd.matches(".*\\d.*");
        Level level;
        if (!(lengthOk && hasLetter && hasDigit)) {
            level = Level.WEAK;
        } else {
            boolean mixedCase = pwd.matches(".*[a-z].*") && pwd.matches(".*[A-Z].*");
            boolean symbol = pwd.matches(".*[^A-Za-z0-9].*");
            level = (pwd.length() >= 12 || mixedCase || symbol) ? Level.STRONG : Level.MEDIUM;
        }
        return new Result(lengthOk, hasLetter, hasDigit, level);
    }
}
