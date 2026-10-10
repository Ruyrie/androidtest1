package com.heban.app.ui.discover.ledger;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** D11 记一笔（占位页，M5 阶段实现） */
public class AddEntryFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "D11 记一笔";
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
                link("关闭（D10）", R.id.ledgerFragment),
                link("保存（D10）", R.id.ledgerFragment)
        };
    }
}
