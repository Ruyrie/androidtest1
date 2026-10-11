package com.heban.app.ui.home;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.color.MaterialColors;
import com.heban.app.HeBanApp;
import com.heban.app.databinding.WeatherCityPickerBinding;
import com.heban.app.ui.common.Snackbars;

/** H02 切换地区：底部弹窗搜索城市，选择后保存地点并关闭。H01 缺少地点时也从这里选择。 */
public class CityPickerDialog extends BottomSheetDialogFragment {
    public static final String TAG = "city_picker";

    private WeatherCityPickerBinding binding;
    private CityPickerViewModel vm;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = WeatherCityPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this, HeBanApp.factory(requireContext())).get(CityPickerViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        CityPickerAdapter adapter = new CityPickerAdapter(vm::select);
        binding.results.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.results.setAdapter(adapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                vm.onQueryChanged(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.etSearch.requestFocus();

        vm.results.observe(getViewLifecycleOwner(), adapter::submitList);
        vm.status.observe(getViewLifecycleOwner(), s -> {
            updateProgress();
            binding.statusText.setTextColor(MaterialColors.getColor(binding.statusText,
                    s == CityPickerViewModel.Status.ERROR
                            ? androidx.appcompat.R.attr.colorError
                            : com.google.android.material.R.attr.colorOnSurfaceVariant));
        });
        vm.saving.observe(getViewLifecycleOwner(), s -> updateProgress());
        vm.done.observe(getViewLifecycleOwner(), d -> dismissAllowingStateLoss());
        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(view, msg));
    }

    private void updateProgress() {
        boolean busy = vm.status.getValue() == CityPickerViewModel.Status.SEARCHING
                || Boolean.TRUE.equals(vm.saving.getValue());
        binding.progress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
