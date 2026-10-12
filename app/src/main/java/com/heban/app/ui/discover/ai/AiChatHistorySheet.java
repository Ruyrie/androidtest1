package com.heban.app.ui.discover.ai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.heban.app.HeBanApp;
import com.heban.app.R;
import com.heban.app.data.local.entity.ChatSessionEntity;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;

import java.util.List;

/**
 * 历史会话（D06 溢出菜单「历史会话」、M11「历史会话」共用）：observeSessions() 列表，点击打开，长按删除。
 * 结果通过宿主的 FragmentManager 返回：{@link #REQUEST_OPEN}（{@link NavArgs#SESSION_ID}，0 为新对话）、
 * {@link #REQUEST_DELETED}（被删除的会话 id）。
 */
public class AiChatHistorySheet extends BottomSheetDialogFragment {
    public static final String TAG = "ai_chat_history";
    public static final String REQUEST_OPEN = "ai_chat_history_open";
    public static final String REQUEST_DELETED = "ai_chat_history_deleted";
    private static final String ARG_SHOW_NEW = "show_new";

    private AiChatHistoryViewModel vm;

    /**
     * @param currentSessionId 当前打开的会话（高亮显示），没有为 0
     * @param showNew          显示「新对话」（D06）
     */
    public static void show(@NonNull FragmentManager fm, long currentSessionId, boolean showNew) {
        if (fm.findFragmentByTag(TAG) != null) return;
        AiChatHistorySheet sheet = new AiChatHistorySheet();
        Bundle args = new Bundle();
        args.putLong(NavArgs.SESSION_ID, currentSessionId);
        args.putBoolean(ARG_SHOW_NEW, showNew);
        sheet.setArguments(args);
        sheet.show(fm, TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ai_chat_sheet_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        long current = args.getLong(NavArgs.SESSION_ID, 0);
        vm = new ViewModelProvider(this, HeBanApp.factory(requireContext())).get(AiChatHistoryViewModel.class);

        View btnNew = view.findViewById(R.id.btn_new);
        btnNew.setVisibility(args.getBoolean(ARG_SHOW_NEW, false) ? View.VISIBLE : View.GONE);
        btnNew.setOnClickListener(v -> open(0));

        RecyclerView rv = view.findViewById(R.id.rv_sessions);
        View empty = view.findViewById(R.id.empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        AiChatHistoryAdapter adapter = new AiChatHistoryAdapter(new AiChatHistoryAdapter.Listener() {
            @Override
            public void onOpen(@NonNull ChatSessionEntity session) {
                open(session.id);
            }

            @Override
            public void onDelete(@NonNull ChatSessionEntity session) {
                confirmDelete(session);
            }
        }, current);
        rv.setAdapter(adapter);

        vm.sessions.observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            boolean isEmpty = list != null && list.isEmpty();
            empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            rv.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });
        vm.deleted.observe(getViewLifecycleOwner(), id -> {
            Bundle b = new Bundle();
            b.putLong(NavArgs.SESSION_ID, id);
            getParentFragmentManager().setFragmentResult(REQUEST_DELETED, b);
            Snackbars.show(view, getString(R.string.ai_chat_history_deleted));
        });
        vm.toast.observe(getViewLifecycleOwner(), t -> Snackbars.show(view, t));
        vm.start();
    }

    private void open(long sessionId) {
        Bundle b = new Bundle();
        b.putLong(NavArgs.SESSION_ID, sessionId);
        getParentFragmentManager().setFragmentResult(REQUEST_OPEN, b);
        dismiss();
    }

    private void confirmDelete(ChatSessionEntity s) {
        List<ChatSessionEntity> list = vm.sessions.getValue();
        if (list == null) return;
        String title = s.title.trim().isEmpty() ? getString(R.string.ai_chat_history_untitled) : s.title;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ai_chat_history_delete_title)
                .setMessage(getString(R.string.ai_chat_history_delete_message, title))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> vm.delete(s.id))
                .show();
    }
}
