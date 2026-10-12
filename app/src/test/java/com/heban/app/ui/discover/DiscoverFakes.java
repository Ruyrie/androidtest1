package com.heban.app.ui.discover;

import androidx.annotation.Nullable;
import androidx.paging.PagingData;

import com.heban.app.data.local.entity.FavoriteEntity;
import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.model.CategoryShare;
import com.heban.app.data.model.LedgerDay;
import com.heban.app.data.model.LedgerMonthSummary;
import com.heban.app.data.model.MonthBalance;
import com.heban.app.data.repository.FavoriteRepository;
import com.heban.app.data.repository.LedgerRepository;
import com.heban.app.data.repository.MarketRepository;
import com.heban.app.data.repository.PestRepository;

import java.time.YearMonth;
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
import io.reactivex.rxjava3.processors.PublishProcessor;

/** D01 / D04 / D05 ViewModel 测试用的假数据源。 */
public final class DiscoverFakes {
    private DiscoverFakes() {
    }

    public static PestEntity pest(long id, String name, String kind, String category, String cropTags,
                                  String level) {
        PestEntity p = new PestEntity();
        p.id = id;
        p.name = name;
        p.kind = kind;
        p.category = category;
        p.cropTags = cropTags;
        p.seasonLevel = level;
        return p;
    }

    public static ProductEntity product(long id, String title, long priceCent, String unit, String seller,
                                        double km) {
        ProductEntity p = new ProductEntity();
        p.id = id;
        p.title = title;
        p.priceCent = priceCent;
        p.unit = unit;
        p.sellerName = seller;
        p.distanceKm = km;
        return p;
    }

    /** search 每次调用记录参数并返回一个新的 PublishProcessor，便于验证 switchMap 取消旧查询 */
    public static class Pests implements PestRepository {
        public static final class Call {
            public final String keyword;
            public final String kind;
            public final String crop;
            public final PublishProcessor<List<PestEntity>> results = PublishProcessor.create();
            public boolean cancelled;

            Call(String keyword, String kind, String crop) {
                this.keyword = keyword;
                this.kind = kind;
                this.crop = crop;
            }
        }

        public final List<Call> calls = new ArrayList<>();
        public final BehaviorProcessor<List<PestEntity>> seasonal = BehaviorProcessor.createDefault(Collections.emptyList());
        public final BehaviorProcessor<Integer> count = BehaviorProcessor.createDefault(0);
        public final Map<Long, PestEntity> byId = new HashMap<>();
        public List<String> crops = Collections.emptyList();
        public int seasonalMonth = -1;

        public Call last() {
            return calls.get(calls.size() - 1);
        }

        @Override
        public Flowable<List<PestEntity>> search(@Nullable String keyword, @Nullable String kind, @Nullable String crop) {
            Call c = new Call(keyword, kind, crop);
            calls.add(c);
            return c.results.doOnCancel(() -> c.cancelled = true);
        }

        @Override
        public Flowable<List<PestEntity>> seasonal(int month) {
            seasonalMonth = month;
            return seasonal;
        }

        @Override
        public Flowable<Integer> count() {
            return count;
        }

        @Override
        public Single<PestEntity> get(long id) {
            PestEntity p = byId.get(id);
            return p == null ? Single.error(new IllegalStateException("not found")) : Single.just(p);
        }

        @Override
        public Maybe<PestEntity> matchByName(String name) {
            return Maybe.empty();
        }

        @Override
        public Single<List<String>> cropOptions() {
            return Single.just(crops);
        }
    }

    public static final class Market implements MarketRepository {
        public final BehaviorProcessor<Integer> count = BehaviorProcessor.createDefault(0);
        public final BehaviorProcessor<List<ProductEntity>> nearby = BehaviorProcessor.createDefault(Collections.emptyList());
        public String countCategory = "unset";
        public int nearbyLimit = -1;

        @Override
        public Flowable<PagingData<ProductEntity>> products(@Nullable String category, String sort) {
            return Flowable.never();
        }

        @Override
        public Flowable<Integer> count(@Nullable String category) {
            countCategory = category;
            return count;
        }

        @Override
        public Flowable<List<ProductEntity>> nearby(int limit) {
            nearbyLimit = limit;
            return nearby;
        }

        @Override
        public Flowable<ProductEntity> observeProduct(long id) {
            return Flowable.never();
        }

        @Override
        public Completable addView(long id) {
            return Completable.complete();
        }

        @Override
        public Single<Long> publish(ProductEntity product, boolean pin24h) {
            return Single.never();
        }

        @Override
        public Completable delete(long id) {
            return Completable.complete();
        }

        @Override
        public Flowable<List<ProductEntity>> observeMine() {
            return Flowable.never();
        }
    }

    public static final class Ledger implements LedgerRepository {
        public final BehaviorProcessor<LedgerMonthSummary> month = BehaviorProcessor.create();
        public YearMonth requested;

        public static LedgerMonthSummary summary(long incomeCent, long expenseCent) {
            LedgerMonthSummary s = new LedgerMonthSummary();
            s.incomeCent = incomeCent;
            s.expenseCent = expenseCent;
            return s;
        }

        @Override
        public Single<Long> addEntry(LedgerEntryEntity entry) {
            return Single.never();
        }

        @Override
        public Completable deleteEntry(long entryId) {
            return Completable.complete();
        }

        @Override
        public Single<LedgerEntryEntity> getEntry(long entryId) {
            return Single.never();
        }

        @Override
        public Flowable<LedgerMonthSummary> observeMonth(YearMonth m) {
            requested = m;
            return month;
        }

        @Override
        public Flowable<List<MonthBalance>> observeRecentBalances(YearMonth endMonth, int months) {
            return Flowable.never();
        }

        @Override
        public Flowable<List<CategoryShare>> observeExpenseMix(YearMonth m) {
            return Flowable.never();
        }

        @Override
        public Flowable<List<LedgerDay>> observeDays(YearMonth m) {
            return Flowable.never();
        }
    }

    public static final class Favorites implements FavoriteRepository {
        public final BehaviorProcessor<Boolean> state = BehaviorProcessor.createDefault(false);
        public String observedType;
        public long observedId = -1;
        public int toggles;
        public Throwable toggleError;

        @Override
        public Flowable<Boolean> observeIsFavorite(String targetType, long targetId) {
            observedType = targetType;
            observedId = targetId;
            return state;
        }

        @Override
        public Completable toggle(String targetType, long targetId) {
            if (toggleError != null) return Completable.error(toggleError);
            toggles++;
            state.onNext(!state.getValue());
            return Completable.complete();
        }

        @Override
        public Flowable<List<FavoriteEntity>> observeAll(String targetType) {
            return Flowable.never();
        }
    }
}
