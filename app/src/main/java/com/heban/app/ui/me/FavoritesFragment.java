package com.heban.app.ui.me;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** M04 我的收藏（占位页，M7 阶段实现） */
public class FavoritesFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "M04 我的收藏";
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
                link("返回（M01）", R.id.profileFragment),
                link("霜降前后大棚管理三件事：夜间保温、中…（S02）", R.id.postDetailFragment),
                link("黄瓜霜霉病：症状识别与防治方法 百科…（D05）", R.id.pestDetailFragment),
                link("句容红颜草莓 当天现摘 商品 ¥25…（D08）", R.id.productFragment),
                link("黄瓜花多果少，怎么提高坐果率（S05）", R.id.questionFragment),
                link("今年的红颜定植完成啦！分享下定植密度…（S02）", R.id.postDetailFragment)
        };
    }
}
