package com.heban.app.ui.discover.pest;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.model.FarmType;
import com.heban.app.databinding.FragmentPestDetailBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;
import com.heban.app.ui.discover.DiscoverSpacing;

import java.util.List;

/**
 * D05 病害详情（原型 PestDetail.dc.html）。参数 {@link NavArgs#PEST_ID}（long）。
 * 收藏、问问 AI、加入农事日历需要登录；分享为纯文本（名称 + 症状 + 「来自禾伴」）。
 */
public class PestDetailFragment extends BaseFragment {
    /** 图片超过这个数量时显示页码点（2 张时第二张已露出一部分） */
    private static final int DOTS_MIN_IMAGES = 3;

    private FragmentPestDetailBinding binding;
    private PestDetailViewModel vm;
    private PestDetailImageAdapter imageAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPestDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(PestDetailViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        Bundle args = getArguments();
        vm.start(args == null ? -1L : args.getLong(NavArgs.PEST_ID, -1L),
                getString(R.string.pest_detail_label_reminder));

        binding.btnBack.setOnClickListener(v -> nav().navigateUp());
        binding.btnBookmark.setOnClickListener(v -> {
            if (LoginRequired.check(this)) vm.toggleFavorite();
        });
        binding.btnShare.setOnClickListener(v -> share());
        binding.btnAskAi.setOnClickListener(v -> askAi());
        binding.btnAddTask.setOnClickListener(v -> addTask());

        setUpImages();
        setUpPrevention();

        vm.favorite.observe(getViewLifecycleOwner(), fav -> {
            int attr = Boolean.TRUE.equals(fav) ? androidx.appcompat.R.attr.colorPrimary
                    : com.google.android.material.R.attr.colorOnSurfaceVariant;
            binding.btnBookmark.setImageTintList(ColorStateList.valueOf(MaterialColors.getColor(binding.btnBookmark, attr)));
        });
        vm.pest.observe(getViewLifecycleOwner(), p -> {
            if (p != null) imageAdapter.setPestName(p.name);
        });
        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(binding.getRoot(), msg));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        imageAdapter = null;
    }

    // ---------- 图片 ----------

    private void setUpImages() {
        imageAdapter = new PestDetailImageAdapter();
        binding.images.setAdapter(imageAdapter);
        binding.images.addItemDecoration(new DiscoverSpacing(getResources().getDimensionPixelSize(R.dimen.chip_gap)));
        new PestDetailSnapHelper().attachToRecyclerView(binding.images);
        binding.images.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                updateDots();
            }
        });
        vm.images.observe(getViewLifecycleOwner(), list -> {
            imageAdapter.submitList(list);
            buildDots(list.size());
        });
    }

    private void buildDots(int count) {
        binding.dots.removeAllViews();
        boolean show = count >= DOTS_MIN_IMAGES;
        binding.dots.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;
        int size = getResources().getDimensionPixelSize(R.dimen.pest_detail_dot);
        int gap = getResources().getDimensionPixelSize(R.dimen.pest_detail_dot_gap);
        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            dot.setBackgroundResource(R.drawable.pest_detail_dot);
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(size, size);
            if (i > 0) lp.setMarginStart(gap);
            binding.dots.addView(dot, lp);
        }
        updateDots();
    }

    private void updateDots() {
        if (binding == null || binding.dots.getChildCount() == 0) return;
        LinearLayoutManager llm = (LinearLayoutManager) binding.images.getLayoutManager();
        if (llm == null) return;
        int page = PestDetailSnapHelper.currentPage(llm, binding.dots.getChildCount());
        int on = MaterialColors.getColor(binding.dots, androidx.appcompat.R.attr.colorPrimary);
        int off = MaterialColors.getColor(binding.dots, com.google.android.material.R.attr.colorOutlineVariant);
        for (int i = 0; i < binding.dots.getChildCount(); i++) {
            binding.dots.getChildAt(i).setBackgroundTintList(ColorStateList.valueOf(i == page ? on : off));
        }
    }

    // ---------- 防治方法 ----------

    private void setUpPrevention() {
        vm.chemSelected.observe(getViewLifecycleOwner(), chem -> {
            int id = Boolean.TRUE.equals(chem) ? R.id.btn_chem : R.id.btn_agri;
            if (binding.groupPrevention.getCheckedButtonId() != id) binding.groupPrevention.check(id);
            updateSegmentIcons();
        });
        binding.groupPrevention.addOnButtonCheckedListener((group, id, checked) -> {
            if (checked) vm.selectChem(id == R.id.btn_chem);
            updateSegmentIcons();
        });
        vm.steps.observe(getViewLifecycleOwner(), this::showSteps);
    }

    private void updateSegmentIcons() {
        for (MaterialButton b : new MaterialButton[]{binding.btnAgri, binding.btnChem}) {
            b.setIconResource(b.isChecked() ? R.drawable.ic_check : 0);
        }
    }

    private void showSteps(List<String> steps) {
        binding.steps.removeAllViews();
        int gap = getResources().getDimensionPixelSize(R.dimen.chip_gap);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < steps.size(); i++) {
            View row = inflater.inflate(R.layout.pest_detail_item_step, binding.steps, false);
            ((TextView) row.findViewById(R.id.text)).setText(steps.get(i));
            if (i > 0) ((ViewGroup.MarginLayoutParams) row.getLayoutParams()).topMargin = gap;
            binding.steps.addView(row);
        }
        boolean empty = steps.isEmpty() && Boolean.TRUE.equals(vm.loaded.getValue());
        binding.tvStepsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    // ---------- 操作 ----------

    private void share() {
        PestEntity p = vm.pest.getValue();
        if (p == null) return;
        String symptoms = p.symptoms == null ? "" : p.symptoms;
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, p.name)
                .putExtra(Intent.EXTRA_TEXT, getString(R.string.pest_detail_share_text, p.name, symptoms));
        try {
            startActivity(Intent.createChooser(send, getString(R.string.pest_detail_share_chooser)));
        } catch (ActivityNotFoundException e) {
            Snackbars.show(binding.getRoot(), getString(R.string.feature_not_open));
        }
    }

    /** 问问 AI：带入「黄瓜霜霉病怎么防治？」 */
    private void askAi() {
        String name = vm.name();
        if (name == null || !LoginRequired.check(this)) return;
        Bundle args = new Bundle();
        args.putString(NavArgs.AI_PROMPT, getString(R.string.pest_detail_ai_prompt, name));
        nav().navigate(R.id.aiChatFragment, args);
    }

    /** 加入农事日历：H04 预选「用药」，标题「防治黄瓜霜霉病」 */
    private void addTask() {
        String name = vm.name();
        if (name == null || !LoginRequired.check(this)) return;
        Bundle args = new Bundle();
        args.putString(NavArgs.FARM_TYPE, FarmType.SPRAY.name());
        args.putString(NavArgs.TASK_TITLE, getString(R.string.pest_detail_task_title, name));
        nav().navigate(R.id.addTaskFragment, args);
    }
}
