package com.heban.app.ui.community;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** S01 农友圈（占位页，M6 阶段实现） */
public class CommunityFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "S01 农友圈";
    }

    @Override
    protected String phase() {
        return "M6";
    }

    @Override
    protected boolean isTopLevel() {
        return true;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("消息，3 条未读（S08）", R.id.messagesFragment),
                link("问答（S04）", R.id.qaFragment),
                link("草莓姐姐的主页（S06）", R.id.userProfileFragment),
                link("草莓姐姐（S06）", R.id.userProfileFragment),
                link("今年的红颜定植完成啦！棚里铺了银灰色…（S02）", R.id.postDetailFragment),
                link("查看图片（S02）", R.id.postDetailFragment),
                link("评论（S02）", R.id.postDetailFragment),
                link("王农艺师的主页（S06）", R.id.userProfileFragment),
                link("王农艺师（S06）", R.id.userProfileFragment),
                link("霜降前后大棚管理三件事：① 夜间加盖…（S02）", R.id.postDetailFragment),
                link("果园阿强的主页（S06）", R.id.userProfileFragment),
                link("果园阿强（S06）", R.id.userProfileFragment),
                link("梨树秋施基肥开始了，每棵树 斤羊粪加…（S02）", R.id.postDetailFragment),
                link("发布（S03）", R.id.publishFragment)
        };
    }
}
