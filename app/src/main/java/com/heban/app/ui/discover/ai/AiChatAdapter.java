package com.heban.app.ui.discover.ai;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.util.ImagePaths;

/** D06 消息列表：欢迎语 / 用户消息 / 助手回答。 */
public class AiChatAdapter extends ListAdapter<AiChatItem, RecyclerView.ViewHolder> {

    /** 列表中的点击 */
    public interface Listener {
        void onSuggestion(@NonNull String question);

        void onCopy(@NonNull AiChatItem item);

        void onLike(@NonNull AiChatItem item);

        void onRegenerate(@NonNull AiChatItem item);

        void onPest(@NonNull AiChatItem item);
    }

    private static final DiffUtil.ItemCallback<AiChatItem> DIFF = new DiffUtil.ItemCallback<AiChatItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull AiChatItem a, @NonNull AiChatItem b) {
            return a.sameItem(b);
        }

        @Override
        public boolean areContentsTheSame(@NonNull AiChatItem a, @NonNull AiChatItem b) {
            return a.equals(b);
        }
    };

    private final Listener listener;

    public AiChatAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).kind;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == AiChatItem.WELCOME) {
            return new WelcomeHolder(inf.inflate(R.layout.ai_chat_item_welcome, parent, false));
        }
        if (viewType == AiChatItem.USER) {
            return new UserHolder(inf.inflate(R.layout.ai_chat_item_user, parent, false));
        }
        return new AssistantHolder(inf.inflate(R.layout.ai_chat_item_assistant, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AiChatItem item = getItem(position);
        if (holder instanceof UserHolder) {
            ((UserHolder) holder).bind(item);
        } else if (holder instanceof AssistantHolder) {
            ((AssistantHolder) holder).bind(item);
        }
    }

    final class WelcomeHolder extends RecyclerView.ViewHolder {
        WelcomeHolder(@NonNull View v) {
            super(v);
            TextView c1 = v.findViewById(R.id.chip_suggest_1);
            TextView c2 = v.findViewById(R.id.chip_suggest_2);
            c1.setOnClickListener(x -> listener.onSuggestion(c1.getText().toString()));
            c2.setOnClickListener(x -> listener.onSuggestion(c2.getText().toString()));
        }
    }

    static final class UserHolder extends RecyclerView.ViewHolder {
        private final View imageFrame;
        private final ImageView image;
        private final TextView text;

        UserHolder(@NonNull View v) {
            super(v);
            imageFrame = v.findViewById(R.id.image_frame);
            image = v.findViewById(R.id.iv_image);
            text = v.findViewById(R.id.tv_text);
        }

        void bind(AiChatItem item) {
            boolean hasImage = item.imagePath != null && !item.imagePath.isEmpty();
            imageFrame.setVisibility(hasImage ? View.VISIBLE : View.GONE);
            if (hasImage) {
                // 加载失败时露出下方的占位（图标 +「照片」）
                Glide.with(image).load(ImagePaths.toUri(item.imagePath))
                        .placeholder((Drawable) null).error((Drawable) null)
                        .centerCrop().into(image);
            } else {
                Glide.with(image).clear(image);
            }
            text.setText(item.text);
            text.setVisibility(item.text.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    final class AssistantHolder extends RecyclerView.ViewHolder {
        private final TextView text;
        private final View actions;
        private final View copy;
        private final ImageButton like;
        private final View refresh;
        private final View incomplete;
        private final View regenerate;
        private final View pestChip;
        private final TextView pest;
        private AiChatItem item;

        AssistantHolder(@NonNull View v) {
            super(v);
            text = v.findViewById(R.id.tv_text);
            actions = v.findViewById(R.id.row_actions);
            copy = v.findViewById(R.id.btn_copy);
            like = v.findViewById(R.id.btn_like);
            refresh = v.findViewById(R.id.btn_refresh);
            incomplete = v.findViewById(R.id.tv_incomplete);
            regenerate = v.findViewById(R.id.btn_regenerate);
            pestChip = v.findViewById(R.id.chip_pest);
            pest = v.findViewById(R.id.tv_pest);
            // 行高由 AiChatSpans 按块设置，去掉 Body Large 自带的行距
            text.setLineSpacing(0f, 1f);
            text.setIncludeFontPadding(false);
            copy.setOnClickListener(x -> listener.onCopy(item));
            like.setOnClickListener(x -> listener.onLike(item));
            refresh.setOnClickListener(x -> listener.onRegenerate(item));
            regenerate.setOnClickListener(x -> listener.onRegenerate(item));
            pestChip.setOnClickListener(x -> listener.onPest(item));
        }

        void bind(AiChatItem item) {
            this.item = item;
            Context ctx = text.getContext();
            if (item.text.trim().isEmpty()) {
                if (item.streaming) {
                    text.setVisibility(View.VISIBLE);
                    text.setText(thinking(ctx));
                } else {
                    text.setVisibility(View.GONE);
                }
            } else {
                text.setVisibility(View.VISIBLE);
                text.setText(AiChatSpans.render(ctx, item.text));
            }

            boolean showActions = item.saved && !item.streaming;
            actions.setVisibility(showActions ? View.VISIBLE : View.GONE);
            if (!showActions) return;
            boolean done = !item.incomplete;
            copy.setVisibility(item.text.trim().isEmpty() ? View.GONE : View.VISIBLE);
            like.setVisibility(done ? View.VISIBLE : View.GONE);
            like.setImageResource(item.liked ? R.drawable.ic_thumb_up_fill : R.drawable.ic_thumb_up);
            like.setImageTintList(android.content.res.ColorStateList.valueOf(MaterialColors.getColor(like,
                    item.liked ? androidx.appcompat.R.attr.colorPrimary
                            : com.google.android.material.R.attr.colorOnSurfaceVariant)));
            like.setSelected(item.liked);
            refresh.setVisibility(done && item.canRegenerate ? View.VISIBLE : View.GONE);
            incomplete.setVisibility(done ? View.GONE : View.VISIBLE);
            regenerate.setVisibility(!done && item.canRegenerate ? View.VISIBLE : View.GONE);
            boolean hasPest = done && item.pestId > 0 && item.pestName != null;
            pestChip.setVisibility(hasPest ? View.VISIBLE : View.GONE);
            if (hasPest) pest.setText(ctx.getString(R.string.ai_chat_pest_chip, item.pestName));
        }

        /** 「正在思考…」：14sp On Surface Variant */
        private CharSequence thinking(Context ctx) {
            SpannableString s = new SpannableString(ctx.getString(R.string.ai_chat_thinking));
            int size = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14,
                    ctx.getResources().getDisplayMetrics()));
            s.setSpan(new AbsoluteSizeSpan(size), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(new ForegroundColorSpan(MaterialColors.getColor(ctx,
                    com.google.android.material.R.attr.colorOnSurfaceVariant, 0)), 0, s.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return s;
        }
    }
}
