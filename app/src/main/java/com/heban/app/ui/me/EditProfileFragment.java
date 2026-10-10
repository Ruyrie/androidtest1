package com.heban.app.ui.me;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** M03 编辑资料（占位页，M7 阶段实现） */
public class EditProfileFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "M03 编辑资料";
    }

    @Override
    protected String phase() {
        return "M7";
    }

    @Override
    protected boolean isTopLevel() {
        return false;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("关闭（M02）", R.id.myHomeFragment),
                link("保存（M02）", R.id.myHomeFragment)
        };
    }
}
