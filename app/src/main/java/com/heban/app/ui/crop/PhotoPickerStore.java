package com.heban.app.ui.crop;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.List;

import io.reactivex.rxjava3.core.Single;

/**
 * 把系统照片选择器 / 相机返回的图片复制到 App 私有目录（filesDir/crops、filesDir/records）。
 * 返回的路径是 file:// 地址，可直接交给 ImagePaths / Glide 加载，也是存入数据库的值。
 */
public interface PhotoPickerStore {
    /** C03 作物封面 */
    String DIR_CROPS = "crops";
    /** C04 现场照片 */
    String DIR_RECORDS = "records";

    /** 复制一张图片，在 io 线程执行 */
    @NonNull
    Single<String> copy(@NonNull Uri source, @NonNull String dir);

    /** 按顺序复制多张图片，在 io 线程执行；任一张失败时整体失败（已复制的会被删除） */
    @NonNull
    Single<List<String>> copyAll(@NonNull List<Uri> sources, @NonNull String dir);

    /** 删除本类复制出来的文件（只处理 App 私有目录内的 file:// 路径），后台执行，不关心结果 */
    void delete(@NonNull Collection<String> paths);

    /** 相机拍照的临时写入位置（无法创建时返回 null） */
    @Nullable
    Uri createCameraTarget();

    /** 删除相机临时写入位置（复制完成或取消拍照后），后台执行 */
    void discardCameraTarget(@NonNull Uri target);
}
