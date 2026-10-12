package com.heban.app.ui.discover.scan;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.heban.app.HeBanApp;
import com.heban.app.R;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.ScanOutcome;
import com.heban.app.data.model.ScanResult;
import com.heban.app.databinding.FragmentScanResultBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;
import com.heban.app.util.ImagePaths;

import java.util.List;

/**
 * D03 识别结果（原型 ScanResult.dc.html，技术准备文档 9.5 / 表 9-3）：与 D02 共用 ScanViewModel。
 * 识别中显示照片与「正在识别…」；成功显示结论、严重程度、匹配度、特征标签、症状说明、其他可能、发到问答、
 * 保存到作物记录和识别服务名；失败 / 照片不可用提供重试、重拍。带 NavArgs.SCAN_RECORD_ID 打开时显示保存的结果。
 */
public class ScanResultFragment extends BaseFragment {
    /** S03 发布页参数（NavArgs 常量后续统一补充）：发布类型、预填正文 */
    private static final String ARG_PUBLISH_TYPE = "publish_type";
    private static final String ARG_PUBLISH_TEXT = "publish_text";
    private static final String PUBLISH_TYPE_QUESTION = "QUESTION";

    private FragmentScanResultBinding binding;
    private ScanViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentScanResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(nav().getBackStackEntry(R.id.scan_graph), HeBanApp.factory(requireContext()))
                .get(ScanViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());
        binding.btnRefresh.setOnClickListener(v -> vm.retry());
        binding.btnRetake.setOnClickListener(v -> retake());
        binding.btnTreatment.setOnClickListener(v -> openPest(vm.pestId()));
        binding.btnAskAi.setOnClickListener(v -> askAi());
        binding.btnPublish.setOnClickListener(v -> publish());
        binding.btnSave.setOnClickListener(v -> save());

        vm.phase.observe(getViewLifecycleOwner(), p -> {
            renderProblem();
            renderProvider();
        });
        vm.errorMessage.observe(getViewLifecycleOwner(), m -> renderProblem());
        vm.photo.observe(getViewLifecycleOwner(), b -> renderPhoto());
        vm.outcome.observe(getViewLifecycleOwner(), o -> {
            renderPhoto();
            renderOutcome(o);
            renderProblem();
            renderProvider();
        });
        vm.matchedCrops.observe(getViewLifecycleOwner(), m -> renderSave());
        vm.saved.observe(getViewLifecycleOwner(), s -> renderSave());
        vm.saving.observe(getViewLifecycleOwner(), s -> renderSave());
        vm.savedTo.observe(getViewLifecycleOwner(), label ->
                Snackbars.show(binding.getRoot(), getString(R.string.scan_result_saved_to, label)));
        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(binding.getRoot(), msg));

        long recordId = getArguments() == null ? 0 : getArguments().getLong(NavArgs.SCAN_RECORD_ID, 0);
        if (recordId > 0) {
            vm.openRecord(recordId);
        } else if (!vm.hasResult()) {
            // 进程被回收后照片已丢失：回到拍照页
            view.post(this::retake);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ———————————————— 渲染 ————————————————

    private void renderPhoto() {
        Bitmap bmp = vm.photo.getValue();
        if (bmp != null) {
            Glide.with(binding.photo).clear(binding.photo);
            binding.photo.setImageBitmap(bmp);
            return;
        }
        ScanOutcome o = vm.outcome.getValue();
        String path = o == null ? null : o.imagePath;
        if (path == null || path.isEmpty()) {
            Glide.with(binding.photo).clear(binding.photo);
            binding.photo.setImageDrawable(null);
            return;
        }
        Glide.with(binding.photo)
                .load(ImagePaths.toUri(path))
                .placeholder(R.drawable.ph_image)
                .error(R.drawable.ph_image)
                .centerCrop()
                .into(binding.photo);
    }

    private void renderProblem() {
        ScanViewModel.Phase p = vm.phase.getValue();
        if (p == ScanViewModel.Phase.ERROR) {
            setProblem(R.drawable.ic_error, com.google.android.material.R.attr.colorErrorContainer,
                    com.google.android.material.R.attr.colorOnErrorContainer,
                    getString(R.string.scan_result_failed), vm.errorMessage.getValue());
        } else if (p == ScanViewModel.Phase.IMAGE_BAD) {
            ScanOutcome o = vm.outcome.getValue();
            String issue = o == null || o.result == null ? null : o.result.imageIssue;
            if (issue == null || issue.trim().isEmpty()) issue = getString(R.string.scan_result_image_bad_default);
            setProblem(R.drawable.ic_image, R.attr.colorWarningContainer, R.attr.colorOnWarningContainer,
                    getString(R.string.scan_result_image_bad), issue);
        }
    }

    private void setProblem(int icon, @AttrRes int bg, @AttrRes int fg, String title, @Nullable String message) {
        binding.problemIconBg.setBackgroundTintList(ColorStateList.valueOf(color(bg)));
        binding.problemIcon.setImageResource(icon);
        binding.problemIcon.setImageTintList(ColorStateList.valueOf(color(fg)));
        binding.problemTitle.setText(title);
        binding.problemMessage.setText(message);
        binding.problemMessage.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void renderProvider() {
        ScanOutcome o = vm.outcome.getValue();
        ScanViewModel.Phase p = vm.phase.getValue();
        boolean show = o != null && o.provider != null && !o.provider.trim().isEmpty()
                && (p == ScanViewModel.Phase.SUCCESS || p == ScanViewModel.Phase.IMAGE_BAD);
        binding.tvProvider.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.tvProvider.setText(show ? getString(R.string.scan_result_provider, o.provider.trim()) : null);
    }

    private void renderOutcome(@Nullable ScanOutcome o) {
        ScanResult.Top top = o == null || o.result == null ? null : o.result.top;
        if (top == null) return;
        binding.tvName.setText(top.name);

        ScanFormat.Severity severity = ScanFormat.severity(top);
        int[] colors = ScanFormat.severityColors(severity);
        binding.severityChip.setVisibility(colors == null ? View.GONE : View.VISIBLE);
        if (colors != null) {
            int fg = color(colors[1]);
            binding.severityChip.setBackgroundTintList(ColorStateList.valueOf(color(colors[0])));
            binding.severityIcon.setImageResource(severity == ScanFormat.Severity.HEALTHY
                    ? R.drawable.ic_check_circle : R.drawable.ic_warning);
            binding.severityIcon.setImageTintList(ColorStateList.valueOf(fg));
            binding.tvSeverity.setText(ScanFormat.severityLabel(top));
            binding.tvSeverity.setTextColor(fg);
        }

        int percent = ScanFormat.percent(top.confidence);
        binding.matchBar.setProgress(percent);
        binding.tvMatch.setText(getString(R.string.scan_result_match, percent));

        renderTags(ScanFormat.tags(top, getString(R.string.scan_result_spread_fast)));
        String evidence = ScanFormat.evidenceText(top);
        binding.tvEvidence.setText(evidence);
        binding.tvEvidence.setVisibility(evidence.isEmpty() ? View.GONE : View.VISIBLE);

        // 健康植株不需要防治方法；其余按是否匹配到本地百科决定能否点击
        boolean healthy = severity == ScanFormat.Severity.HEALTHY;
        binding.btnTreatment.setVisibility(healthy ? View.GONE : View.VISIBLE);
        binding.btnTreatment.setEnabled(o.pestId != null);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) binding.btnAskAi.getLayoutParams();
        lp.setMarginStart(healthy ? 0 : dp(8));
        binding.btnAskAi.setLayoutParams(lp);

        renderOthers(o.result.others);
    }

    private void renderTags(List<String> tags) {
        binding.tags.removeAllViews();
        int h = dp(24);
        for (String t : tags) {
            TextView tv = new TextView(requireContext(), null, 0, R.style.Widget_HeBan_Scan_Tag);
            tv.setText(t);
            binding.tags.addView(tv, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, h));
        }
        binding.tags.setVisibility(tags.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void renderOthers(@Nullable List<ScanResult.Other> others) {
        binding.othersList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(binding.othersList.getContext());
        int count = 0;
        if (others != null) {
            for (ScanResult.Other other : others) {
                if (other == null || other.name == null || other.name.trim().isEmpty()) continue;
                View item = inflater.inflate(R.layout.scan_item_other, binding.othersList, false);
                item.findViewById(R.id.divider).setVisibility(count == 0 ? View.GONE : View.VISIBLE);
                ((TextView) item.findViewById(R.id.tv_name)).setText(other.name);
                ((LinearProgressIndicator) item.findViewById(R.id.bar))
                        .setProgress(ScanFormat.otherBarProgress(other.confidence));
                ((TextView) item.findViewById(R.id.tv_percent)).setText(
                        getString(R.string.scan_result_percent, ScanFormat.percent(other.confidence)));
                String name = other.name;
                item.findViewById(R.id.row).setOnClickListener(v -> openOther(name));
                binding.othersList.addView(item);
                count++;
            }
        }
        binding.othersSection.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
    }

    private void renderSave() {
        boolean saved = Boolean.TRUE.equals(vm.saved.getValue());
        boolean saving = Boolean.TRUE.equals(vm.saving.getValue());
        if (saved) {
            binding.btnSave.setText(R.string.scan_result_saved);
            binding.btnSave.setIconResource(R.drawable.ic_bookmark_fill);
        } else {
            CropCard target = vm.saveTarget();
            binding.btnSave.setText(target == null ? getString(R.string.scan_result_save_to_crop)
                    : getString(R.string.scan_result_save_to, ScanFormat.cropLabel(target)));
            binding.btnSave.setIconResource(R.drawable.ic_bookmark_add);
        }
        binding.btnSave.setEnabled(!saved && !saving);
    }

    // ———————————————— 操作 ————————————————

    /** 重拍：回到 D02 */
    private void retake() {
        if (binding == null) return;
        if (!nav().popBackStack(R.id.scanFragment, false)) nav().navigate(R.id.scanFragment);
    }

    private void openPest(@Nullable Long pestId) {
        if (pestId == null) return;
        Bundle b = new Bundle();
        b.putLong(NavArgs.PEST_ID, pestId);
        nav().navigate(R.id.pestDetailFragment, b);
    }

    /** 其他可能：匹配到本地百科时打开详情，否则打开百科列表 */
    private void openOther(String name) {
        Long id = vm.otherPestId(name);
        if (id != null) openPest(id);
        else nav().navigate(R.id.pestsFragment);
    }

    private void askAi() {
        if (!LoginRequired.check(this)) return;
        Bundle b = new Bundle();
        b.putString(NavArgs.AI_PROMPT, vm.aiPrompt());
        nav().navigate(R.id.aiChatFragment, b);
    }

    private void publish() {
        if (!LoginRequired.check(this)) return;
        Bundle b = new Bundle();
        b.putString(ARG_PUBLISH_TYPE, PUBLISH_TYPE_QUESTION);
        b.putString(ARG_PUBLISH_TEXT, vm.publishText());
        nav().navigate(R.id.publishFragment, b);
    }

    private void save() {
        if (!LoginRequired.check(this)) return;
        if (Boolean.TRUE.equals(vm.saved.getValue())) return;
        List<CropCard> crops = vm.crops.getValue();
        if (crops == null || crops.isEmpty()) {
            Snackbars.show(binding.getRoot(), getString(R.string.scan_result_no_crops),
                    getString(R.string.scan_result_add_crop), v -> nav().navigate(R.id.addCropFragment));
            return;
        }
        List<CropCard> matched = vm.matchingCrops();
        if (matched.size() == 1) {
            vm.saveToCrop(matched.get(0));
        } else {
            ScanCropPicker.show(requireContext(), crops, matched, vm::saveToCrop);
        }
    }

    private int color(@AttrRes int attr) {
        return MaterialColors.getColor(binding.getRoot(), attr);
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }
}
