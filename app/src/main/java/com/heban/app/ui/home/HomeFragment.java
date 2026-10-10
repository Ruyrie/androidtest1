package com.heban.app.ui.home;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** H01 首页（占位页，M3 阶段实现） */
public class HomeFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "H01 首页";
    }

    @Override
    protected String phase() {
        return "M3";
    }

    @Override
    protected boolean isTopLevel() {
        return true;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("消息，3 条未读（S08）", R.id.messagesFragment),
                link("今日天气，查看详情（H02）", R.id.weatherFragment),
                link("查看（H02）", R.id.weatherFragment),
                link("拍照识别（D02）", R.id.scan_graph),
                link("农事日历（H03）", R.id.calendarFragment),
                link("记一笔（D11）", R.id.addEntryFragment),
                link("签到 +10（H05）", R.id.checkInFragment),
                link("全部（H03）", R.id.calendarFragment),
                link("番茄 结果期 · 第 天（C02）", R.id.cropDetailFragment),
                link("黄瓜 开花期 · 第 天（C02）", R.id.cropDetailFragment),
                link("冬小麦 分蘖期 · 第 天（C02）", R.id.cropDetailFragment),
                link("问 AI（D06）", R.id.aiChatFragment)
        };
    }
}
