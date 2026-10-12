package com.heban.app.ui.discover.ledger;

import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.model.CategoryShare;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.CropDetail;
import com.heban.app.data.model.CropProfile;
import com.heban.app.data.model.CropStats;
import com.heban.app.data.model.LedgerDay;
import com.heban.app.data.model.LedgerItem;
import com.heban.app.data.model.LedgerMonthSummary;
import com.heban.app.data.model.MonthBalance;
import com.heban.app.data.model.PlotCard;
import com.heban.app.data.model.PlotStats;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.data.repository.LedgerRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** D10 / D11 ViewModel 测试用的假数据（数值取自原型 Ledger / AddEntry）。 */
final class LedgerFakes {
    static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private LedgerFakes() {
    }

    static LedgerEntryEntity entry(long id, String type, String category, long cents, LocalDate day, String note,
                                   String source) {
        LedgerEntryEntity e = new LedgerEntryEntity();
        e.id = id;
        e.userId = 1;
        e.type = type;
        e.category = category;
        e.amountCent = cents;
        e.date = day.atTime(9, 30).atZone(ZONE).toInstant().toEpochMilli();
        e.note = note;
        e.source = source;
        e.createdAt = 1234L;
        return e;
    }

    static LedgerItem item(LedgerEntryEntity e, String crop, String plot) {
        LedgerItem i = new LedgerItem();
        i.entry = e;
        i.cropName = crop;
        i.plotName = plot;
        return i;
    }

    /** 原型的「今天 · 10月10日  收 960 · 支 60」两条流水 */
    static LedgerDay prototypeDay(LocalDate today) {
        LedgerDay d = new LedgerDay();
        d.date = today;
        d.incomeCent = 96000;
        d.expenseCent = 6000;
        d.items.add(item(entry(11, LedgerEntryEntity.TYPE_IN, "农产品销售", 96000, today, "樱桃番茄 120 斤",
                LedgerEntryEntity.SOURCE_MANUAL), "番茄", "1号大棚"));
        d.items.add(item(entry(12, LedgerEntryEntity.TYPE_OUT, "化肥", 6000, today, "硫酸钾 5 kg",
                LedgerEntryEntity.SOURCE_RECORD), "番茄", "1号大棚"));
        return d;
    }

    static CropCard card(long id, String name, String plot) {
        CropCard c = new CropCard();
        c.id = id;
        c.name = name;
        c.plotName = plot;
        return c;
    }

    static final class Ledger implements LedgerRepository {
        final Map<YearMonth, LedgerMonthSummary> summaries = new HashMap<>();
        final Map<YearMonth, List<CategoryShare>> mixes = new HashMap<>();
        final Map<YearMonth, List<LedgerDay>> days = new HashMap<>();
        /** 原型的近 6 个月结余（元）：5月 820 … 10月 3460，结束月份为 2026-10 */
        long[] balancesYuan = {820, 1960, 3120, 280, 1450, 3460};
        final List<YearMonth> requestedMonths = new ArrayList<>();
        final List<LedgerEntryEntity> added = new ArrayList<>();
        final List<Long> deleted = new ArrayList<>();
        LedgerEntryEntity stored;
        boolean failWrite;

        @Override
        public Single<Long> addEntry(LedgerEntryEntity entry) {
            if (failWrite) return Single.error(new BusinessException("NOT_LOGGED_IN", "请先登录"));
            added.add(entry);
            return Single.just(entry.id == 0 ? 99L : entry.id);
        }

        @Override
        public Completable deleteEntry(long entryId) {
            if (failWrite) return Completable.error(new BusinessException("NOT_LOGGED_IN", "请先登录"));
            deleted.add(entryId);
            return Completable.complete();
        }

        @Override
        public Single<LedgerEntryEntity> getEntry(long entryId) {
            return stored != null && stored.id == entryId ? Single.just(stored)
                    : Single.error(new IllegalStateException("not found"));
        }

        @Override
        public Flowable<LedgerMonthSummary> observeMonth(YearMonth month) {
            requestedMonths.add(month);
            LedgerMonthSummary s = summaries.get(month);
            return Flowable.just(s == null ? new LedgerMonthSummary() : s);
        }

        @Override
        public Flowable<List<MonthBalance>> observeRecentBalances(YearMonth endMonth, int months) {
            List<MonthBalance> out = new ArrayList<>();
            for (int i = 0; i < months; i++) {
                YearMonth m = endMonth.minusMonths(months - 1 - i);
                int idx = balancesYuan.length - months + i;
                long yuan = endMonth.equals(YearMonth.of(2026, 10)) && idx >= 0 ? balancesYuan[idx] : 0;
                out.add(new MonthBalance(m, yuan * 100));
            }
            return Flowable.just(out);
        }

        @Override
        public Flowable<List<CategoryShare>> observeExpenseMix(YearMonth month) {
            List<CategoryShare> l = mixes.get(month);
            return Flowable.just(l == null ? Collections.emptyList() : l);
        }

        @Override
        public Flowable<List<LedgerDay>> observeDays(YearMonth month) {
            List<LedgerDay> l = days.get(month);
            return Flowable.just(l == null ? Collections.emptyList() : l);
        }
    }

    static final class Crops implements CropRepository {
        List<CropCard> cards = new ArrayList<>();

        @Override public Flowable<List<CropCard>> observeCropCards(@Nullable String filter) { return Flowable.just(cards); }
        @Override public Flowable<CropStats> observeCropStats() { return Flowable.just(new CropStats()); }
        @Override public Flowable<CropDetail> observeCropDetail(long cropId) { return Flowable.empty(); }
        @Override public Single<CropEntity> getCrop(long cropId) { return Single.error(new IllegalStateException()); }
        @Override public Single<Long> saveCrop(CropEntity crop) { return Single.just(1L); }
        @Override public Completable deleteCrop(long cropId) { return Completable.complete(); }
        @Override public Completable dismissAttention(long cropId) { return Completable.complete(); }
        @Override public Completable markAttention(long cropId, String note, String source) { return Completable.complete(); }
        @Override public Single<List<CropEntity>> listCrops() { return Single.just(Collections.emptyList()); }
        @Override public Completable setWeatherAttention(long cropId, @Nullable String note) { return Completable.complete(); }
        @Override public CropProfile profileFor(String cropName) { return new CropProfile(); }
        @Override public List<CropProfile> allProfiles() { return Collections.emptyList(); }
        @Override public Flowable<List<PlotEntity>> observePlots() { return Flowable.just(Collections.emptyList()); }
        @Override public Flowable<List<PlotCard>> observePlotCards() { return Flowable.just(Collections.emptyList()); }
        @Override public Flowable<PlotStats> observePlotStats() { return Flowable.just(new PlotStats()); }
        @Override public Single<Long> savePlot(PlotEntity plot) { return Single.just(1L); }
        @Override public Completable deletePlot(long plotId) { return Completable.complete(); }
        @Override public Completable recordPlotReading(long plotId, double t, int h) { return Completable.complete(); }
        @Override public Single<Long> saveRecord(FarmRecordEntity record, boolean syncToLedger) { return Single.just(1L); }
        @Override public Completable deleteRecord(long recordId) { return Completable.complete(); }
    }
}
