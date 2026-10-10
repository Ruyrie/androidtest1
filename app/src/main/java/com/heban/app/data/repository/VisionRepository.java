package com.heban.app.data.repository;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.ScanRecordEntity;
import com.heban.app.data.model.ScanMode;
import com.heban.app.data.model.ScanOutcome;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * 拍照识别（D02 / D03，技术准备文档 9.5）：压缩为 1280px JPEG → 主模型（用户识图模型或 DeepSeek 视觉模型），
 * 按表 9-4 失败时切换通义千问 VL 备用模型；结果解析后保存到 scan_record，并按 pest 表匹配百科条目。
 */
public interface VisionRepository {
    Single<ScanOutcome> recognize(Bitmap photo, ScanMode mode, @Nullable String cropHint);

    /** 用同一张照片重新识别（D03 右上角「刷新」） */
    Single<ScanOutcome> retry(long recordId);

    Single<ScanOutcome> getOutcome(long recordId);

    Flowable<List<ScanRecordEntity>> observeHistory();

    /**
     * D03「保存到「黄瓜 · 2号大棚」记录」：把识别结果保存为该作物的一条农事记录（类型其他，附照片），
     * 并标记作物「需关注」（来源 scan）。
     */
    Completable saveToCrop(long recordId, long cropId);

    Completable deleteRecord(long recordId);
}
