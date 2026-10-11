package com.heban.app.ui.crop;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.R;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.databinding.FragmentCropsBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;

import java.util.ArrayList;
import java.util.List;

/** C01 我的作物（原型 Crops.dc.html）。 */
public class CropsFragment extends BaseFragment {
    private FragmentCropsBinding binding;
    private CropsViewModel vm;
    private CropsAdapter adapter;

    /** 系统语音识别（中文），识别结果填入搜索框 */
    private final ActivityResultLauncher<Intent> voice =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (vm == null || result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
                ArrayList<String> texts = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (texts != null && !texts.isEmpty() && texts.get(0) != null) vm.search.setValue(texts.get(0).trim());
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCropsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(CropsViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.crops_action_plots) {
                nav().navigate(R.id.plotsFragment);
                return true;
            } else if (item.getItemId() == R.id.crops_action_add) {
                addCrop();
                return true;
            }
            return false;
        });

        adapter = new CropsAdapter(card -> {
            Bundle args = new Bundle();
            args.putLong(NavArgs.CROP_ID, card.id);
            nav().navigate(R.id.cropDetailFragment, args);
        });
        binding.list.setAdapter(adapter);
        vm.cards.observe(getViewLifecycleOwner(), adapter::submitList);

        setUpSearch();
        setUpFilters();
        binding.empty.setOnAction(v -> addCrop());

        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(binding.getRoot(), msg));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }

    private void addCrop() {
        if (!LoginRequired.check(this)) return;
        nav().navigate(R.id.addCropFragment);
    }

    // ---------- 搜索 ----------

    private void setUpSearch() {
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        vm.hasQuery.observe(getViewLifecycleOwner(), has -> {
            boolean on = Boolean.TRUE.equals(has);
            binding.btnMic.setImageResource(on ? R.drawable.ic_close : R.drawable.ic_mic);
            binding.btnMic.setContentDescription(getString(on ? R.string.crops_clear_search : R.string.crops_voice_search));
        });
        binding.btnMic.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(vm.hasQuery.getValue())) {
                vm.clearSearch();
            } else {
                startVoiceSearch();
            }
        });
    }

    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.crops_voice_prompt));
        try {
            voice.launch(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Snackbars.show(binding.getRoot(), getString(R.string.feature_not_open));
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = requireContext().getSystemService(InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
        binding.etSearch.clearFocus();
    }

    // ---------- 筛选 Chip ----------

    private void setUpFilters() {
        vm.selectedFilter.observe(getViewLifecycleOwner(), f -> {
            int id = chipFor(f);
            if (binding.chips.getCheckedChipId() != id) binding.chips.check(id);
        });
        binding.chips.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            vm.selectFilter(filterFor(ids));
        });
    }

    private static String filterFor(List<Integer> ids) {
        int id = ids.get(0);
        if (id == R.id.chip_greenhouse) return CropRepository.FILTER_GREENHOUSE;
        if (id == R.id.chip_open) return CropRepository.FILTER_OPEN;
        if (id == R.id.chip_attention) return CropRepository.FILTER_ATTENTION;
        return CropRepository.FILTER_ALL;
    }

    private static int chipFor(String f) {
        if (CropRepository.FILTER_GREENHOUSE.equals(f)) return R.id.chip_greenhouse;
        if (CropRepository.FILTER_OPEN.equals(f)) return R.id.chip_open;
        if (CropRepository.FILTER_ATTENTION.equals(f)) return R.id.chip_attention;
        return R.id.chip_all;
    }
}
