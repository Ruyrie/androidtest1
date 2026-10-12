package com.heban.app.ui.discover.pest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.heban.app.data.local.entity.FavoriteEntity;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.FavoriteRepository;
import com.heban.app.data.repository.PestRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * D05 病害详情：本地百科离线可用；图片与说明、农业 / 化学防治从 JSON 数组解析；收藏（需要登录）。
 */
public class PestDetailViewModel extends BaseViewModel {
    private final PestRepository repo;
    private final FavoriteRepository favorites;
    private final Gson gson;

    public final MutableLiveData<PestEntity> pest = new MutableLiveData<>(null);
    public final MutableLiveData<List<PestDetailImage>> images = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<List<String>> agri = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<List<String>> chem = new MutableLiveData<>(Collections.emptyList());
    /** 防治方法分段按钮：false = 农业防治，true = 化学防治 */
    public final MutableLiveData<Boolean> chemSelected = new MutableLiveData<>(false);
    /** 当前分段的措施列表 */
    public final MediatorLiveData<List<String>> steps = new MediatorLiveData<>();
    public final MutableLiveData<Boolean> favorite = new MutableLiveData<>(false);
    /** 数据已加载 */
    public final MutableLiveData<Boolean> loaded = new MutableLiveData<>(false);
    /** 没有这条百科（id 无效） */
    public final MutableLiveData<Boolean> notFound = new MutableLiveData<>(false);

    private long pestId = -1;

    public PestDetailViewModel(AppContainer c) {
        this(c, c.pestRepository, c.favoriteRepository, c.gson);
    }

    PestDetailViewModel(@Nullable AppContainer c, @NonNull PestRepository repo,
                        @NonNull FavoriteRepository favorites, @NonNull Gson gson) {
        super(c);
        this.repo = repo;
        this.favorites = favorites;
        this.gson = gson;
        steps.setValue(Collections.emptyList());
        steps.addSource(chemSelected, v -> updateSteps());
        steps.addSource(agri, v -> updateSteps());
        steps.addSource(chem, v -> updateSteps());
    }

    /** 打开页面时调用一次；labelReminder 为化学防治末尾的标签用量提醒 */
    public void start(long id, @NonNull String labelReminder) {
        if (pestId == id) return;
        pestId = id;
        add(repo.get(id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(p -> bind(p, labelReminder), e -> {
                    notFound.setValue(true);
                    loaded.setValue(false);
                }));
        add(favorites.observeIsFavorite(FavoriteEntity.TYPE_PEST, id)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(favorite::setValue, e -> favorite.setValue(false)));
    }

    public void selectChem(boolean chemical) {
        if (Boolean.valueOf(chemical).equals(chemSelected.getValue())) return;
        chemSelected.setValue(chemical);
    }

    /** 收藏 / 取消收藏（Fragment 先检查登录） */
    public void toggleFavorite() {
        if (pestId < 0) return;
        add(favorites.toggle(FavoriteEntity.TYPE_PEST, pestId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                }, this::onError));
    }

    @Nullable
    public String name() {
        PestEntity p = pest.getValue();
        return p == null ? null : p.name;
    }

    private void bind(PestEntity p, String labelReminder) {
        images.setValue(PestDetailImage.zip(PestsFormat.jsonList(gson, p.images),
                PestsFormat.jsonList(gson, p.imageCaptions)));
        agri.setValue(PestsFormat.jsonList(gson, p.preventionAgri));
        chem.setValue(PestsFormat.withLabelReminder(PestsFormat.jsonList(gson, p.preventionChem), labelReminder));
        pest.setValue(p);
        notFound.setValue(false);
        loaded.setValue(true);
    }

    private void updateSteps() {
        boolean c = Boolean.TRUE.equals(chemSelected.getValue());
        List<String> list = c ? chem.getValue() : agri.getValue();
        steps.setValue(list == null ? Collections.emptyList() : list);
    }

    private void onError(Throwable e) {
        toast.setValue(AppError.from(e).message);
    }
}
