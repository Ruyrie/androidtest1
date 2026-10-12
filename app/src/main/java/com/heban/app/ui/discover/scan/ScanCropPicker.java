package com.heban.app.ui.discover.scan;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.heban.app.R;
import com.heban.app.data.model.CropCard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * D03「保存到作物记录」：识别出的作物没有唯一对应的作物时，让用户选择保存到哪种作物。
 * 同名作物排在前面并默认选中第一个。
 */
final class ScanCropPicker {
    private ScanCropPicker() {
    }

    static void show(@NonNull Context context, @NonNull List<CropCard> crops, @NonNull List<CropCard> matched,
                     @NonNull Consumer<CropCard> onPick) {
        Map<Long, CropCard> ordered = new LinkedHashMap<>();
        for (CropCard c : matched) ordered.put(c.id, c);
        for (CropCard c : crops) if (!ordered.containsKey(c.id)) ordered.put(c.id, c);
        List<CropCard> list = new ArrayList<>(ordered.values());
        String[] labels = new String[list.size()];
        for (int i = 0; i < list.size(); i++) labels[i] = ScanFormat.cropLabel(list.get(i));
        int[] selected = {0};
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.scan_result_pick_crop)
                .setSingleChoiceItems(labels, 0, (d, which) -> selected[0] = which)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.scan_result_save, (d, w) -> {
                    if (selected[0] >= 0 && selected[0] < list.size()) onPick.accept(list.get(selected[0]));
                })
                .show();
    }
}
