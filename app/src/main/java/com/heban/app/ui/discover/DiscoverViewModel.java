package com.heban.app.ui.discover;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.LedgerRepository;
import com.heban.app.data.repository.MarketRepository;
import com.heban.app.data.repository.PestRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;

/**
 * D01 发现：百科条数、集市在售数、本月结余、「本季高发」（按当前月份，标题带地区）、「附近好物」（最近的 2 件）
 * （技术准备文档 12.4）。游客的记账入口显示「登录后记账」。
 */
public class DiscoverViewModel extends BaseViewModel {
    /** 「附近好物」显示的商品数 */
    static final int NEARBY_LIMIT = 2;

    public final MutableLiveData<Integer> pestCount = new MutableLiveData<>(0);
    public final MutableLiveData<Integer> marketCount = new MutableLiveData<>(0);
    /** 已用账号登录（游客为 false） */
    public final MutableLiveData<Boolean> signedIn = new MutableLiveData<>(false);
    /** 本月结余「+¥3,460」；游客为 null */
    public final MutableLiveData<String> balance = new MutableLiveData<>(null);
    /** 「江宁区 · 10 月」/「10 月」 */
    public final MutableLiveData<String> seasonHeader = new MutableLiveData<>();
    public final MutableLiveData<List<PestEntity>> seasonal = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<List<ProductEntity>> nearby = new MutableLiveData<>(Collections.emptyList());
    /** 加载完成且为空时显示空状态（加载前不闪空状态） */
    public final MutableLiveData<Boolean> seasonalEmpty = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> nearbyEmpty = new MutableLiveData<>(false);

    public DiscoverViewModel(AppContainer c) {
        this(c, c.pestRepository, c.marketRepository, c.ledgerRepository,
                c.currentUser.observeUserId(), c.userRepository.currentUser(), LocalDate.now());
    }

    DiscoverViewModel(@Nullable AppContainer c, @NonNull PestRepository pests, @NonNull MarketRepository market,
                      @NonNull LedgerRepository ledger, @NonNull Flowable<Long> userId,
                      @NonNull Flowable<UserEntity> currentUser, @NonNull LocalDate today) {
        super(c);
        int month = today.getMonthValue();
        seasonHeader.setValue(DiscoverFormat.seasonHeader(null, month));

        add(pests.count()
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pestCount::setValue, this::onError));

        add(market.count(null)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(marketCount::setValue, this::onError));

        Flowable<Long> uid = userId.distinctUntilChanged().replay(1).refCount();

        add(uid.switchMap(id -> id > 0
                        ? ledger.observeMonth(YearMonth.from(today))
                        .map(s -> Optional.of(DiscoverFormat.balance(s.balanceCent())))
                        : Flowable.just(Optional.<String>empty()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(b -> {
                    signedIn.setValue(b.isPresent());
                    balance.setValue(b.orElse(null));
                }, this::onError));

        // 游客 / 未登录时 currentUser 不发射，只显示月份
        add(uid.switchMap(id -> id > 0
                        ? currentUser.map(u -> DiscoverFormat.seasonHeader(u.region, month))
                        : Flowable.just(DiscoverFormat.seasonHeader(null, month)))
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(seasonHeader::setValue, this::onError));

        add(pests.seasonal(month)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    seasonal.setValue(list);
                    seasonalEmpty.setValue(list.isEmpty());
                }, this::onError));

        add(market.nearby(NEARBY_LIMIT)
                .map(list -> list.size() > NEARBY_LIMIT ? new ArrayList<>(list.subList(0, NEARBY_LIMIT)) : list)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    nearby.setValue(list);
                    nearbyEmpty.setValue(list.isEmpty());
                }, this::onError));
    }

    private void onError(Throwable e) {
        toast.setValue(AppError.from(e).message);
    }
}
