package com.heban.app.ui.discover.market;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingData;

import com.heban.app.data.local.entity.FavoriteEntity;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.data.repository.FavoriteRepository;
import com.heban.app.data.repository.MarketRepository;
import com.heban.app.data.repository.PointsRepository;
import com.heban.app.data.repository.UserRepository;
import com.heban.app.ui.crop.PhotoPickerStore;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.BehaviorProcessor;

/** D07–D09 ViewModel 测试用的假数据源（大接口只实现用到的方法，其余调用抛出异常）。 */
final class MarketFakes {
    private MarketFakes() {
    }

    static ProductEntity product(long id, String category, String title, long priceCent, String unit) {
        ProductEntity p = new ProductEntity();
        p.id = id;
        p.category = category;
        p.title = title;
        p.priceCent = priceCent;
        p.unit = unit;
        p.sellerName = "草莓姐姐";
        return p;
    }

    /** 记录 products(category, sort) 的调用 */
    static final class Market implements MarketRepository {
        final List<String> calls = new ArrayList<>();
        final List<String> countCalls = new ArrayList<>();
        final BehaviorProcessor<ProductEntity> product = BehaviorProcessor.create();
        int views;
        long deleted;
        ProductEntity published;
        boolean publishedPin;
        Throwable publishError;

        @Override
        public Flowable<PagingData<ProductEntity>> products(@Nullable String category, String sort) {
            calls.add(category + "|" + sort);
            return Flowable.just(PagingData.empty());
        }

        @Override
        public Flowable<Integer> count(@Nullable String category) {
            countCalls.add(String.valueOf(category));
            return Flowable.just(category == null ? 36 : 2);
        }

        @Override
        public Flowable<List<ProductEntity>> nearby(int limit) {
            return Flowable.just(Collections.emptyList());
        }

        @Override
        public Flowable<ProductEntity> observeProduct(long id) {
            return product;
        }

        @Override
        public Completable addView(long id) {
            return Completable.fromAction(() -> views++);
        }

        @Override
        public Single<Long> publish(ProductEntity p, boolean pin24h) {
            if (publishError != null) return Single.error(publishError);
            published = p;
            publishedPin = pin24h;
            return Single.just(99L);
        }

        @Override
        public Completable delete(long id) {
            return Completable.fromAction(() -> deleted = id);
        }

        @Override
        public Flowable<List<ProductEntity>> observeMine() {
            return Flowable.just(Collections.emptyList());
        }
    }

    static final class Favorites implements FavoriteRepository {
        final Set<Long> ids = new HashSet<>();
        final BehaviorProcessor<Boolean> isFavorite = BehaviorProcessor.createDefault(false);
        final BehaviorProcessor<List<FavoriteEntity>> all = BehaviorProcessor.createDefault(Collections.emptyList());

        @Override
        public Flowable<Boolean> observeIsFavorite(String targetType, long targetId) {
            return isFavorite;
        }

        @Override
        public Completable toggle(String targetType, long targetId) {
            return Completable.fromAction(() -> isFavorite.onNext(!isFavorite.getValue()));
        }

        @Override
        public Flowable<List<FavoriteEntity>> observeAll(String targetType) {
            return all;
        }
    }

    static PointsRepository points(BehaviorProcessor<Integer> balance) {
        return proxy(PointsRepository.class, (name, args) ->
                "observeBalance".equals(name) ? balance : null);
    }

    static CropRepository crops(BehaviorProcessor<List<CropCard>> cards) {
        return proxy(CropRepository.class, (name, args) ->
                "observeCropCards".equals(name) ? cards : null);
    }

    static UserRepository users(@Nullable UserEntity user) {
        return proxy(UserRepository.class, (name, args) -> "currentUser".equals(name)
                ? (user == null ? Flowable.never() : Flowable.just(user)) : null);
    }

    static UserEntity user(long id, String nickname, String region) {
        UserEntity u = new UserEntity();
        u.id = id;
        u.nickname = nickname;
        u.region = region;
        u.phone = "13800000000";
        u.years = 8;
        return u;
    }

    static CropCard crop(long id, String name, String plot) {
        CropCard c = new CropCard();
        c.id = id;
        c.name = name;
        c.plotName = plot;
        return c;
    }

    /** 复制图片：返回 file:///products/N.jpg */
    static final class Photos implements PhotoPickerStore {
        final List<String> deleted = new ArrayList<>();
        int counter;

        @NonNull
        @Override
        public Single<String> copy(@NonNull Uri source, @NonNull String dir) {
            return Single.just("file:///" + dir + "/" + (++counter) + ".jpg");
        }

        @NonNull
        @Override
        public Single<List<String>> copyAll(@NonNull List<Uri> sources, @NonNull String dir) {
            List<String> out = new ArrayList<>();
            for (int i = 0; i < sources.size(); i++) out.add("file:///" + dir + "/" + (++counter) + ".jpg");
            return Single.just(out);
        }

        @Override
        public void delete(@NonNull Collection<String> paths) {
            deleted.addAll(paths);
        }

        @Nullable
        @Override
        public Uri createCameraTarget() {
            return null;
        }

        @Override
        public void discardCameraTarget(@NonNull Uri target) {
        }
    }

    static BusinessException pointsNotEnough() {
        return new BusinessException("POINTS_NOT_ENOUGH", "积分不足");
    }

    interface Handler {
        Object handle(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, args) -> {
            if (m.getDeclaringClass() == Object.class) {
                if ("equals".equals(m.getName())) return p == args[0];
                if ("hashCode".equals(m.getName())) return System.identityHashCode(p);
                return type.getSimpleName() + "Fake";
            }
            Object r = handler.handle(m.getName(), args);
            if (r == null) throw new UnsupportedOperationException(m.getName());
            return r;
        });
    }
}
