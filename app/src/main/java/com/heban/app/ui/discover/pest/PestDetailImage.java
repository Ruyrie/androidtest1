package com.heban.app.ui.discover.pest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** D05 顶部图片：相对路径（可为空，显示占位）+ 图片说明（如「叶片正面症状」）。 */
public final class PestDetailImage {
    @Nullable
    public final String path;
    @Nullable
    public final String caption;

    public PestDetailImage(@Nullable String path, @Nullable String caption) {
        this.path = path;
        this.caption = caption;
    }

    /** images 与 image_captions 按下标对应；两者都为空时返回一张没有说明的占位图 */
    @NonNull
    public static List<PestDetailImage> zip(@NonNull List<String> paths, @NonNull List<String> captions) {
        int n = Math.max(paths.size(), captions.size());
        if (n == 0) return Collections.singletonList(new PestDetailImage(null, null));
        List<PestDetailImage> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(new PestDetailImage(i < paths.size() ? paths.get(i) : null,
                    i < captions.size() ? captions.get(i) : null));
        }
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PestDetailImage)) return false;
        PestDetailImage that = (PestDetailImage) o;
        return Objects.equals(path, that.path) && Objects.equals(caption, that.caption);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, caption);
    }
}
