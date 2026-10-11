package com.heban.app.ui.crop;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.R;
import com.heban.app.data.model.PlotCard;
import com.heban.app.data.model.PlotStats;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/** C05 地块管理：地块卡片、统计（地块数 / 总面积 / 利用率），删除地块、记录棚内温湿度。 */
public class PlotsViewModel extends BaseViewModel {
    public static final double TEMP_MIN = -30;
    public static final double TEMP_MAX = 60;

    private final CropRepository repo;

    public final MutableLiveData<List<PlotCard>> plots = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<PlotStats> stats = new MutableLiveData<>(new PlotStats());
    public final MutableLiveData<Boolean> loaded = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> hasPlots = new MutableLiveData<>(false);
    public final SingleLiveEvent<Integer> toastRes = new SingleLiveEvent<>();

    public PlotsViewModel(AppContainer c) {
        this(c, c.cropRepository);
    }

    PlotsViewModel(@Nullable AppContainer c, @NonNull CropRepository repo) {
        super(c);
        this.repo = repo;
        add(repo.observePlotCards()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    plots.setValue(list);
                    hasPlots.setValue(!list.isEmpty());
                    loaded.setValue(true);
                }, this::onError));
        add(repo.observePlotStats()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stats::setValue, this::onError));
    }

    public void deletePlot(long plotId) {
        add(repo.deletePlot(plotId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> toastRes.setValue(R.string.plots_deleted), this::onError));
    }

    /**
     * 记录棚内温湿度；输入不合法时返回错误提示的字符串资源（温度 -30–60℃，湿度 0–100%），合法时返回 0 并保存。
     */
    public int recordReading(long plotId, @Nullable String tempText, @Nullable String humidityText) {
        Double temp = parseTemp(tempText);
        if (temp == null) return R.string.plots_error_temp;
        Integer humidity = parseHumidity(humidityText);
        if (humidity == null) return R.string.plots_error_humidity;
        add(repo.recordPlotReading(plotId, temp, humidity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> toastRes.setValue(R.string.plots_saved), this::onError));
        return 0;
    }

    @Nullable
    static Double parseTemp(@Nullable String text) {
        try {
            double v = Double.parseDouble(text == null ? "" : text.trim());
            return v >= TEMP_MIN && v <= TEMP_MAX ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    static Integer parseHumidity(@Nullable String text) {
        try {
            int v = (int) Math.round(Double.parseDouble(text == null ? "" : text.trim()));
            return v >= 0 && v <= 100 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void onError(Throwable e) {
        toast.setValue(AppError.from(e).message);
    }
}
