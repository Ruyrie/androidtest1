package com.heban.app.ui.discover.scan;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** D03 识别结果（占位页，M4 阶段实现） */
public class ScanResultFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "D03 识别结果";
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
                link("返回（D02）", R.id.scanFragment),
                link("重新拍摄（D02）", R.id.scanFragment),
                link("查看防治方法（D05）", R.id.pestDetailFragment),
                link("问 AI（D06）", R.id.aiChatFragment),
                link("黄瓜细菌性角斑病 6%（D05）", R.id.pestDetailFragment),
                link("缺镁症（生理性） 2%（D05）", R.id.pestDetailFragment),
                link("发到问答（S03）", R.id.publishFragment),
                link("保存到「黄瓜 · 2号大棚」记录（C04）", R.id.addRecordFragment)
        };
    }
}
