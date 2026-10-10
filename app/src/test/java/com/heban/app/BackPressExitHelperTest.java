package com.heban.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.heban.app.ui.common.BackPressExitHelper;

import org.junit.Test;

public class BackPressExitHelperTest {

    @Test
    public void firstPress_onlyHints() {
        BackPressExitHelper h = new BackPressExitHelper(2000);
        assertFalse(h.onBackPressed(10_000));
    }

    @Test
    public void secondPressWithinWindow_exits() {
        BackPressExitHelper h = new BackPressExitHelper(2000);
        h.onBackPressed(10_000);
        assertTrue(h.onBackPressed(11_500));
    }

    @Test
    public void secondPressAfterWindow_hintsAgain() {
        BackPressExitHelper h = new BackPressExitHelper(2000);
        h.onBackPressed(10_000);
        assertFalse(h.onBackPressed(12_500));
        assertTrue(h.onBackPressed(13_000));
    }
}
