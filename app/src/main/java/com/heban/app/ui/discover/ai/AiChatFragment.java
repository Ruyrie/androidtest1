package com.heban.app.ui.discover.ai;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.databinding.FragmentAiChatBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;
import com.heban.app.util.ImagePaths;

import java.util.ArrayList;
import java.util.List;

/**
 * D06 农技 AI 助手（原型 AIChat.dc.html，技术准备文档 9.3 / 9.7 / 9.8 / 12.4）。
 * 参数：{@link NavArgs#SESSION_ID} 打开历史会话；{@link NavArgs#AI_PROMPT} 在新会话中自动发送一次该问题。
 * 游客可以打开页面，发送、上传照片、点赞、重新生成前需要登录。
 */
public class AiChatFragment extends BaseFragment {
    private FragmentAiChatBinding binding;
    private AiChatViewModel vm;
    private AiChatAdapter adapter;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImage = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (vm != null) vm.attachImage(uri);
            });

    private final ActivityResultLauncher<Intent> voiceInput = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (vm == null || result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
                ArrayList<String> texts = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (texts != null && !texts.isEmpty()) vm.appendVoice(texts.get(0));
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(AiChatViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        setUpToolbar();
        setUpList();
        setUpInput();
        listenToHistory();

        vm.toast.observe(getViewLifecycleOwner(), this::showMessage);
        vm.toastRes.observe(getViewLifecycleOwner(), res -> showMessage(getString(res)));

        Bundle args = getArguments() == null ? new Bundle() : getArguments();
        long sessionId = args.getLong(NavArgs.SESSION_ID, 0);
        String prompt = args.getString(NavArgs.AI_PROMPT);
        if (prompt != null) {
            // 只自动发送一次（页面重建时不再发送）
            args.remove(NavArgs.AI_PROMPT);
            if (!container().currentUser.isSignedIn()) {
                vm.input.setValue(prompt);
                prompt = null;
                LoginRequired.check(this);
            }
        }
        vm.start(sessionId, prompt);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapter = null;
        binding = null;
    }

    private void showMessage(@Nullable String text) {
        if (binding != null && text != null) Snackbars.show(binding.getRoot(), text);
    }

    // ---------- 顶栏 ----------

    private void setUpToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());
        Drawable overflow = ContextCompat.getDrawable(requireContext(), R.drawable.ic_more_vert);
        if (overflow != null) {
            overflow = overflow.mutate();
            overflow.setTint(MaterialColors.getColor(binding.toolbar,
                    com.google.android.material.R.attr.colorOnSurface));
            binding.toolbar.setOverflowIcon(overflow);
        }
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.ai_chat_action_settings) {
                nav().navigate(R.id.aiSettingsFragment);
                return true;
            }
            if (item.getItemId() == R.id.ai_chat_action_history) {
                AiChatHistorySheet.show(getChildFragmentManager(), vm.sessionId(), true);
                return true;
            }
            return false;
        });
    }

    private void listenToHistory() {
        getChildFragmentManager().setFragmentResultListener(AiChatHistorySheet.REQUEST_OPEN,
                getViewLifecycleOwner(), (key, b) -> vm.openSession(b.getLong(NavArgs.SESSION_ID, 0)));
        getChildFragmentManager().setFragmentResultListener(AiChatHistorySheet.REQUEST_DELETED,
                getViewLifecycleOwner(), (key, b) -> vm.onSessionDeleted(b.getLong(NavArgs.SESSION_ID, 0)));
    }

    // ---------- 消息列表 ----------

    private void setUpList() {
        RecyclerView rv = binding.rvMessages;
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);   // 内容贴底（原型 justify-content: flex-end）
        rv.setLayoutManager(lm);
        rv.setItemAnimator(null);
        int gap = getResources().getDimensionPixelSize(R.dimen.card_gap);
        rv.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect out, @NonNull View child, @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                if (parent.getChildAdapterPosition(child) > 0) out.top = gap;
            }
        });
        adapter = new AiChatAdapter(new AiChatAdapter.Listener() {
            @Override
            public void onSuggestion(@NonNull String question) {
                if (!LoginRequired.check(AiChatFragment.this)) return;
                vm.ask(question);
            }

            @Override
            public void onCopy(@NonNull AiChatItem item) {
                copy(item);
            }

            @Override
            public void onLike(@NonNull AiChatItem item) {
                if (!LoginRequired.check(AiChatFragment.this)) return;
                vm.toggleLike(item);
            }

            @Override
            public void onRegenerate(@NonNull AiChatItem item) {
                if (!LoginRequired.check(AiChatFragment.this)) return;
                vm.regenerate(item);
            }

            @Override
            public void onPest(@NonNull AiChatItem item) {
                Bundle b = new Bundle();
                b.putLong(NavArgs.PEST_ID, item.pestId);
                nav().navigate(R.id.pestDetailFragment, b);
            }
        });
        rv.setAdapter(adapter);
        vm.items.observe(getViewLifecycleOwner(), this::renderItems);
        vm.scrollToEnd.observe(getViewLifecycleOwner(), x -> scrollToEnd());
    }

    private void renderItems(List<AiChatItem> list) {
        if (adapter == null) return;
        boolean atBottom = !binding.rvMessages.canScrollVertically(1);
        adapter.submitList(list, () -> {
            if (atBottom) scrollToEnd();
        });
        // 页面重建（如进程被回收）后回到同一个会话
        if (getArguments() != null) getArguments().putLong(NavArgs.SESSION_ID, vm.sessionId());
    }

    private void scrollToEnd() {
        if (binding == null || adapter == null || adapter.getItemCount() == 0) return;
        binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
    }

    private void copy(AiChatItem item) {
        ClipboardManager cm = requireContext().getSystemService(ClipboardManager.class);
        if (cm == null) return;
        cm.setPrimaryClip(ClipData.newPlainText(getString(R.string.ai_chat_title),
                AiChatMarkdown.toPlainText(item.text)));
        showMessage(getString(R.string.ai_chat_copied));
    }

    // ---------- 输入栏 ----------

    private void setUpInput() {
        binding.btnSend.setOnClickListener(v -> {
            if (vm.isStreaming()) {
                vm.stop();
                return;
            }
            if (!LoginRequired.check(this)) return;
            vm.send();
        });
        binding.btnPhoto.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            pickImage.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
        binding.btnMic.setOnClickListener(v -> startVoiceInput());
        vm.streaming.observe(getViewLifecycleOwner(), s -> renderSend());
        vm.canSend.observe(getViewLifecycleOwner(), s -> renderSend());
        vm.attachedImage.observe(getViewLifecycleOwner(), path -> {
            if (path == null) {
                Glide.with(binding.ivAttachment).clear(binding.ivAttachment);
            } else {
                Glide.with(binding.ivAttachment).load(ImagePaths.toUri(path))
                        .placeholder((Drawable) null).error((Drawable) null)
                        .centerCrop().into(binding.ivAttachment);
            }
        });
        TooltipCompat.setTooltipText(binding.btnPhoto, getString(R.string.ai_chat_add_photo));
        TooltipCompat.setTooltipText(binding.btnMic, getString(R.string.ai_chat_voice));
    }

    /** 发送按钮：输入为空时不可用；生成中变为「停止生成」 */
    private void renderSend() {
        if (binding == null) return;
        boolean streaming = Boolean.TRUE.equals(vm.streaming.getValue());
        boolean canSend = Boolean.TRUE.equals(vm.canSend.getValue());
        binding.btnSend.setEnabled(streaming || canSend);
        binding.btnSend.setImageResource(streaming ? R.drawable.ai_chat_ic_stop : R.drawable.ic_arrow_upward);
        String label = getString(streaming ? R.string.ai_chat_stop : R.string.ai_chat_send);
        binding.btnSend.setContentDescription(label);
        TooltipCompat.setTooltipText(binding.btnSend, label);
    }

    /** 系统语音识别（普通话）；没有可用的识别服务时提示功能暂未开放 */
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.ai_chat_input_hint));
        try {
            voiceInput.launch(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            showMessage(getString(R.string.feature_not_open));
        }
    }
}
