package com.heban.app.ui.discover.pest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.PestRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * D04 病虫害百科：搜索框 debounce(300ms) + distinctUntilChanged + switchMap 查询本地百科（技术准备文档 12.4），
 * 与类别 Chip（全部 / 病害 / 虫害 / 生理性）、作物下拉组合；「本季高发」横幅取当月第一条。
 */
public class PestsViewModel extends BaseViewModel {
    static final long SEARCH_DEBOUNCE_MS = 300;
    /** 类别「全部」 */
    public static final String KIND_ALL = "";

    private final PestRepository repo;
    private final BehaviorProcessor<String> query = BehaviorProcessor.createDefault("");
    private final BehaviorProcessor<String> kind = BehaviorProcessor.createDefault(KIND_ALL);
    /** "" 为全部作物 */
    private final BehaviorProcessor<String> crop = BehaviorProcessor.createDefault("");

    /** 搜索框（双向绑定），每次修改推给 query */
    public final MutableLiveData<String> search = new MutableLiveData<String>("") {
        @Override
        public void setValue(String value) {
            super.setValue(value);
            query.onNext(value == null ? "" : value);
        }
    };
    /** 当前类别：KIND_ALL 或 PestEntity.KIND_* */
    public final MutableLiveData<String> selectedKind = new MutableLiveData<>(KIND_ALL);
    /** 当前作物，null 为全部作物 */
    public final MutableLiveData<String> selectedCrop = new MutableLiveData<>(null);
    public final MutableLiveData<List<String>> cropOptions = new MutableLiveData<>(Collections.emptyList());

    public final MutableLiveData<List<PestEntity>> pests = new MutableLiveData<>(Collections.emptyList());
    /** 筛选后的条数（「共 N 条」） */
    public final MutableLiveData<Integer> resultCount = new MutableLiveData<>(0);
    /** 「本季高发」横幅（当月第一条），没有时为 null */
    public final MutableLiveData<PestEntity> banner = new MutableLiveData<>(null);
    /** 有横幅且没有输入关键字时显示 */
    public final MutableLiveData<Boolean> showBanner = new MutableLiveData<>(false);
    /** 查询完成且没有结果 */
    public final MutableLiveData<Boolean> showNoResult = new MutableLiveData<>(false);
    /** 没有结果时：true = 有筛选条件或关键字（「没有找到相关病虫害」），false = 百科本身为空 */
    public final MutableLiveData<Boolean> filtered = new MutableLiveData<>(false);

    /** 从 D01 搜索栏进入时，只在第一次打开时自动聚焦搜索框 */
    private boolean focusHandled;
    private boolean hasKeyword;

    public PestsViewModel(AppContainer c) {
        this(c, c.pestRepository, Schedulers.computation(), LocalDate.now());
    }

    PestsViewModel(@Nullable AppContainer c, @NonNull PestRepository repo, @NonNull Scheduler debounceScheduler,
                   @NonNull LocalDate today) {
        super(c);
        this.repo = repo;

        // 清空关键字时立即生效，输入时等 300ms 停顿
        Flowable<String> keyword = query.map(String::trim)
                .debounce(q -> q.isEmpty()
                        ? Flowable.<Long>empty()
                        : Flowable.timer(SEARCH_DEBOUNCE_MS, TimeUnit.MILLISECONDS, debounceScheduler))
                .distinctUntilChanged();

        add(Flowable.combineLatest(keyword, kind, crop, Filter::new)
                .distinctUntilChanged()
                .switchMap(f -> repo.search(f.keywordOrNull(), f.kindOrNull(), f.cropOrNull())
                        .map(list -> new Result(f, list)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onResult, this::onError));

        add(repo.seasonal(today.getMonthValue())
                .map(list -> list.isEmpty() ? Optional.<PestEntity>empty() : Optional.of(list.get(0)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(p -> {
                    banner.setValue(p.orElse(null));
                    updateBanner();
                }, this::onError));

        add(repo.cropOptions()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cropOptions::setValue, this::onError));
    }

    /** 点击类别 Chip：KIND_ALL 或 PestEntity.KIND_* */
    public void selectKind(@NonNull String k) {
        if (k.equals(selectedKind.getValue())) return;
        selectedKind.setValue(k);
        kind.onNext(k);
    }

    /** 作物下拉：null 为全部作物 */
    public void selectCrop(@Nullable String c) {
        String v = c == null || c.trim().isEmpty() ? null : c.trim();
        if (Objects.equals(v, selectedCrop.getValue())) return;
        selectedCrop.setValue(v);
        crop.onNext(v == null ? "" : v);
    }

    public void clearSearch() {
        search.setValue("");
    }

    /** 是否需要自动聚焦搜索框（只返回一次 true） */
    public boolean consumeFocusRequest(boolean requested) {
        if (!requested || focusHandled) return false;
        focusHandled = true;
        return true;
    }

    private void onResult(Result r) {
        pests.setValue(r.list);
        resultCount.setValue(r.list.size());
        filtered.setValue(!r.filter.isUnfiltered());
        showNoResult.setValue(r.list.isEmpty());
        hasKeyword = !r.filter.keyword.isEmpty();
        updateBanner();
    }

    private void updateBanner() {
        showBanner.setValue(banner.getValue() != null && !hasKeyword);
    }

    private void onError(Throwable e) {
        toast.setValue(AppError.from(e).message);
    }

    /** 当前查询条件 */
    static final class Filter {
        final String keyword;
        final String kind;
        final String crop;

        Filter(String keyword, String kind, String crop) {
            this.keyword = keyword;
            this.kind = kind;
            this.crop = crop;
        }

        @Nullable
        String keywordOrNull() {
            return keyword.isEmpty() ? null : keyword;
        }

        @Nullable
        String kindOrNull() {
            return kind.isEmpty() ? null : kind;
        }

        @Nullable
        String cropOrNull() {
            return crop.isEmpty() ? null : crop;
        }

        boolean isUnfiltered() {
            return keyword.isEmpty() && kind.isEmpty() && crop.isEmpty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Filter)) return false;
            Filter f = (Filter) o;
            return keyword.equals(f.keyword) && kind.equals(f.kind) && crop.equals(f.crop);
        }

        @Override
        public int hashCode() {
            return Objects.hash(keyword, kind, crop);
        }
    }

    private static final class Result {
        final Filter filter;
        final List<PestEntity> list;

        Result(Filter filter, List<PestEntity> list) {
            this.filter = filter;
            this.list = list;
        }
    }
}
