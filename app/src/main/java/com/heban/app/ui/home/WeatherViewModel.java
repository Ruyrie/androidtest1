package com.heban.app.ui.home;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.WeatherRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/** H02 天气详情：实时、逐小时、农事指数、未来 7 天；下拉刷新；切换地点见 CityPickerViewModel。 */
public class WeatherViewModel extends BaseViewModel {
    public final MutableLiveData<WeatherStateUi> state =
            new MutableLiveData<>(new WeatherStateUi(WeatherStateUi.loading()));
    public final MutableLiveData<Boolean> refreshing = new MutableLiveData<>(false);

    private final WeatherRepository repo;

    public WeatherViewModel(AppContainer c) {
        this(c, c.weatherRepository);
    }

    @VisibleForTesting
    WeatherViewModel(AppContainer c, WeatherRepository repo) {
        super(c);
        this.repo = repo;
        add(repo.observe()
                .onErrorReturn(WeatherStateUi::failed)
                .map(WeatherStateUi::new)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state::setValue, e -> toast.setValue(AppError.from(e).message)));
    }

    /** 下拉刷新 / 重试：忽略 30 分钟缓存 */
    public void refresh() {
        refreshing.setValue(true);
        add(repo.refresh(true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> refreshing.setValue(false), e -> {
                    refreshing.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }
}
