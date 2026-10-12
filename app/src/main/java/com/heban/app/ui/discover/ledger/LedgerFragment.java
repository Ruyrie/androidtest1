package com.heban.app.ui.discover.ledger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavBackStackEntry;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.heban.app.R;
import com.heban.app.databinding.FragmentLedgerBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;

import java.time.YearMonth;

/**
 * D10 收支记账（原型 Ledger.dc.html）：月份切换（不能超过当前月）、本月汇总、结余趋势 / 支出构成两张自绘图、
 * 按天分组的明细。点手动记的账进入 D11 编辑；来自农事记录的账目只提示到农事记录中修改；长按手动账目删除。
 */
public class LedgerFragment extends BaseFragment {
    /** D11 保存 / 删除后带回的提示（previousBackStackEntry 的 SavedStateHandle） */
    public static final String RESULT_MESSAGE = "ledger_result_message";

    private FragmentLedgerBinding binding;
    private LedgerViewModel vm;
    private LedgerAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLedgerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(LedgerViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        vm.start();

        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());

        setUpViewToggle();
        setUpCharts();
        setUpList();
        setUpFab();

        vm.month.observe(getViewLifecycleOwner(), this::renderBalanceLabel);
        vm.toast.observe(getViewLifecycleOwner(), this::showMessage);
        vm.messageRes.observe(getViewLifecycleOwner(), res -> showMessage(getString(res)));
        observeResult();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapter = null;
        binding = null;
    }

    private void renderBalanceLabel(@Nullable YearMonth m) {
        if (m == null || binding == null) return;
        boolean current = m.equals(vm.currentMonth());
        binding.tvBalanceLabel.setText(current ? getString(R.string.ledger_balance_current)
                : getString(R.string.ledger_balance_month, m.getMonthValue()));
    }

    // ---------- 结余趋势 / 支出构成 ----------

    private void setUpViewToggle() {
        binding.groupView.check(Boolean.TRUE.equals(vm.showMix.getValue()) ? R.id.btn_mix : R.id.btn_trend);
        renderToggleIcons();
        binding.groupView.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_mix) vm.showMix();
            else vm.showTrend();
            renderToggleIcons();
        });
    }

    /** 选中的分段显示勾（原型 check 18px） */
    private void renderToggleIcons() {
        for (MaterialButton b : new MaterialButton[]{binding.btnTrend, binding.btnMix}) {
            if (b.isChecked()) b.setIconResource(R.drawable.ic_check);
            else b.setIcon(null);
        }
    }

    private void setUpCharts() {
        binding.chartMix.setEmptyText(getString(R.string.ledger_mix_empty));
        vm.bars.observe(getViewLifecycleOwner(), bars -> binding.chartTrend.setBars(bars));
        vm.mix.observe(getViewLifecycleOwner(), rows -> binding.chartMix.setRows(rows));
    }

    // ---------- 明细 ----------

    private void setUpList() {
        adapter = new LedgerAdapter(new LedgerAdapter.Listener() {
            @Override
            public void onClick(@NonNull LedgerRow row) {
                onRowClicked(row);
            }

            @Override
            public void onLongClick(@NonNull LedgerRow row) {
                onRowLongClicked(row);
            }
        });
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setItemAnimator(null);
        binding.list.setAdapter(adapter);
        vm.rows.observe(getViewLifecycleOwner(), rows -> adapter.submitList(rows));
    }

    private void onRowClicked(LedgerRow row) {
        if (row.fromRecord) {
            showMessage(getString(R.string.ledger_record_locked));
            return;
        }
        if (!LoginRequired.check(this)) return;
        Bundle args = new Bundle();
        args.putLong(NavArgs.ENTRY_ID, row.entryId);
        nav().navigate(R.id.addEntryFragment, args);
    }

    private void onRowLongClicked(LedgerRow row) {
        if (row.fromRecord) {
            showMessage(getString(R.string.ledger_record_locked));
            return;
        }
        if (!LoginRequired.check(this)) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ledger_delete_title)
                .setMessage(getString(R.string.ledger_delete_message, row.title, row.amount))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> vm.deleteEntry(row.entryId))
                .show();
    }

    // ---------- 记一笔 ----------

    private void setUpFab() {
        binding.fabAdd.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            nav().navigate(R.id.addEntryFragment);
        });
        // 列表向下滚动时收起为图标，向上滚动时展开（UI 设计文档表 6-1）
        binding.scroll.setOnScrollChangeListener((View.OnScrollChangeListener) (v, x, y, oldX, oldY) -> {
            if (binding == null) return;
            if (y > oldY + 4 && binding.fabAdd.isExtended()) binding.fabAdd.shrink();
            else if ((y < oldY - 4 || y == 0) && !binding.fabAdd.isExtended()) binding.fabAdd.extend();
        });
    }

    /** D11 保存 / 删除后返回本页时显示提示（挂在 FAB 上方） */
    private void observeResult() {
        NavBackStackEntry entry = nav().getCurrentBackStackEntry();
        if (entry == null) return;
        MutableLiveData<String> result = entry.getSavedStateHandle().getLiveData(RESULT_MESSAGE);
        result.observe(getViewLifecycleOwner(), text -> {
            if (text == null) return;
            entry.getSavedStateHandle().set(RESULT_MESSAGE, null);
            showMessage(text);
        });
    }

    private void showMessage(@Nullable String text) {
        if (binding == null || text == null) return;
        Snackbar.make(binding.root, text, Snackbar.LENGTH_LONG).setAnchorView(binding.fabAdd).show();
    }
}
