package com.heban.app.ui.crop;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** C03 添加作物（占位页，M2 阶段实现） */
public class AddCropFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "C03 添加作物";
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
                link("关闭（C01）", R.id.cropsFragment),
                link("保存（C01）", R.id.cropsFragment)
        };
    }
}
