package com.heban.app.ui.discover.ai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.heban.app.R;
import com.heban.app.data.local.entity.ChatSessionEntity;

import java.time.ZoneId;

/** 历史会话列表：点击打开，长按删除。 */
public class AiChatHistoryAdapter extends ListAdapter<ChatSessionEntity, AiChatHistoryAdapter.Holder> {

    public interface Listener {
        void onOpen(@NonNull ChatSessionEntity session);

        void onDelete(@NonNull ChatSessionEntity session);
    }

    private static final DiffUtil.ItemCallback<ChatSessionEntity> DIFF =
            new DiffUtil.ItemCallback<ChatSessionEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChatSessionEntity a, @NonNull ChatSessionEntity b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChatSessionEntity a, @NonNull ChatSessionEntity b) {
                    return a.title.equals(b.title) && a.updatedAt == b.updatedAt;
                }
            };

    private final Listener listener;
    private final long currentSessionId;

    public AiChatHistoryAdapter(@NonNull Listener listener, long currentSessionId) {
        super(DIFF);
        this.listener = listener;
        this.currentSessionId = currentSessionId;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ai_chat_item_session, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        ChatSessionEntity s = getItem(position);
        Context ctx = h.itemView.getContext();
        h.title.setText(s.title.trim().isEmpty() ? ctx.getString(R.string.ai_chat_history_untitled) : s.title);
        h.time.setText(AiChatTimes.format(s.updatedAt, System.currentTimeMillis(), ZoneId.systemDefault(),
                ctx.getString(R.string.ai_chat_time_today), ctx.getString(R.string.ai_chat_time_yesterday),
                ctx.getString(R.string.ai_chat_time_this_year), ctx.getString(R.string.ai_chat_time_other_year)));
        h.itemView.setActivated(s.id == currentSessionId);
        h.itemView.setOnClickListener(v -> listener.onOpen(s));
        h.itemView.setOnLongClickListener(v -> {
            listener.onDelete(s);
            return true;
        });
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView time;

        Holder(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tv_title);
            time = v.findViewById(R.id.tv_time);
        }
    }
}
