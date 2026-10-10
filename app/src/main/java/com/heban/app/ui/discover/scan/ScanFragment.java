package com.heban.app.ui.discover.scan;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** D02 拍照识别（占位页，M4 阶段实现） */
public class ScanFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "D02 拍照识别";
    }

    @Override
    protected String phase() {
        return "M4";
    }

    @Override
    protected boolean isTopLevel() {
        return false;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("关闭（D01）", R.id.discoverFragment),
                link("拍照（D03）", R.id.scanResultFragment)
        };
    }
}
