package com.heban.app.ui.discover.ledger;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** D10 收支记账（占位页，M5 阶段实现） */
public class LedgerFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "D10 收支记账";
    }

    @Override
    protected String phase() {
        return "M5";
    }

    @Override
    protected boolean isTopLevel() {
        return false;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("返回（D01）", R.id.discoverFragment),
                link("记一笔（D11）", R.id.addEntryFragment)
        };
    }
}
