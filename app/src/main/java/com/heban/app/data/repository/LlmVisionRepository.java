package com.heban.app.data.repository;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.local.entity.ScanRecordEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.local.prefs.SecureStore;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.model.FarmType;
import com.heban.app.data.model.ScanMode;
import com.heban.app.data.model.ScanOutcome;
import com.heban.app.data.model.ScanResult;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.remote.llm.LlmClient;
import com.heban.app.data.remote.llm.LlmConfigProvider;
import com.heban.app.data.remote.llm.VisionRecognizer;
import com.heban.app.util.ImageUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;

/**
 * VisionRepository 的大模型实现（技术准备文档 9.5）：照片在 computation 线程压缩为 1280px、质量 80 的 JPEG，
 * 先调用主模型（用户识图模型或 DeepSeek 视觉模型），按表 9-4 失败时切换通义千问 VL 备用模型（各 30 秒超时）；
 * 解析通过后把照片保存到 filesDir/scans/ 并写入 scan_record，识别名称按 pest 表名称 / 别名匹配百科条目。
 * 识别记录按当前账号隔离；未登录时 recognize 等写操作抛出 NOT_LOGGED_IN，没有任何可用 Key 时直接以
 * BusinessException 结束（提示到「AI 助手设置 → 模型服务」配置）。
 */
public final class LlmVisionRepository implements VisionRepository {
    private final Context app;
    private final OkHttpClient baseClient;
    private final Gson gson;
    private final AppDatabase db;
    private final SettingsStore settings;
    private final SecureStore secureStore;
    private final CurrentUser user;
    private final PestRepository pestRepository;
    private final CropRepository cropRepository;

    private final LlmConfigProvider configs;
    private final VisionRecognizer recognizer;

    public LlmVisionRepository(Context app, OkHttpClient baseClient, Gson gson, AppDatabase db,
                               SettingsStore settings, SecureStore secureStore, CurrentUser user,
                               PestRepository pestRepository, CropRepository cropRepository) {
        this.app = app;
        this.baseClient = baseClient;
        this.gson = gson;
        this.db = db;
        this.settings = settings;
        this.secureStore = secureStore;
        this.user = user;
        this.pestRepository = pestRepository;
        this.cropRepository = cropRepository;
        this.configs = new LlmConfigProvider(settings, secureStore);
        this.recognizer = new VisionRecognizer(new LlmClient(baseClient, gson), gson);
    }

    @Override
    public Single<ScanOutcome> recognize(Bitmap photo, ScanMode mode, @Nullable String cropHint) {
        ScanMode m = mode == null ? ScanMode.PEST : mode;
        return UserScope.require(user)
                .flatMap(uid -> {
                    LlmConfigProvider.VisionPair pair = configs.vision();
                    if (pair == null) return Single.<ScanOutcome>error(LlmConfigProvider.notConfigured());
                    return Single.fromCallable(() -> ImageUtils.compress(photo, ImageUtils.MAX_EDGE, ImageUtils.QUALITY))
                            .subscribeOn(Schedulers.computation())
                            .flatMap(jpeg -> recognizer.recognize(pair.primary, pair.backup,
                                            ImageUtils.toDataUrl(jpeg), m, cropHint)
                                    .observeOn(Schedulers.io())
                                    .map(r -> saveNew(uid, jpeg, m, r)));
                })
                .flatMap(this::withPest)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<ScanOutcome> retry(long recordId) {
        return UserScope.require(user)
                .flatMap(uid -> {
                    ScanRecordEntity rec = requireRecord(recordId, uid);
                    File photo = ImageUtils.toFile(rec.imagePath);
                    if (photo == null || !photo.isFile()) {
                        return Single.<ScanOutcome>error(new BusinessException("IMAGE_MISSING", "照片已不存在，请重新拍摄"));
                    }
                    LlmConfigProvider.VisionPair pair = configs.vision();
                    if (pair == null) return Single.<ScanOutcome>error(LlmConfigProvider.notConfigured());
                    String dataUrl = ImageUtils.toDataUrl(ImageUtils.readBytes(photo));   // 已是压缩后的 JPEG
                    ScanMode mode = modeOf(rec.mode);
                    return recognizer.recognize(pair.primary, pair.backup, dataUrl, mode, null)
                            .observeOn(Schedulers.io())
                            .map(r -> {
                                ScanRecordEntity cur = requireRecord(recordId, uid);
                                cur.resultJson = gson.toJson(r.result);
                                cur.provider = r.provider;
                                db.scanRecordDao().updateSync(cur);
                                return toOutcome(cur, r.result);
                            });
                })
                .flatMap(this::withPest)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<ScanOutcome> getOutcome(long recordId) {
        return UserScope.require(user)
                .map(uid -> {
                    ScanRecordEntity rec = requireRecord(recordId, uid);
                    return toOutcome(rec, parseStored(rec));
                })
                .flatMap(this::withPest)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Flowable<List<ScanRecordEntity>> observeHistory() {
        return UserScope.observe(user, Collections::<ScanRecordEntity>emptyList,
                uid -> db.scanRecordDao().observeAll(uid));
    }

    @Override
    public Completable saveToCrop(long recordId, long cropId) {
        return UserScope.require(user)
                .flatMapCompletable(uid -> {
                    ScanRecordEntity rec = requireRecord(recordId, uid);
                    ScanResult result = parseStored(rec);
                    if (!result.imageOk || result.top == null || result.top.name == null || result.top.name.isEmpty()) {
                        return Completable.error(new BusinessException("SCAN_UNUSABLE", "这次识别没有结果，不能保存"));
                    }
                    FarmRecordEntity record = farmRecordFor(result, rec.imagePath, cropId, System.currentTimeMillis(),
                            gson);
                    Completable mark = isHealthy(result) ? Completable.complete()
                            : cropRepository.markAttention(cropId, result.top.name, CropEntity.ATTENTION_SCAN);
                    return cropRepository.saveRecord(record, false).ignoreElement()
                            .andThen(mark)
                            .andThen(Completable.fromAction(() -> db.scanRecordDao().setCropSync(recordId, uid, cropId)));
                })
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Completable deleteRecord(long recordId) {
        return UserScope.require(user)
                .flatMapCompletable(uid -> Completable.fromAction(() -> {
                    ScanRecordEntity rec = db.scanRecordDao().getSync(recordId, uid);
                    if (rec == null) return;
                    db.scanRecordDao().deleteSync(recordId, uid);
                    // 已保存到作物记录的照片由农事记录继续使用，不删除
                    if (rec.cropId == null) ImageUtils.deleteInDir(app, rec.imagePath, ImageUtils.DIR_SCANS);
                }))
                .subscribeOn(Schedulers.io());
    }

    // ———————————————————————— 内部 ————————————————————————

    /** 保存照片与识别记录（io 线程） */
    private ScanOutcome saveNew(long uid, byte[] jpeg, ScanMode mode, VisionRecognizer.Recognition r) throws Exception {
        File file = ImageUtils.saveJpeg(app, jpeg, ImageUtils.DIR_SCANS);
        ScanRecordEntity rec = new ScanRecordEntity();
        rec.userId = uid;
        rec.imagePath = ImageUtils.toFileUri(file);
        rec.mode = mode.name();
        rec.resultJson = gson.toJson(r.result);
        rec.provider = r.provider;
        rec.createdAt = System.currentTimeMillis();
        try {
            rec.id = db.scanRecordDao().insertSync(rec);
        } catch (RuntimeException e) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            throw e;
        }
        return toOutcome(rec, r.result);
    }

    /** 识别名称按 pest 表名称 / 别名匹配百科条目（照片不可用或没有结果时不匹配） */
    private Single<ScanOutcome> withPest(ScanOutcome o) {
        ScanResult r = o.result;
        if (r == null || !r.imageOk || r.top == null || r.top.name == null || r.top.name.isEmpty()) {
            return Single.just(o);
        }
        return pestRepository.matchByName(r.top.name)
                .map(p -> p.id)
                .onErrorComplete()
                .map(id -> {
                    o.pestId = id;
                    return o;
                })
                .defaultIfEmpty(o);
    }

    private ScanRecordEntity requireRecord(long recordId, long uid) {
        ScanRecordEntity rec = db.scanRecordDao().getSync(recordId, uid);
        if (rec == null) throw UserScope.notFound("识别记录不存在");
        return rec;
    }

    private ScanResult parseStored(ScanRecordEntity rec) {
        try {
            ScanResult r = rec.resultJson == null ? null : gson.fromJson(rec.resultJson, ScanResult.class);
            if (r != null) return r;
        } catch (JsonParseException ignored) {
            // 记录损坏
        }
        throw new BusinessException("SCAN_BROKEN", "识别记录已损坏，请重新识别");
    }

    @NonNull
    static ScanOutcome toOutcome(@NonNull ScanRecordEntity rec, @NonNull ScanResult result) {
        ScanOutcome o = new ScanOutcome();
        o.result = result;
        o.provider = rec.provider;
        o.recordId = rec.id;
        o.imagePath = rec.imagePath;
        o.mode = modeOf(rec.mode);
        o.cropId = rec.cropId;
        o.createdAt = rec.createdAt;
        return o;
    }

    @NonNull
    static ScanMode modeOf(@Nullable String name) {
        try {
            return name == null ? ScanMode.PEST : ScanMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return ScanMode.PEST;
        }
    }

    static boolean isHealthy(@NonNull ScanResult r) {
        return r.top != null && "健康".equals(r.top.category);
    }

    /**
     * 保存到作物的农事记录：类型其他，内容「拍照识别：黄瓜霜霉病（中度）」，附识别照片，作业时间为现在。
     */
    @NonNull
    static FarmRecordEntity farmRecordFor(@NonNull ScanResult result, @Nullable String imagePath, long cropId,
                                          long now, @NonNull Gson gson) {
        FarmRecordEntity r = new FarmRecordEntity();
        r.cropId = cropId;
        r.type = FarmType.OTHER.name();
        r.date = now;
        r.material = materialFor(result);
        r.photos = imagePath == null || imagePath.isEmpty()
                ? null : gson.toJson(Collections.singletonList(imagePath));
        return r;
    }

    /** 「拍照识别：黄瓜霜霉病（中度）」，没有严重程度时省略括号 */
    @NonNull
    static String materialFor(@NonNull ScanResult result) {
        String name = result.top == null || result.top.name == null ? "" : result.top.name.trim();
        String severity = result.top == null || result.top.severity == null ? "" : result.top.severity.trim();
        return "拍照识别：" + name + (severity.isEmpty() ? "" : "（" + severity + "）");
    }
}
