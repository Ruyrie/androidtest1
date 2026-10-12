package com.heban.app.ui.discover.scan;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.local.entity.ScanRecordEntity;
import com.heban.app.data.model.ScanMode;
import com.heban.app.data.model.ScanOutcome;
import com.heban.app.data.model.ScanResult;
import com.heban.app.data.repository.PestRepository;
import com.heban.app.data.repository.VisionRepository;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.subjects.SingleSubject;

/** 拍照识别测试用的内存仓库与样例数据（原型中的黄瓜霜霉病）。 */
final class ScanFakes {
    private ScanFakes() {
    }

    /** 纯 JVM 中无法构造 Bitmap，用 Unsafe 分配一个空实例（仓库假实现不读取像素） */
    static Bitmap bitmap() {
        try {
            Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            return (Bitmap) unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, Bitmap.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    static ScanResult sampleResult() {
        ScanResult r = new ScanResult();
        r.imageOk = true;
        r.crop = "黄瓜";
        r.top = new ScanResult.Top();
        r.top.name = "黄瓜霜霉病";
        r.top.category = "卵菌病害";
        r.top.part = "叶部";
        r.top.confidence = 0.92;
        r.top.severity = "中度";
        r.top.evidence = "叶面受叶脉限制的黄色多角形斑，叶背有灰黑色霉层";
        r.top.urgencyDays = 3;
        r.others.add(other("黄瓜细菌性角斑病", 0.06));
        r.others.add(other("缺镁症（生理性）", 0.02));
        return r;
    }

    static ScanResult.Other other(String name, double c) {
        ScanResult.Other o = new ScanResult.Other();
        o.name = name;
        o.confidence = c;
        return o;
    }

    static ScanOutcome outcome(long recordId, ScanResult r) {
        ScanOutcome o = new ScanOutcome();
        o.recordId = recordId;
        o.result = r;
        o.provider = "DeepSeek";
        o.imagePath = "file:///data/scan/" + recordId + ".jpg";
        o.pestId = 11L;
        o.createdAt = 1_000L;
        return o;
    }

    static final class FakeVision implements VisionRepository {
        SingleSubject<ScanOutcome> pending = SingleSubject.create();
        final List<ScanMode> recognizeModes = new ArrayList<>();
        final List<String> recognizeHints = new ArrayList<>();
        final List<Bitmap> recognizePhotos = new ArrayList<>();
        final List<Long> retried = new ArrayList<>();
        final List<Long> loaded = new ArrayList<>();
        final List<long[]> savedToCrop = new ArrayList<>();
        final List<Long> deleted = new ArrayList<>();
        final Map<Long, ScanOutcome> records = new HashMap<>();
        final BehaviorProcessor<List<ScanRecordEntity>> history =
                BehaviorProcessor.createDefault(Collections.emptyList());
        @Nullable Throwable saveError;

        @Override
        public Single<ScanOutcome> recognize(Bitmap photo, ScanMode mode, @Nullable String cropHint) {
            recognizePhotos.add(photo);
            recognizeModes.add(mode);
            recognizeHints.add(cropHint);
            return pending;
        }

        @Override
        public Single<ScanOutcome> retry(long recordId) {
            retried.add(recordId);
            return pending;
        }

        @Override
        public Single<ScanOutcome> getOutcome(long recordId) {
            loaded.add(recordId);
            ScanOutcome o = records.get(recordId);
            return o == null ? Single.error(new IllegalStateException("missing")) : Single.just(o);
        }

        @Override
        public Flowable<List<ScanRecordEntity>> observeHistory() {
            return history;
        }

        @Override
        public Completable saveToCrop(long recordId, long cropId) {
            if (saveError != null) return Completable.error(saveError);
            savedToCrop.add(new long[]{recordId, cropId});
            return Completable.complete();
        }

        @Override
        public Completable deleteRecord(long recordId) {
            deleted.add(recordId);
            return Completable.complete();
        }
    }

    static final class FakePests implements PestRepository {
        final Map<String, Long> ids = new HashMap<>();

        @Override
        public Flowable<List<PestEntity>> search(@Nullable String keyword, @Nullable String kind, @Nullable String crop) {
            return Flowable.just(Collections.emptyList());
        }

        @Override
        public Flowable<List<PestEntity>> seasonal(int month) {
            return Flowable.just(Collections.emptyList());
        }

        @Override
        public Flowable<Integer> count() {
            return Flowable.just(0);
        }

        @Override
        public Single<PestEntity> get(long id) {
            return Single.error(new IllegalStateException());
        }

        @Override
        public Maybe<PestEntity> matchByName(String name) {
            Long id = ids.get(name);
            if (id == null) return Maybe.empty();
            PestEntity p = new PestEntity();
            p.id = id;
            p.name = name;
            return Maybe.just(p);
        }

        @Override
        public Single<List<String>> cropOptions() {
            return Single.just(Collections.emptyList());
        }
    }
}
