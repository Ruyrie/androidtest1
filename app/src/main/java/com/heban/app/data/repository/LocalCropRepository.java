package com.heban.app.data.repository;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.R;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.CropDetail;
import com.heban.app.data.model.CropProfile;
import com.heban.app.data.model.CropStats;
import com.heban.app.data.model.PlotCard;
import com.heban.app.data.model.PlotStats;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * CropRepository 的本地实现（Room，两个 flavor 共用）。
 * 查询随当前账号切换（未登录 / 游客为空），写操作要求已登录；所有操作在 io 线程执行。
 */
public final class LocalCropRepository implements CropRepository {
    private final Context app;
    private final AppDatabase db;
    private final CurrentUser user;
    private final Gson gson;
    private final CropProfiles profiles;
    private final Map<String, Integer> iconCache = new ConcurrentHashMap<>();

    public LocalCropRepository(Context app, AppDatabase db, CurrentUser user, Gson gson) {
        this.app = app;
        this.db = db;
        this.user = user;
        this.gson = gson;
        this.profiles = new CropProfiles(app, gson);
    }

    private static ZoneId zone() {
        return ZoneId.systemDefault();
    }

    // —— 作物 ——

    @Override
    public Flowable<List<CropCard>> observeCropCards(@Nullable String filter) {
        return UserScope.observe(user, Collections::<CropCard>emptyList, uid -> Flowable.combineLatest(
                        db.cropDao().observeAll(uid), db.plotDao().observeAll(uid),
                        (crops, plots) -> CropCards.filter(toCards(crops, plots), filter)))
                .distinctUntilChanged();
    }

    @Override
    public Flowable<CropStats> observeCropStats() {
        return UserScope.observe(user, CropStats::new, uid -> {
            long[] week = DateRanges.week(LocalDate.now(zone()), zone());
            return Flowable.combineLatest(db.cropDao().observeAll(uid),
                    db.farmTaskDao().observeCountBetween(uid, week[0], week[1]), CropCards::stats);
        }).distinctUntilChanged(CropCards::sameStats);
    }

    @Override
    public Flowable<CropDetail> observeCropDetail(long cropId) {
        return user.observeUserId()
                .switchMap(uid -> uid <= 0
                        ? Flowable.just(Optional.<CropDetail>empty())
                        : Flowable.combineLatest(
                        db.cropDao().observeById(cropId, uid),
                        db.plotDao().observeAll(uid),
                        db.farmRecordDao().observeByCrop(cropId),
                        (crops, plots, records) -> crops.isEmpty()
                                ? Optional.<CropDetail>empty()
                                : Optional.of(toDetail(crops.get(0), plots, records))))
                .takeWhile(Optional::isPresent)          // 作物不存在 / 被删除 / 账号切换时结束
                .map(Optional::get)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<CropEntity> getCrop(long cropId) {
        return UserScope.require(user).map(uid -> {
            CropEntity c = db.cropDao().getSync(cropId, uid);
            if (c == null) throw UserScope.notFound("作物不存在");
            return c;
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<Long> saveCrop(CropEntity crop) {
        return UserScope.require(user).map(uid -> db.runInTransaction(() -> {
            long now = System.currentTimeMillis();
            crop.plantedAt = crop.plantedAt > 0
                    ? DateRanges.truncateToDay(crop.plantedAt, zone())
                    : DateRanges.startOfDay(LocalDate.now(zone()), zone());
            if (crop.plotId != null && db.plotDao().getSync(crop.plotId, uid) == null) crop.plotId = null;
            crop.userId = uid;
            if (crop.id == 0) {
                if (crop.expectedDays <= 0) crop.expectedDays = profiles.forCrop(crop.name, crop.variety).expectedDays;
                crop.createdAt = now;
                crop.id = db.cropDao().insertSync(crop);
                return crop.id;
            }
            CropEntity old = db.cropDao().getSync(crop.id, uid);
            if (old == null) throw UserScope.notFound("作物不存在");
            // 改了作物名称 / 品种且预计天数仍是原作物的默认值时，换成新作物的默认值
            boolean renamed = !Objects.equals(old.name, crop.name) || !Objects.equals(old.variety, crop.variety);
            boolean wasDefault = crop.expectedDays == old.expectedDays
                    && old.expectedDays == profiles.forCrop(old.name, old.variety).expectedDays;
            if (crop.expectedDays <= 0 || (renamed && wasDefault)) {
                crop.expectedDays = profiles.forCrop(crop.name, crop.variety).expectedDays;
            }
            // 「需关注」只通过 markAttention / dismissAttention / 天气规则修改
            crop.createdAt = old.createdAt;
            crop.attentionNote = old.attentionNote;
            crop.attentionSource = old.attentionSource;
            crop.attentionDismissedAt = old.attentionDismissedAt;
            db.cropDao().updateSync(crop);
            return crop.id;
        })).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable deleteCrop(long cropId) {
        return UserScope.require(user).map(uid -> db.runInTransaction(() -> {
            if (db.cropDao().getSync(cropId, uid) == null) return Collections.<Long>emptyList();
            List<Long> taskIds = db.farmTaskDao().idsByCropSync(cropId, uid);
            db.farmTaskDao().deleteByCropSync(cropId, uid);
            db.farmRecordDao().deleteByCropSync(cropId, uid);
            db.ledgerDao().clearCropSync(cropId, uid);          // 收支保留，只解除关联
            db.cropDao().deleteSync(cropId, uid);
            return taskIds;
        })).doOnSuccess(ids -> {
            for (Long id : ids) TaskReminders.cancel(app, id);
        }).ignoreElement().subscribeOn(Schedulers.io());
    }

    @Override
    public Completable dismissAttention(long cropId) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() ->
                        db.cropDao().dismissAttentionSync(cropId, uid, System.currentTimeMillis())))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Completable markAttention(long cropId, String note, String source) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() -> {
            String n = note == null || note.trim().isEmpty() ? null : note.trim();
            db.cropDao().setAttentionSync(cropId, uid, n, n == null ? null : source);
        })).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<List<CropEntity>> listCrops() {
        return user.userId()
                .flatMap(uid -> uid > 0 ? db.cropDao().listAll(uid) : Single.just(Collections.<CropEntity>emptyList()))
                .subscribeOn(Schedulers.io());
    }

    /** 天气规则自动调用：未登录或作物不存在时什么也不做 */
    @Override
    public Completable setWeatherAttention(long cropId, @Nullable String note) {
        return user.userId().observeOn(Schedulers.io()).flatMapCompletable(uid -> uid <= 0
                ? Completable.complete()
                : Completable.fromAction(() -> db.runInTransaction(() -> {
            CropEntity c = db.cropDao().getSync(cropId, uid);
            if (c == null) return;
            switch (AttentionRules.weather(c, note, System.currentTimeMillis())) {
                case MARK:
                    db.cropDao().setAttentionSync(cropId, uid, note.trim(), CropEntity.ATTENTION_WEATHER);
                    break;
                case CLEAR:
                    db.cropDao().setAttentionSync(cropId, uid, null, null);
                    break;
                default:
                    break;
            }
        }))).subscribeOn(Schedulers.io());
    }

    @Override
    public CropProfile profileFor(String cropName) {
        return profiles.forName(cropName);
    }

    @Override
    public List<CropProfile> allProfiles() {
        return profiles.all();
    }

    // —— 地块 ——

    @Override
    public Flowable<List<PlotEntity>> observePlots() {
        return UserScope.observe(user, Collections::<PlotEntity>emptyList, uid -> db.plotDao().observeAll(uid));
    }

    @Override
    public Flowable<List<PlotCard>> observePlotCards() {
        return UserScope.observe(user, Collections::<PlotCard>emptyList, uid -> Flowable.combineLatest(
                db.plotDao().observeAll(uid),
                db.cropDao().observeAll(uid),
                db.ledgerDao().observeOutByPlot(uid),
                db.farmRecordDao().observeLastIrrigationByPlot(uid),
                (plots, crops, costs, irrigations) -> {
                    List<CropCard> cards = toCards(crops, plots);
                    Map<Long, CropCard> byCrop = new HashMap<>();
                    for (CropCard c : cards) byCrop.put(c.id, c);
                    return PlotCards.cards(plots, crops, byCrop, costs, irrigations);
                })).distinctUntilChanged();
    }

    @Override
    public Flowable<PlotStats> observePlotStats() {
        return UserScope.observe(user, PlotStats::new, uid -> Flowable.combineLatest(
                        db.plotDao().observeAll(uid), db.cropDao().observeAll(uid), PlotCards::stats))
                .distinctUntilChanged(PlotCards::sameStats);
    }

    @Override
    public Single<Long> savePlot(PlotEntity plot) {
        return UserScope.require(user).map(uid -> db.runInTransaction(() -> {
            long now = System.currentTimeMillis();
            plot.userId = uid;
            boolean hasReading = plot.indoorTemp != null || plot.indoorHumidity != null;
            if (plot.id == 0) {
                plot.createdAt = now;
                if (hasReading && plot.readingAt == 0) plot.readingAt = now;
                plot.id = db.plotDao().insertSync(plot);
                return plot.id;
            }
            PlotEntity old = db.plotDao().getSync(plot.id, uid);
            if (old == null) throw UserScope.notFound("地块不存在");
            plot.createdAt = old.createdAt;
            if (!hasReading) {
                // 编辑表单没有温湿度：保留最近一次记录
                plot.indoorTemp = old.indoorTemp;
                plot.indoorHumidity = old.indoorHumidity;
                plot.readingAt = old.readingAt;
            } else if (!Objects.equals(plot.indoorTemp, old.indoorTemp)
                    || !Objects.equals(plot.indoorHumidity, old.indoorHumidity)) {
                plot.readingAt = now;
            } else {
                plot.readingAt = old.readingAt;
            }
            db.plotDao().updateSync(plot);
            return plot.id;
        })).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable deletePlot(long plotId) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() ->
                db.runInTransaction(() -> {
                    db.cropDao().clearPlotSync(plotId, uid);
                    db.plotDao().deleteSync(plotId, uid);
                }))).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable recordPlotReading(long plotId, double tempCelsius, int humidityPercent) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() ->
                        db.plotDao().updateReadingSync(plotId, uid, tempCelsius, humidityPercent,
                                System.currentTimeMillis())))
                .subscribeOn(Schedulers.io());
    }

    // —— 农事记录 ——

    /**
     * 新增（id == 0）或修改农事记录。同一事务内：写记录；syncToLedger 且 costCent > 0 时写一笔支出
     * （类别按记录类型，备注如「施肥 · 硫酸钾」），并把支出 id 存到记录上。修改时先删除原来同步的支出。
     */
    @Override
    public Single<Long> saveRecord(FarmRecordEntity record, boolean syncToLedger) {
        return UserScope.require(user).map(uid -> db.runInTransaction(() -> {
            if (db.cropDao().getSync(record.cropId, uid) == null) throw UserScope.notFound("作物不存在");
            long now = System.currentTimeMillis();
            record.userId = uid;
            if (record.date == 0) record.date = now;
            if (record.id != 0) {
                FarmRecordEntity old = db.farmRecordDao().getSync(record.id, uid);
                if (old == null) throw UserScope.notFound("记录不存在");
                record.createdAt = old.createdAt;
                if (old.ledgerEntryId != null) db.ledgerDao().deleteSync(old.ledgerEntryId, uid);
                db.ledgerDao().deleteByRecordSync(record.id, uid);
                record.ledgerEntryId = null;
                db.farmRecordDao().updateSync(record);
            } else {
                record.createdAt = now;
                record.ledgerEntryId = null;
                record.id = db.farmRecordDao().insertSync(record);
            }
            if (syncToLedger && record.costCent > 0) {
                LedgerEntryEntity entry = LedgerCategories.entryFor(record);
                entry.userId = uid;
                entry.createdAt = now;
                record.ledgerEntryId = db.ledgerDao().insertSync(entry);
                db.farmRecordDao().updateSync(record);
            }
            return record.id;
        })).subscribeOn(Schedulers.io());
    }

    /** 删除农事记录及其同步生成的支出 */
    @Override
    public Completable deleteRecord(long recordId) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() ->
                db.runInTransaction(() -> {
                    FarmRecordEntity r = db.farmRecordDao().getSync(recordId, uid);
                    if (r == null) return;
                    if (r.ledgerEntryId != null) db.ledgerDao().deleteSync(r.ledgerEntryId, uid);
                    db.ledgerDao().deleteByRecordSync(recordId, uid);
                    db.farmRecordDao().deleteSync(recordId, uid);
                }))).subscribeOn(Schedulers.io());
    }

    // —— 组装展示数据 ——

    private List<CropCard> toCards(List<CropEntity> crops, List<PlotEntity> plots) {
        Map<Long, PlotEntity> plotById = indexPlots(plots);
        ZoneId zone = zone();
        LocalDate today = LocalDate.now(zone);
        List<CropCard> out = new ArrayList<>(crops.size());
        for (CropEntity c : crops) {
            out.add(toCard(c, c.plotId == null ? null : plotById.get(c.plotId), today, zone));
        }
        return out;
    }

    private CropCard toCard(CropEntity c, @Nullable PlotEntity plot, LocalDate today, ZoneId zone) {
        CropProfile profile = profiles.forCrop(c.name, c.variety);
        return CropCards.card(c, plot, profile, iconRes(profile.icon), today, zone);
    }

    private CropDetail toDetail(CropEntity c, List<PlotEntity> plots, List<FarmRecordEntity> records) {
        ZoneId zone = zone();
        PlotEntity plot = c.plotId == null ? null : indexPlots(plots).get(c.plotId);
        CropProfile profile = profiles.forCrop(c.name, c.variety);
        CropCard card = CropCards.card(c, plot, profile, iconRes(profile.icon), LocalDate.now(zone), zone);
        return CropCards.detail(c, plot, card, profile, records, zone);
    }

    private static Map<Long, PlotEntity> indexPlots(List<PlotEntity> plots) {
        Map<Long, PlotEntity> m = new HashMap<>();
        for (PlotEntity p : plots) m.put(p.id, p);
        return m;
    }

    /** 生长期表中的图标名 → @drawable/ic_<icon>，找不到时用 ic_eco */
    @DrawableRes
    private int iconRes(@Nullable String icon) {
        String key = icon == null || icon.isEmpty() ? CropProfiles.DEFAULT_ICON : icon;
        Integer cached = iconCache.get(key);
        if (cached != null) return cached;
        int id = app.getResources().getIdentifier("ic_" + key, "drawable", app.getPackageName());
        if (id == 0) id = R.drawable.ic_eco;
        iconCache.put(key, id);
        return id;
    }
}
