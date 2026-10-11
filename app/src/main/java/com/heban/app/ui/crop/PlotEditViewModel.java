package com.heban.app.ui.crop;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.R;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/** C05 添加 / 编辑地块（底部弹窗）：名称、面积、类型、棚型、灌溉、土壤、备注。 */
public class PlotEditViewModel extends BaseViewModel {
    private final CropRepository repo;
    private boolean started;
    @Nullable
    private PlotEntity editing;

    public final MutableLiveData<String> name = new MutableLiveData<>("");
    public final MutableLiveData<String> area = new MutableLiveData<>("");
    public final MutableLiveData<String> type = new MutableLiveData<>(PlotEntity.TYPE_GREENHOUSE);
    public final MutableLiveData<String> structure = new MutableLiveData<>("");
    public final MutableLiveData<String> irrigation = new MutableLiveData<>("");
    public final MutableLiveData<String> soil = new MutableLiveData<>("");
    public final MutableLiveData<String> note = new MutableLiveData<>("");
    public final MutableLiveData<Boolean> isEdit = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    /** 字段错误提示（字符串资源，0 表示无） */
    public final MutableLiveData<Integer> nameError = new MutableLiveData<>(0);
    public final MutableLiveData<Integer> areaError = new MutableLiveData<>(0);
    public final SingleLiveEvent<Boolean> saved = new SingleLiveEvent<>();

    public PlotEditViewModel(AppContainer c) {
        this(c, c.cropRepository);
    }

    PlotEditViewModel(@Nullable AppContainer c, @NonNull CropRepository repo) {
        super(c);
        this.repo = repo;
    }

    /** plotId 为 0 时新增，否则读取该地块填入表单（只执行一次） */
    public void start(long plotId) {
        if (started) return;
        started = true;
        if (plotId == 0) return;
        isEdit.setValue(true);
        add(repo.observePlots()
                .firstOrError()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> fill(find(list, plotId)), this::onError));
    }

    @Nullable
    private static PlotEntity find(List<PlotEntity> list, long id) {
        for (PlotEntity p : list) if (p.id == id) return p;
        return null;
    }

    private void fill(@Nullable PlotEntity p) {
        if (p == null) return;
        editing = p;
        name.setValue(p.name);
        area.setValue(p.areaMu > 0 ? CropsFormat.amount(p.areaMu) : "");
        type.setValue(PlotEntity.TYPE_OPEN.equals(p.type) ? PlotEntity.TYPE_OPEN : PlotEntity.TYPE_GREENHOUSE);
        structure.setValue(nz(p.structure));
        irrigation.setValue(nz(p.irrigation));
        soil.setValue(nz(p.soil));
        note.setValue(nz(p.note));
    }

    public void selectType(@NonNull String t) {
        if (!t.equals(type.getValue())) type.setValue(t);
    }

    public void save() {
        if (Boolean.TRUE.equals(saving.getValue())) return;
        String n = trim(name.getValue());
        Double a = parseArea(area.getValue());
        nameError.setValue(n.isEmpty() ? R.string.plots_error_name : 0);
        areaError.setValue(a == null ? R.string.plots_error_area : 0);
        if (n.isEmpty() || a == null) return;

        PlotEntity p = editing != null ? editing : new PlotEntity();
        p.name = n;
        p.areaMu = a;
        p.type = PlotEntity.TYPE_OPEN.equals(type.getValue()) ? PlotEntity.TYPE_OPEN : PlotEntity.TYPE_GREENHOUSE;
        p.structure = emptyToNull(structure.getValue());
        p.irrigation = emptyToNull(irrigation.getValue());
        p.soil = emptyToNull(soil.getValue());
        p.note = emptyToNull(note.getValue());
        saving.setValue(true);
        add(repo.savePlot(p)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(id -> {
                    saving.setValue(false);
                    saved.setValue(true);
                }, e -> {
                    saving.setValue(false);
                    onError(e);
                }));
    }

    @Nullable
    static Double parseArea(@Nullable String text) {
        try {
            double v = Double.parseDouble(trim(text));
            return v > 0 && v < 100000 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @NonNull
    private static String trim(@Nullable String s) {
        return s == null ? "" : s.trim();
    }

    @Nullable
    private static String emptyToNull(@Nullable String s) {
        String t = trim(s);
        return t.isEmpty() ? null : t;
    }

    @NonNull
    private static String nz(@Nullable String s) {
        return s == null ? "" : s;
    }

    private void onError(Throwable e) {
        toast.setValue(AppError.from(e).message);
    }
}
