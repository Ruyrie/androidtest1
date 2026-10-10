package com.heban.app.ui.discover.pest;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** D04 病虫害百科（占位页，M5 阶段实现） */
public class PestsFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "D04 病虫害百科";
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
                link("本季高发 霜霉病 黄瓜、白菜等 · …（D05）", R.id.pestDetailFragment),
                link("黄瓜霜霉病 黄瓜 · 卵菌病害（D05）", R.id.pestDetailFragment),
                link("番茄灰霉病 番茄 · 真菌病害（D05）", R.id.pestDetailFragment),
                link("白粉病 瓜类、草莓 · 真菌病害（D05）", R.id.pestDetailFragment),
                link("蚜虫 多种蔬菜 · 虫害（D05）", R.id.pestDetailFragment),
                link("红蜘蛛（叶螨） 草莓、茄果 · 虫害（D05）", R.id.pestDetailFragment),
                link("番茄晚疫病 番茄、马铃薯 · 卵菌病害（D05）", R.id.pestDetailFragment)
        };
    }
}
