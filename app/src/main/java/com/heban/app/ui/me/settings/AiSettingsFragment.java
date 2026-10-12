package com.heban.app.ui.me.settings;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavOptions;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.heban.app.R;
import com.heban.app.databinding.FragmentAiSettingsBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;
import com.heban.app.ui.discover.ai.AiChatHistorySheet;

/**
 * M11 AI 助手设置（原型 AISettings.dc.html；「模型服务」区块按技术准备文档表 9-5 补充）。
 * 「清空全部对话」显示可撤销的 Snackbar，Snackbar 消失（未撤销）后才真正清空，离开页面时立即清空。
 */
public class AiSettingsFragment extends BaseFragment {
    private FragmentAiSettingsBinding binding;
    private AiSettingsViewModel vm;
    @Nullable private Snackbar undoBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(AiSettingsViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());

        setUpStyle();
        setUpSwitches();
        setUpHistory();
        setUpModelService();

        vm.toast.observe(getViewLifecycleOwner(), this::showMessage);
        vm.toastRes.observe(getViewLifecycleOwner(), res -> showMessage(getString(res)));
        vm.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!Boolean.TRUE.equals(vm.clearPending.getValue())) vm.refreshStats();
    }

    @Override
    public void onDestroyView() {
        // 离开页面：撤销提示随之关闭，清空立即执行
        if (undoBar != null) {
            Snackbar bar = undoBar;
            undoBar = null;
            bar.dismiss();
        }
        vm.commitClear();
        super.onDestroyView();
        binding = null;
    }

    private void showMessage(@Nullable String text) {
        if (binding != null && text != null) Snackbars.show(binding.getRoot(), text);
    }

    // ---------- 回答风格 ----------

    private void setUpStyle() {
        vm.style.observe(getViewLifecycleOwner(), s -> {
            renderRadio(binding.ivStyleBrief, binding.rowStyleBrief, AiSettingsPrefs.STYLE_BRIEF.equals(s));
            renderRadio(binding.ivStyleDetail, binding.rowStyleDetail, AiSettingsPrefs.STYLE_DETAIL.equals(s));
            renderRadio(binding.ivStylePlain, binding.rowStylePlain, AiSettingsPrefs.STYLE_PLAIN.equals(s));
        });
    }

    private void renderRadio(ImageView icon, View row, boolean checked) {
        icon.setImageResource(checked ? R.drawable.ic_radio_button_checked : R.drawable.ic_radio_button_unchecked);
        icon.setImageTintList(ColorStateList.valueOf(color(checked ? androidx.appcompat.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOnSurfaceVariant)));
        row.setSelected(checked);
        row.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                          @NonNull android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.RadioButton.class.getName());
                info.setCheckable(true);
                info.setChecked(checked);
            }
        });
    }

    private int color(@AttrRes int attr) {
        return MaterialColors.getColor(binding.getRoot(), attr);
    }

    // ---------- 回答时参考 ----------

    private void setUpSwitches() {
        bindSwitch(binding.rowUseCrops, binding.swUseCrops, vm.useCrops, AiSettingsPrefs.Flag.USE_CROPS);
        bindSwitch(binding.rowUseWeather, binding.swUseWeather, vm.useWeather, AiSettingsPrefs.Flag.USE_WEATHER);
        bindSwitch(binding.rowSaveHistory, binding.swSaveHistory, vm.saveHistory,
                AiSettingsPrefs.Flag.SAVE_HISTORY);
    }

    private void bindSwitch(View row, MaterialSwitch sw, MutableLiveData<Boolean> state, AiSettingsPrefs.Flag flag) {
        CompoundButton.OnCheckedChangeListener listener = (b, on) -> vm.setFlag(flag, on);
        state.observe(getViewLifecycleOwner(), on -> {
            sw.setOnCheckedChangeListener(null);
            sw.setChecked(Boolean.TRUE.equals(on));
            sw.setOnCheckedChangeListener(listener);
        });
        sw.setOnCheckedChangeListener(listener);
        row.setOnClickListener(v -> sw.toggle());
    }

    // ---------- 对话记录 ----------

    private void setUpHistory() {
        vm.historyStats.observe(getViewLifecycleOwner(), s -> renderHistory());
        vm.clearPending.observe(getViewLifecycleOwner(), p -> renderHistory());
        binding.rowHistory.setOnClickListener(v -> {
            if (undoBar != null) undoBar.dismiss();   // 先完成待执行的清空
            AiChatHistorySheet.show(getChildFragmentManager(), 0, false);
        });
        getChildFragmentManager().setFragmentResultListener(AiChatHistorySheet.REQUEST_OPEN,
                getViewLifecycleOwner(), (key, b) -> {
                    Bundle args = new Bundle();
                    args.putLong(NavArgs.SESSION_ID, b.getLong(NavArgs.SESSION_ID, 0));
                    // 从 D06 进入本页时，替换原来的 D06，避免返回栈中出现两个对话页
                    nav().navigate(R.id.aiChatFragment, args, new NavOptions.Builder()
                            .setPopUpTo(R.id.aiChatFragment, true).build());
                });
        getChildFragmentManager().setFragmentResultListener(AiChatHistorySheet.REQUEST_DELETED,
                getViewLifecycleOwner(), (key, b) -> vm.refreshStats());
        vm.showUndo.observe(getViewLifecycleOwner(), this::showUndo);
    }

    private void renderHistory() {
        long[] s = vm.historyStats.getValue();
        boolean pending = Boolean.TRUE.equals(vm.clearPending.getValue());
        if (s == null && !pending) {
            binding.tvHistorySub.setText(" ");
        } else if (pending || s[0] <= 0) {
            binding.tvHistorySub.setText(R.string.ai_settings_history_none);
        } else {
            binding.tvHistorySub.setText(getString(R.string.ai_settings_history_stats, (int) s[0],
                    AiSettingsFormat.size(s[1])));
        }
    }

    private void showUndo(int count) {
        Snackbar bar = Snackbar.make(binding.getRoot(), getString(R.string.ai_settings_cleared, count),
                Snackbar.LENGTH_LONG);
        bar.setAction(R.string.ai_settings_undo, v -> vm.undoClear());
        bar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (undoBar == transientBottomBar) undoBar = null;
                if (event != DISMISS_EVENT_ACTION) vm.commitClear();
            }
        });
        undoBar = bar;
        bar.show();
    }

    // ---------- 模型服务 ----------

    private void setUpModelService() {
        vm.userConfigActive.observe(getViewLifecycleOwner(), a -> renderStatus());
        vm.activeProvider.observe(getViewLifecycleOwner(), a -> renderStatus());
        vm.activeModel.observe(getViewLifecycleOwner(), a -> renderStatus());
        vm.provider.observe(getViewLifecycleOwner(), p -> {
            binding.tvProvider.setText(ModelServicePresets.label(p));
            boolean editable = vm.isBaseUrlEditable();
            binding.tilBaseUrl.setEnabled(editable);
        });
        binding.rowProvider.setOnClickListener(v -> chooseProvider());
        vm.savedKeyLast4.observe(getViewLifecycleOwner(), last4 -> renderKeyHelper());
        bindError(binding.tilBaseUrl, vm.baseUrlError);
        bindError(binding.tilChatModel, vm.chatModelError);
        vm.apiKeyError.observe(getViewLifecycleOwner(), e -> renderKeyHelper());
        vm.testResult.observe(getViewLifecycleOwner(), r -> {
            if (r == null) {
                binding.tvTestResult.setVisibility(View.GONE);
                return;
            }
            binding.tvTestResult.setVisibility(View.VISIBLE);
            binding.tvTestResult.setText(r.success ? getString(R.string.ai_settings_test_ok, r.text) : r.text);
            int c = color(r.success ? androidx.appcompat.R.attr.colorPrimary : androidx.appcompat.R.attr.colorError);
            binding.tvTestResult.setTextColor(c);
            binding.tvTestResult.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    r.success ? R.drawable.ic_check_circle : R.drawable.ic_error, 0, 0, 0);
            binding.tvTestResult.setCompoundDrawableTintList(ColorStateList.valueOf(c));
        });
        binding.btnSave.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            vm.save();
        });
        binding.btnUseBuiltin.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ai_settings_use_builtin_title)
                .setMessage(R.string.ai_settings_use_builtin_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, w) -> vm.useBuiltIn())
                .show());
    }

    private void bindError(TextInputLayout til, MutableLiveData<Integer> error) {
        error.observe(getViewLifecycleOwner(), res -> til.setError(res == null || res == 0 ? null : getString(res)));
    }

    /** API Key：校验错误优先；否则保存过时显示「已保存 ••••abcd」 */
    private void renderKeyHelper() {
        Integer err = vm.apiKeyError.getValue();
        String last4 = vm.savedKeyLast4.getValue();
        if (err != null && err != 0) {
            binding.tilApiKey.setError(getString(err));
            return;
        }
        binding.tilApiKey.setError(null);
        binding.tilApiKey.setHelperText(last4 == null ? null : getString(R.string.ai_settings_api_key_saved, last4));
    }

    private void renderStatus() {
        boolean user = Boolean.TRUE.equals(vm.userConfigActive.getValue());
        binding.tvModelStatus.setText(user ? R.string.ai_settings_active_user : R.string.ai_settings_active_builtin);
        boolean noKey = !user && !vm.hasBuiltInKey;
        if (noKey) {
            binding.ivModelStatus.setImageResource(R.drawable.ic_warning);
            binding.ivModelStatus.setImageTintList(ColorStateList.valueOf(color(androidx.appcompat.R.attr.colorError)));
            binding.tvModelStatusSub.setText(R.string.ai_settings_no_key);
            binding.tvModelStatusSub.setTextColor(color(androidx.appcompat.R.attr.colorError));
        } else {
            binding.ivModelStatus.setImageResource(R.drawable.ic_auto_awesome);
            binding.ivModelStatus.setImageTintList(ColorStateList.valueOf(color(androidx.appcompat.R.attr.colorPrimary)));
            String model = vm.activeModel.getValue();
            binding.tvModelStatusSub.setText(getString(R.string.ai_settings_active_detail,
                    getString(ModelServicePresets.label(vm.activeProvider.getValue())), model == null ? "" : model));
            binding.tvModelStatusSub.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        }
    }

    private void chooseProvider() {
        String[] ids = ModelServicePresets.PROVIDERS;
        String[] labels = new String[ids.length];
        int checked = 0;
        String current = vm.provider.getValue();
        for (int i = 0; i < ids.length; i++) {
            labels[i] = getString(ModelServicePresets.label(ids[i]));
            if (ids[i].equals(current)) checked = i;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ai_settings_provider)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    vm.selectProvider(ids[which]);
                    d.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
