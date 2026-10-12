package com.heban.app.ui.discover.market;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.heban.app.data.local.entity.FavoriteEntity;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.FavoriteRepository;
import com.heban.app.data.repository.MarketRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * D08 商品详情：观察商品与收藏状态；「N 人想要」= wants + 1（当前用户已收藏时）；
 * 每次打开详情浏览数 + 1（同一 ViewModel 只计一次）；自己发布的商品可删除。
 */
public class ProductViewModel extends BaseViewModel {
    private final MarketRepository repo;
    private final FavoriteRepository favorites;
    private final Gson gson;

    public final MutableLiveData<ProductEntity> product = new MutableLiveData<>();
    public final MutableLiveData<Boolean> favorite = new MutableLiveData<>(false);
    /** 「23 人想要」 */
    public final MutableLiveData<Integer> wants = new MutableLiveData<>(0);
    public final MutableLiveData<Boolean> mine = new MutableLiveData<>(false);
    public final MutableLiveData<List<String>> images = new MutableLiveData<>(Collections.emptyList());
    /** 详情页特征标签（产地直供 / 可自提 / 同城配送 / 附种植记录 …） */
    public final MutableLiveData<List<String>> features = new MutableLiveData<>(Collections.emptyList());
    public final SingleLiveEvent<Boolean> deleted = new SingleLiveEvent<>();

    private long productId;
    private boolean started;
    private boolean deleting;

    public ProductViewModel(AppContainer c) {
        this(c.marketRepository, c.favoriteRepository, c.gson);
    }

    ProductViewModel(@NonNull MarketRepository repo, @NonNull FavoriteRepository favorites, @NonNull Gson gson) {
        super(null);
        this.repo = repo;
        this.favorites = favorites;
        this.gson = gson;
    }

    /** 打开详情：只在第一次调用时订阅并计一次浏览 */
    public void start(long id) {
        if (started) return;
        started = true;
        productId = id;
        add(repo.observeProduct(id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onProduct, this::onError));
        add(favorites.observeIsFavorite(FavoriteEntity.TYPE_PRODUCT, id)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(f -> {
                    favorite.setValue(f);
                    updateWants();
                }, this::onError));
        add(repo.addView(id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> { }, e -> { }));   // 浏览数失败不打扰用户
    }

    private void onProduct(@Nullable ProductEntity p) {
        if (p == null) return;
        product.setValue(p);
        mine.setValue(p.mine);
        images.setValue(MarketFormat.jsonList(gson, p.images));
        features.setValue(MarketFormat.jsonList(gson, p.tags));
        updateWants();
    }

    private void updateWants() {
        ProductEntity p = product.getValue();
        int base = p == null ? 0 : p.wants;
        wants.setValue(base + (Boolean.TRUE.equals(favorite.getValue()) ? 1 : 0));
    }

    /** 收藏 / 取消收藏（Fragment 先检查登录） */
    public void toggleFavorite() {
        if (productId == 0) return;
        add(favorites.toggle(FavoriteEntity.TYPE_PRODUCT, productId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> { }, this::onError));
    }

    /** 删除自己发布的商品（Fragment 先确认） */
    public void delete() {
        if (productId == 0 || deleting || !Boolean.TRUE.equals(mine.getValue())) return;
        deleting = true;
        add(repo.delete(productId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> deleted.setValue(true), e -> {
                    deleting = false;
                    onError(e);
                }));
    }

    public long productId() {
        return productId;
    }

    private void onError(Throwable e) {
        toast.setValue(AppError.from(e).message);
    }
}
