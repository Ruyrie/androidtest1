package com.heban.app.ui.discover.scan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.heban.app.HeBanApp;
import com.heban.app.R;
import com.heban.app.data.model.ScanMode;
import com.heban.app.databinding.ScanSheetHistoryBinding;
import com.heban.app.util.ImagePaths;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/** D02「历史」：识别记录（scan_record）底部弹窗；点一条进入 D03 查看保存的结果，长按删除。 */
public class ScanHistorySheet extends BottomSheetDialogFragment {
    public static final String TAG = "scan_history";

    private ScanSheetHistoryBinding binding;
    private ScanViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = ScanSheetHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(NavHostFragment.findNavController(this).getBackStackEntry(R.id.scan_graph),
                HeBanApp.factory(requireContext())).get(ScanViewModel.class);
        Adapter adapter = new Adapter(getString(R.string.scan_history_unusable),
                getString(R.string.scan_mode_pest), getString(R.string.scan_mode_crop),
                getString(R.string.scan_history_saved));
        binding.list.setAdapter(adapter);
        vm.history().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            boolean empty = list == null || list.isEmpty();
            binding.list.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void open(ScanViewModel.HistoryItem item) {
        dismiss();
        vm.openHistory(item.id);
    }

    private void confirmDelete(ScanViewModel.HistoryItem item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.scan_history_delete_title)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.scan_history_delete, (d, w) -> vm.deleteRecord(item.id))
                .show();
    }

    private final class Adapter extends ListAdapter<ScanViewModel.HistoryItem, Holder> {
        private final String unusable;
        private final String pestLabel;
        private final String cropLabel;
        private final String savedLabel;
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA);
        private final ZoneId zone = ZoneId.systemDefault();

        Adapter(String unusable, String pestLabel, String cropLabel, String savedLabel) {
            super(DIFF);
            this.unusable = unusable;
            this.pestLabel = pestLabel;
            this.cropLabel = cropLabel;
            this.savedLabel = savedLabel;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.scan_item_history, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            ScanViewModel.HistoryItem item = getItem(position);
            h.title.setText(ScanFormat.historyTitle(item.result, unusable));
            StringBuilder sub = new StringBuilder(fmt.format(Instant.ofEpochMilli(item.createdAt).atZone(zone)));
            sub.append(" · ").append(item.mode == ScanMode.CROP ? cropLabel : pestLabel);
            if (item.saved) sub.append(" · ").append(savedLabel);
            h.subtitle.setText(sub);
            Glide.with(h.thumb)
                    .load(ImagePaths.toUri(item.imagePath))
                    .placeholder(R.drawable.ph_image)
                    .error(R.drawable.ph_image)
                    .centerCrop()
                    .into(h.thumb);
            h.itemView.setOnClickListener(v -> open(item));
            h.itemView.setOnLongClickListener(v -> {
                confirmDelete(item);
                return true;
            });
        }
    }

    private static final class Holder extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final TextView title;
        final TextView subtitle;

        Holder(@NonNull View v) {
            super(v);
            thumb = v.findViewById(R.id.thumb);
            title = v.findViewById(R.id.tv_title);
            subtitle = v.findViewById(R.id.tv_subtitle);
        }
    }

    private static final DiffUtil.ItemCallback<ScanViewModel.HistoryItem> DIFF =
            new DiffUtil.ItemCallback<ScanViewModel.HistoryItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull ScanViewModel.HistoryItem a,
                                               @NonNull ScanViewModel.HistoryItem b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull ScanViewModel.HistoryItem a,
                                                  @NonNull ScanViewModel.HistoryItem b) {
                    return a.createdAt == b.createdAt && a.saved == b.saved && a.mode == b.mode
                            && Objects.equals(a.imagePath, b.imagePath)
                            && Objects.equals(titleOf(a), titleOf(b));
                }

                private String titleOf(ScanViewModel.HistoryItem i) {
                    return ScanFormat.historyTitle(i.result, "");
                }
            };
}
