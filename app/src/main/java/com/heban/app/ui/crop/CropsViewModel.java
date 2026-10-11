package com.heban.app.ui.crop;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.CropStats;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * C01 我的作物：筛选 Chip 用 BehaviorProcessor 保存当前筛选，switchMap 切换查询（技术准备文档 12.3）；
 * 搜索框按名称 / 品种 / 地块过滤当前列表（debounce 300ms，清空时立即生效）。
 */
public class CropsViewModel extends BaseViewModel {
    static final long SEARCH_DEBOUNCE_MS = 300;

    private final CropRepository repo;
    private final BehaviorProcessor<String> filter = BehaviorProcessor.createDefault(CropRepository.FILTER_ALL);
    private final BehaviorProcessor<String> query = BehaviorProcessor.createDefault("");

    /** 搜索框（双向绑定），每次修改推给 query */
    public final MutableLiveData<String> search = new MutableLiveData<String>("") {
        @Override
        public void setValue(String value) {
            super.setValue(value);
            query.onNext(value == null ? "" : value);
        }
    };
    public final LiveData<Boolean> hasQuery = Transformations.map(search, s -> s != null && !s.isEmpty());

    public final MutableLiveData<String> selectedFilter = new MutableLiveData<>(CropRepository.FILTER_ALL);
    /** 当前筛选 + 搜索后的卡片 */
    public final MutableLiveData<List<CropCard>> cards = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<CropStats> stats = new MutableLiveData<>(new CropStats());
    /** 全部作物数（「全部 N」） */
    public final MutableLiveData<Integer> totalCount = new MutableLiveData<>(0);

    /** 有作物：显示搜索、筛选和统计 */
    public final MutableLiveData<Boolean> hasCrops = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> showList = new MutableLiveData<>(false);
    /** 一个作物都没有 */
    public final MutableLiveData<Boolean> showEmpty = new MutableLiveData<>(false);
    /** 有作物，但筛选 / 搜索没有结果 */
    public final MutableLiveData<Boolean> showNoMatch = new MutableLiveData<>(false);

    private boolean cardsLoaded;
    private boolean totalLoaded;

    public CropsViewModel(AppContainer c) {
        this(c, c.cropRepository, Schedulers.computation());
    }

    CropsViewModel(@Nullable AppContainer c, @NonNull CropRepository repo, @NonNull Scheduler debounceScheduler) {
        super(c);
        this.repo = repo;

        Flowable<List<CropCard>> filtered = filter.distinctUntilChanged().switchMap(repo::observeCropCards);
        Flowable<String> keyword = query.map(String::trim).distinctUntilChanged()
                .debounce(q -> q.isEmpty()
                        ? Flowable.<Long>empty()
                        : Flowable.timer(SEARCH_DEBOUNCE_MS, TimeUnit.MILLISECONDS, debounceScheduler));
        add(Flowable.combineLatest(filtered, keyword, CropsFormat::search)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    cards.setValue(list);
                    cardsLoaded = true;
                    updateVisibility();
                }, this::onError));

        add(repo.observeCropCards(CropRepository.FILTER_ALL)
                .map(List::size)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(n -> {
                    totalCount.setValue(n);
                    totalLoaded = true;
                    updateVisibility();
                }, this::onError));

        add(repo.observeCropStats()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stats::setValue, this::onError));
    }

    /** 点击筛选 Chip：CropRepository.FILTER_* */
    public void selectFilter(@NonNull String f) {
        if (f.equals(selectedFilter.getValue())) return;
        selectedFilter.setValue(f);
        filter.onNext(f);
    }

    public void clearSearch() {
        search.setValue("");
    }

    private void updateVisibility() {
        if (!cardsLoaded || !totalLoaded) return;
        Integer total = totalCount.getValue();
        boolean any = total != null && total > 0;
        List<CropCard> list = cards.getValue();
        boolean listEmpty = list == null || list.isEmpty();
        hasCrops.setValue(any);
        showEmpty.setValue(!any);
        showList.setValue(any && !listEmpty);
        showNoMatch.setValue(any && listEmpty);
    }

    private void onError(Throwable e) {
        toast.setValue(AppError.from(e).message);
    }
}
