package com.heban.app.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.data.model.CityOption;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.WeatherRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * H02 切换地点（技术准备文档 12.2）：城市搜索 debounce 300ms + distinctUntilChanged + switchMap，
 * 选择后保存地点，天气由 WeatherRepository.observe() 重新发射。
 */
public class CityPickerViewModel extends BaseViewModel {
    public static final long DEBOUNCE_MS = 300;

    public enum Status { IDLE, SEARCHING, RESULTS, EMPTY, ERROR }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.IDLE);
    public final MutableLiveData<List<CityOption>> results = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<String> errorText = new MutableLiveData<>("");
    public final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    /** 地点已保存，关闭弹窗 */
    public final SingleLiveEvent<Boolean> done = new SingleLiveEvent<>();

    private final WeatherRepository repo;
    private final PublishProcessor<String> queries = PublishProcessor.create();

    public CityPickerViewModel(AppContainer c) {
        this(c, c.weatherRepository, Schedulers.computation());
    }

    @VisibleForTesting
    CityPickerViewModel(AppContainer c, WeatherRepository repo, Scheduler debounceScheduler) {
        super(c);
        this.repo = repo;
        add(queries
                .debounce(DEBOUNCE_MS, TimeUnit.MILLISECONDS, debounceScheduler)
                .distinctUntilChanged()
                .switchMap(this::search)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::apply, e -> toast.setValue(AppError.from(e).message)));
    }

    /** 搜索框内容变化 */
    public void onQueryChanged(@Nullable CharSequence text) {
        queries.onNext(text == null ? "" : text.toString().trim());
    }

    public void select(@NonNull CityOption city) {
        if (Boolean.TRUE.equals(saving.getValue())) return;
        saving.setValue(true);
        add(repo.setLocation(city)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    saving.setValue(false);
                    done.setValue(true);
                }, e -> {
                    saving.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }

    private Flowable<Result> search(String keyword) {
        if (keyword.isEmpty()) return Flowable.just(Result.idle());
        return repo.searchCity(keyword)
                .map(Result::of)
                .onErrorReturn(Result::failed)
                .toFlowable()
                .startWithItem(Result.searching());
    }

    private void apply(Result r) {
        status.setValue(r.status);
        results.setValue(r.cities);
        errorText.setValue(r.error);
    }

    private static final class Result {
        final Status status;
        final List<CityOption> cities;
        final String error;

        Result(Status status, List<CityOption> cities, String error) {
            this.status = status;
            this.cities = cities;
            this.error = error;
        }

        static Result idle() {
            return new Result(Status.IDLE, Collections.emptyList(), "");
        }

        static Result searching() {
            return new Result(Status.SEARCHING, Collections.emptyList(), "");
        }

        static Result of(List<CityOption> cities) {
            if (cities == null || cities.isEmpty()) return new Result(Status.EMPTY, Collections.emptyList(), "");
            return new Result(Status.RESULTS, cities, "");
        }

        static Result failed(Throwable t) {
            return new Result(Status.ERROR, Collections.emptyList(), AppError.from(t).message);
        }
    }
}
