package com.heban.app.ui.crop;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** C04 添加农事记录（占位页，M2 阶段实现） */
public class AddRecordFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "C04 添加农事记录";
    }

    @Override
    protected String phase() {
        return "M2";
    }

    @Override
    protected boolean isTopLevel() {
        return false;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("关闭（C02）", R.id.cropDetailFragment),
                link("保存（C02）", R.id.cropDetailFragment)
        };
    }
}
