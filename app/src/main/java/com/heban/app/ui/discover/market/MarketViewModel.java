package com.heban.app.ui.discover.market;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelKt;
import androidx.paging.PagingData;
import androidx.paging.PagingDataTransforms;
import androidx.paging.rxjava3.PagingRx;

import com.heban.app.data.local.entity.FavoriteEntity;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.FavoriteRepository;
import com.heban.app.data.repository.MarketRepository;
import com.heban.app.data.repository.UserRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * D07 农产品集市（技术准备文档 7.6）：分类 + 排序保存在 BehaviorProcessor 中，switchMap 切换分页查询，
 * PagingRx.cachedIn 缓存到 viewModelScope；搜索关键字和「只看收藏」在缓存之后用
 * PagingDataTransforms.filter 过滤当前分页流（不重新查询）。
 */
public class MarketViewModel extends BaseViewModel {
    public static final String CAT_ALL = "全部";
    static final long SEARCH_DEBOUNCE_MS = 300;

    private final MarketRepository repo;
    private final BehaviorProcessor<Query> query =
            BehaviorProcessor.createDefault(new Query(CAT_ALL, MarketRepository.SORT_DISTANCE));
    private final BehaviorProcessor<String> keyword = BehaviorProcessor.createDefault("");
    private final BehaviorProcessor<Boolean> favoritesOnly = BehaviorProcessor.createDefault(false);
    /** 过滤谓词在计算线程执行（测试中 RxJavaPlugins 替换为 trampoline） */
    private final Executor filterExecutor = r -> Schedulers.computation().scheduleDirect(r);

    /** 当前分类 + 排序 + 搜索 + 收藏过滤后的分页数据，Fragment 订阅后交给 PagingDataAdapter */
    public final Flowable<PagingData<ProductEntity>> products;

    public final MutableLiveData<String> category = new MutableLiveData<>(CAT_ALL);
    public final MutableLiveData<String> sort = new MutableLiveData<>(MarketRepository.SORT_DISTANCE);
    /** 「江宁区附近 · N 件」中的 N（当前分类的在售数量） */
    public final MutableLiveData<Integer> count = new MutableLiveData<>(0);
    /** 「江宁区」：资料中的地区最后一级；游客或未填写时为 null */
    public final MutableLiveData<String> district = new MutableLiveData<>(null);
    public final MutableLiveData<Boolean> favoritesOnlyOn = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> searchOpen = new MutableLiveData<>(false);

    /** 搜索框（双向绑定），每次修改推给 keyword */
    public final MutableLiveData<String> search = new MutableLiveData<String>("") {
        @Override
        public void setValue(String value) {
            super.setValue(value);
            keyword.onNext(value == null ? "" : value);
        }
    };

    public MarketViewModel(AppContainer c) {
        super(c);
        this.repo = c.marketRepository;
        this.products = build(c.favoriteRepository, Schedulers.computation(),
                src -> PagingRx.cachedIn(src, ViewModelKt.getViewModelScope(this)));
        observeCountAndRegion(c.userRepository);
    }

    /** 测试用：cache 为 identity 时不经过协程缓存 */
    MarketViewModel(@NonNull MarketRepository repo, @NonNull FavoriteRepository favorites,
                    @Nullable UserRepository users, @NonNull Scheduler debounceScheduler,
                    @NonNull UnaryOperator<Flowable<PagingData<ProductEntity>>> cache) {
        super(null);
        this.repo = repo;
        this.products = build(favorites, debounceScheduler, cache);
        observeCountAndRegion(users);
    }

    private Flowable<PagingData<ProductEntity>> build(FavoriteRepository favorites, Scheduler debounceScheduler,
                                                      UnaryOperator<Flowable<PagingData<ProductEntity>>> cache) {
        Flowable<PagingData<ProductEntity>> source = query.distinctUntilChanged()
                .switchMap(q -> repo.products(q.categoryFilter(), q.sort));
        Flowable<PagingData<ProductEntity>> cached = cache.apply(source);

        Flowable<String> words = keyword.map(String::trim).distinctUntilChanged()
                .debounce(k -> k.isEmpty()
                        ? Flowable.<Long>empty()
                        : Flowable.timer(SEARCH_DEBOUNCE_MS, TimeUnit.MILLISECONDS, debounceScheduler));
        Flowable<Set<Long>> favIds = favoritesOnly.distinctUntilChanged().switchMap(on -> on
                ? favorites.observeAll(FavoriteEntity.TYPE_PRODUCT).map(MarketViewModel::ids)
                : Flowable.just(Filter.ANY_IDS));
        Flowable<Filter> filter = Flowable.combineLatest(words, favIds, Filter::new).distinctUntilChanged();

        return Flowable.combineLatest(cached, filter, (data, f) ->
                PagingDataTransforms.filter(data, filterExecutor, f::test));
    }

    private void observeCountAndRegion(@Nullable UserRepository users) {
        add(query.map(q -> q.category).distinctUntilChanged()
                .switchMap(cat -> repo.count(CAT_ALL.equals(cat) ? null : cat))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count::setValue, this::onError));
        if (users != null) {
            add(users.currentUser()
                    .map(u -> {
                        String d = MarketFormat.district(u.region);
                        return d == null ? "" : d;
                    })
                    .distinctUntilChanged()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(d -> district.setValue(d.isEmpty() ? null : d), this::onError));
        }
    }

    /** 点击分类 Chip：全部 / 农产品 / 农资 / 种苗 / 农机具 */
    public void selectCategory(@NonNull String cat) {
        if (cat.equals(category.getValue())) return;
        category.setValue(cat);
        query.onNext(new Query(cat, sortValue()));
    }

    /** 排序：MarketRepository.SORT_* */
    public void selectSort(@NonNull String s) {
        if (s.equals(sort.getValue())) return;
        sort.setValue(s);
        query.onNext(new Query(categoryValue(), s));
    }

    /** 顶栏收藏图标：只看已收藏的商品 / 恢复全部 */
    public void toggleFavoritesOnly() {
        boolean on = !Boolean.TRUE.equals(favoritesOnlyOn.getValue());
        favoritesOnlyOn.setValue(on);
        favoritesOnly.onNext(on);
    }

    /** 是否输入了搜索关键字（空状态文字用） */
    public boolean hasKeyword() {
        String s = search.getValue();
        return s != null && !s.trim().isEmpty();
    }

    public void openSearch() {
        searchOpen.setValue(true);
    }

    /** 关闭搜索：清空关键字 */
    public void closeSearch() {
        search.setValue("");
        searchOpen.setValue(false);
    }

    private String categoryValue() {
        String c = category.getValue();
        return c == null ? CAT_ALL : c;
    }

    private String sortValue() {
        String s = sort.getValue();
        return s == null ? MarketRepository.SORT_DISTANCE : s;
    }

    private static Set<Long> ids(List<FavoriteEntity> list) {
        Set<Long> out = new HashSet<>();
        for (FavoriteEntity f : list) out.add(f.targetId);
        return out;
    }

    private void onError(Throwable e) {
        toast.setValue(AppError.from(e).message);
    }

    /** 分类 + 排序 */
    static final class Query {
        final String category;
        final String sort;

        Query(String category, String sort) {
            this.category = category;
            this.sort = sort;
        }

        /** 「全部」不过滤（传 null） */
        @Nullable
        String categoryFilter() {
            return CAT_ALL.equals(category) ? null : category;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Query)) return false;
            Query q = (Query) o;
            return category.equals(q.category) && sort.equals(q.sort);
        }

        @Override
        public int hashCode() {
            return Objects.hash(category, sort);
        }
    }

    /** 页面内过滤：关键字 + 收藏 id（ANY_IDS 表示不限） */
    static final class Filter {
        static final Set<Long> ANY_IDS = Collections.unmodifiableSet(new HashSet<>());

        final String keyword;
        final Set<Long> ids;

        Filter(String keyword, Set<Long> ids) {
            this.keyword = keyword;
            this.ids = ids;
        }

        boolean test(ProductEntity p) {
            if (ids != ANY_IDS && !ids.contains(p.id)) return false;
            return MarketFormat.matches(p, keyword);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Filter)) return false;
            Filter f = (Filter) o;
            return keyword.equals(f.keyword) && (ids == f.ids || (ids != ANY_IDS && f.ids != ANY_IDS
                    && ids.equals(f.ids)));
        }

        @Override
        public int hashCode() {
            return Objects.hash(keyword, ids.size());
        }
    }
}
