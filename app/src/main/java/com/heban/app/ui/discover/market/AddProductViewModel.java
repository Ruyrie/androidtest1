package com.heban.app.ui.discover.market;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.google.gson.Gson;
import com.heban.app.R;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.data.repository.MarketRepository;
import com.heban.app.data.repository.PointsRepository;
import com.heban.app.data.repository.PointsRules;
import com.heban.app.data.repository.UserRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;
import com.heban.app.ui.crop.PhotoPickerFileStore;
import com.heban.app.ui.crop.PhotoPickerStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * D09 发布商品（原型 AddProduct.dc.html）：图片（最多 9 张，复制到 filesDir/products）、标题（≤ 30 字）、分类、
 * 起售量、价格、单位、交易方式（多选，默认自提 + 同城配送）、交易地点（默认资料中的地区）、描述、
 * 置顶 24 小时（积分 ≥ 100 才可开启，发布时扣除，技术准备文档 12.4）、附上种植记录。
 */
public class AddProductViewModel extends BaseViewModel {
    public static final int MAX_PHOTOS = 9;
    public static final int TITLE_MAX = 30;
    static final String DIR_PRODUCTS = "products";

    // 交易方式（存入 trade_ways，顿号分隔）
    public static final String WAY_PICKUP = "自提";
    public static final String WAY_LOCAL = "同城配送";
    public static final String WAY_EXPRESS = "快递";
    public static final String WAY_TALK = "面议";
    static final String[] WAYS = {WAY_PICKUP, WAY_LOCAL, WAY_EXPRESS, WAY_TALK};

    // 详情页特征标签（与 D08 图标对应）
    static final String FEATURE_PICKUP = "可自提";
    static final String FEATURE_LOCAL = "同城配送";
    static final String FEATURE_EXPRESS = "快递";
    static final String FEATURE_RECORD = "附种植记录";

    private final MarketRepository market;
    private final PhotoPickerStore photoStore;
    private final Gson gson;
    private final LongSupplier clock;

    public final MutableLiveData<List<String>> photos = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<Boolean> photoBusy = new MutableLiveData<>(false);
    public final MutableLiveData<String> title = new MutableLiveData<>("");
    public final LiveData<Integer> titleLength = Transformations.map(title, t -> t == null ? 0 : t.length());
    /** 大类（ProductEntity.CAT_*），未选择时为 null */
    public final MutableLiveData<String> category = new MutableLiveData<>(null);
    public final MutableLiveData<String> subCategory = new MutableLiveData<>(null);
    public final MutableLiveData<String> minQty = new MutableLiveData<>("");
    public final MutableLiveData<String> price = new MutableLiveData<>("");
    public final MutableLiveData<String> unit = new MutableLiveData<>("斤");
    public final MutableLiveData<Set<String>> tradeWays =
            new MutableLiveData<>(new LinkedHashSet<>(Arrays.asList(WAY_PICKUP, WAY_LOCAL)));
    public final MutableLiveData<String> location = new MutableLiveData<>("");
    public final MutableLiveData<String> desc = new MutableLiveData<>("");

    public final MutableLiveData<Integer> balance = new MutableLiveData<>(0);
    /** 积分 ≥ 100 时置顶开关可用 */
    public final MutableLiveData<Boolean> pinEnabled = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> pin = new MutableLiveData<>(false);

    /** 当前账号的作物（附上种植记录）；没有作物时隐藏整行 */
    public final MutableLiveData<List<CropCard>> crops = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<CropCard> crop = new MutableLiveData<>(null);
    public final MutableLiveData<Boolean> attachRecord = new MutableLiveData<>(true);

    // 校验错误（@StringRes，null 表示无错误）
    public final MutableLiveData<Integer> photosError = new MutableLiveData<>(null);
    public final MutableLiveData<Integer> titleError = new MutableLiveData<>(null);
    public final MutableLiveData<Integer> categoryError = new MutableLiveData<>(null);
    public final MutableLiveData<Integer> priceError = new MutableLiveData<>(null);

    public final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    /** 发布成功，值为新商品 id */
    public final SingleLiveEvent<Long> published = new SingleLiveEvent<>();
    /** 选择的图片超过剩余名额（值为上限） */
    public final SingleLiveEvent<Integer> photoLimit = new SingleLiveEvent<>();
    public final SingleLiveEvent<Integer> messageRes = new SingleLiveEvent<>();

    @Nullable private UserEntity user;
    private boolean locationEdited;
    private boolean done;

    public AddProductViewModel(AppContainer c) {
        this(c.marketRepository, c.pointsRepository, c.cropRepository, c.userRepository,
                new PhotoPickerFileStore(c.app), c.gson, System::currentTimeMillis);
    }

    AddProductViewModel(@NonNull MarketRepository market, @NonNull PointsRepository points,
                        @NonNull CropRepository cropRepo, @Nullable UserRepository users,
                        @NonNull PhotoPickerStore photoStore, @NonNull Gson gson, @NonNull LongSupplier clock) {
        super(null);
        this.market = market;
        this.photoStore = photoStore;
        this.gson = gson;
        this.clock = clock;

        add(points.observeBalance()
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onBalance, this::onError));
        add(cropRepo.observeCropCards(null)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCrops, this::onError));
        if (users != null) {
            add(users.currentUser()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onUser, this::onError));
        }
    }

    private void onBalance(Integer value) {
        int b = value == null ? 0 : value;
        balance.setValue(b);
        boolean enabled = b >= PointsRules.PIN_COST;
        pinEnabled.setValue(enabled);
        if (!enabled && Boolean.TRUE.equals(pin.getValue())) pin.setValue(false);
    }

    private void onCrops(List<CropCard> list) {
        List<CropCard> l = list == null ? Collections.emptyList() : list;
        crops.setValue(l);
        CropCard current = crop.getValue();
        CropCard keep = null;
        if (current != null) {
            for (CropCard c : l) if (c.id == current.id) keep = c;
        }
        crop.setValue(keep != null ? keep : (l.isEmpty() ? null : l.get(0)));
    }

    private void onUser(UserEntity u) {
        user = u;
        if (!locationEdited && u.region != null && !u.region.trim().isEmpty()) location.setValue(u.region.trim());
    }

    // ---------- 图片 ----------

    /** 剩余可选数量 */
    public int remainingPhotos() {
        List<String> list = photos.getValue();
        return MAX_PHOTOS - (list == null ? 0 : list.size());
    }

    /** 照片选择器返回：复制到 filesDir/products，超出 9 张的部分丢弃并提示 */
    public void addPhotos(@Nullable List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;
        int room = remainingPhotos();
        if (room <= 0) {
            photoLimit.setValue(MAX_PHOTOS);
            return;
        }
        List<Uri> accepted = new ArrayList<>(uris.subList(0, Math.min(room, uris.size())));
        if (accepted.size() < uris.size()) photoLimit.setValue(MAX_PHOTOS);
        photoBusy.setValue(true);
        add(photoStore.copyAll(accepted, DIR_PRODUCTS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(paths -> {
                    photoBusy.setValue(false);
                    List<String> next = new ArrayList<>(currentPhotos());
                    List<String> extra = new ArrayList<>();
                    for (String p : paths) {
                        if (next.size() < MAX_PHOTOS) next.add(p);
                        else extra.add(p);
                    }
                    if (!extra.isEmpty()) photoStore.delete(extra);
                    photos.setValue(next);
                    if (!next.isEmpty()) photosError.setValue(null);
                }, e -> {
                    photoBusy.setValue(false);
                    messageRes.setValue(R.string.add_product_photo_failed);
                }));
    }

    public void removePhoto(int index) {
        List<String> next = new ArrayList<>(currentPhotos());
        if (index < 0 || index >= next.size()) return;
        String removed = next.remove(index);
        photos.setValue(next);
        photoStore.delete(Collections.singletonList(removed));
    }

    /** 设为封面：移到第一张 */
    public void setCover(int index) {
        List<String> next = new ArrayList<>(currentPhotos());
        if (index <= 0 || index >= next.size()) return;
        next.add(0, next.remove(index));
        photos.setValue(next);
    }

    private List<String> currentPhotos() {
        List<String> list = photos.getValue();
        return list == null ? Collections.emptyList() : list;
    }

    // ---------- 表单 ----------

    public void selectCategory(@NonNull String cat, @Nullable String sub) {
        category.setValue(cat);
        subCategory.setValue(sub);
        categoryError.setValue(null);
    }

    public void selectUnit(@NonNull String u) {
        unit.setValue(u);
    }

    public void toggleTradeWay(@NonNull String way, boolean on) {
        Set<String> next = new LinkedHashSet<>();
        Set<String> cur = tradeWays.getValue();
        for (String w : WAYS) {
            boolean selected = w.equals(way) ? on : cur != null && cur.contains(w);
            if (selected) next.add(w);
        }
        tradeWays.setValue(next);
    }

    public void setLocation(@Nullable String value) {
        locationEdited = true;
        location.setValue(value == null ? "" : value.trim());
    }

    /** 置顶开关：积分不足时不可开启 */
    public void setPin(boolean on) {
        boolean allowed = Boolean.TRUE.equals(pinEnabled.getValue());
        pin.setValue(on && allowed);
    }

    public void setAttachRecord(boolean on) {
        attachRecord.setValue(on);
    }

    public void selectCrop(@NonNull CropCard c) {
        crop.setValue(c);
    }

    /** 置顶后剩余积分（「剩余 1,180」） */
    public int balanceAfterPin() {
        Integer b = balance.getValue();
        return (b == null ? 0 : b) - PointsRules.PIN_COST;
    }

    public void onTitleChanged() {
        String t = title.getValue();
        if (t != null && !t.trim().isEmpty()) titleError.setValue(null);
    }

    public void onPriceChanged() {
        if (MarketFormat.parsePriceCent(price.getValue()) > 0) priceError.setValue(null);
    }

    /** 校验：至少 1 张图片、标题、分类、价格 &gt; 0；返回是否通过 */
    public boolean validate() {
        boolean ok = true;
        if (currentPhotos().isEmpty()) {
            photosError.setValue(R.string.add_product_err_photos);
            ok = false;
        } else {
            photosError.setValue(null);
        }
        String t = title.getValue() == null ? "" : title.getValue().trim();
        if (t.isEmpty()) {
            titleError.setValue(R.string.add_product_err_title);
            ok = false;
        } else {
            titleError.setValue(null);
        }
        if (category.getValue() == null) {
            categoryError.setValue(R.string.add_product_err_category);
            ok = false;
        } else {
            categoryError.setValue(null);
        }
        if (MarketFormat.parsePriceCent(price.getValue()) <= 0) {
            priceError.setValue(R.string.add_product_err_price);
            ok = false;
        } else {
            priceError.setValue(null);
        }
        return ok;
    }

    /** 发布：校验通过后写入集市（置顶时同一事务扣除 100 积分） */
    public void publish() {
        if (Boolean.TRUE.equals(busy.getValue()) || Boolean.TRUE.equals(photoBusy.getValue())) return;
        if (!validate()) return;
        boolean pinIt = Boolean.TRUE.equals(pin.getValue()) && Boolean.TRUE.equals(pinEnabled.getValue());
        busy.setValue(true);
        add(market.publish(buildProduct(), pinIt)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(id -> {
                    busy.setValue(false);
                    done = true;
                    published.setValue(id);
                }, e -> {
                    busy.setValue(false);
                    onError(e);
                }));
    }

    @NonNull
    ProductEntity buildProduct() {
        ProductEntity p = new ProductEntity();
        p.mine = true;
        p.title = title.getValue() == null ? "" : title.getValue().trim();
        p.category = category.getValue() == null ? ProductEntity.CAT_PRODUCE : category.getValue();
        p.subCategory = subCategory.getValue();
        p.priceCent = MarketFormat.parsePriceCent(price.getValue());
        p.unit = blankToNull(unit.getValue());
        p.minQty = parseQty(minQty.getValue());
        Set<String> ways = tradeWays.getValue() == null ? Collections.emptySet() : tradeWays.getValue();
        List<String> wayList = new ArrayList<>();
        for (String w : WAYS) if (ways.contains(w)) wayList.add(w);
        p.tradeWays = String.join("、", wayList);
        p.location = blankToNull(location.getValue());
        p.images = gson.toJson(currentPhotos());
        p.description = blankToNull(desc.getValue());

        CropCard c = crop.getValue();
        boolean attach = Boolean.TRUE.equals(attachRecord.getValue()) && c != null;
        p.cropId = attach ? c.id : null;
        List<String> features = new ArrayList<>();
        if (ways.contains(WAY_PICKUP)) features.add(FEATURE_PICKUP);
        if (ways.contains(WAY_LOCAL)) features.add(FEATURE_LOCAL);
        if (ways.contains(WAY_EXPRESS)) features.add(FEATURE_EXPRESS);
        if (attach) features.add(FEATURE_RECORD);
        p.tags = gson.toJson(features);

        UserEntity u = user;
        if (u != null) {
            p.userId = u.id;
            p.sellerName = u.nickname;
            p.sellerYears = u.years;
            p.sellerPhone = u.phone;
        }
        p.sellerRegion = MarketFormat.shortRegion(p.location);
        p.createdAt = clock.getAsLong();
        return p;
    }

    private static double parseQty(@Nullable String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try {
            double v = Double.parseDouble(s.trim());
            return v > 0 ? v : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Nullable
    private static String blankToNull(@Nullable String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private void onError(Throwable e) {
        toast.setValue(AppError.from(e).message);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!done) photoStore.delete(currentPhotos());   // 未发布：删除已复制的图片
    }
}
