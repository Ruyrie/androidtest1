package com.heban.app.ui.common;

/**
 * 「再按一次返回键退出」：第一次按返回只提示，在 windowMs 内再按一次才退出，防止误触回到桌面。
 * 只在返回键会离开 App 时使用（导航返回栈为空）。
 */
public final class BackPressExitHelper {
    private final long windowMs;
    private long lastPressAt = Long.MIN_VALUE;

    public BackPressExitHelper(long windowMs) {
        this.windowMs = windowMs;
    }

    /** @return true 表示应当退出；false 表示应当提示「再按一次」 */
    public boolean onBackPressed(long nowMs) {
        boolean exit = lastPressAt != Long.MIN_VALUE && nowMs - lastPressAt <= windowMs;
        lastPressAt = exit ? Long.MIN_VALUE : nowMs;
        return exit;
    }
}
