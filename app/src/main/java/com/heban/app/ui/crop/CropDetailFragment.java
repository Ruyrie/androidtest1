package com.heban.app.ui.crop;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** C02 作物详情（占位页，M2 阶段实现） */
public class CropDetailFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "C02 作物详情";
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
                link("返回（C01）", R.id.cropsFragment),
                link("分享到农友圈（S03）", R.id.publishFragment),
                link("编辑作物（C03）", R.id.addCropFragment),
                link("问 AI 诊断（D06）", R.id.aiChatFragment),
                link("添加记录（C04）", R.id.addRecordFragment)
        };
    }
}
