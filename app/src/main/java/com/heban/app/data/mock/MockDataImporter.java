package com.heban.app.data.mock;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.local.prefs.SettingsStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 模拟数据导入（技术准备文档 7.5）：第一次启动或 MOCK_VERSION 提高时，在后台把 assets/mock/ 中
 * 「将来由后端提供」的共享内容（病虫害百科、集市商品，以及后续的农友、动态、问答、积分商品）写入 Room。
 * 用户自己的地块、作物、农事、记账、积分不导入（新账号从空开始）。
 * <p>
 * 规则：
 * <ul>
 *   <li>导入在一个事务内完成，成功后把 {@link SettingsStore#MOCK_IMPORTED_VERSION} 写为 {@link #MOCK_VERSION}；</li>
 *   <li>只按 JSON 中的 id 覆盖（REPLACE）共享行，不删除其他行；用户自己发布的商品（mine = true）即使 id 与
 *       JSON 相同也不会被覆盖。之后新增模拟商品请使用新的 id；</li>
 *   <li>版本已是最新但共享表为空（例如开发阶段破坏性迁移清空了数据库）时也会重新导入；</li>
 *   <li>后续模块（农友、动态、问答、积分商品）各自增加一个 importXxx 方法，并在 {@link #writeAll} 中调用，
 *       同时提高 MOCK_VERSION。</li>
 * </ul>
 */
public final class MockDataImporter {
    /** 模拟数据版本：assets/mock 中的共享内容有变化时加 1 */
    public static final int MOCK_VERSION = 1;

    private final Context app;
    private final AppDatabase db;
    private final SettingsStore settings;
    private final Gson gson;

    public MockDataImporter(Context app, AppDatabase db, SettingsStore settings, Gson gson) {
        this.app = app;
        this.db = db;
        this.settings = settings;
        this.gson = gson;
    }

    public Completable importIfNeeded() {
        return settings.get(SettingsStore.MOCK_IMPORTED_VERSION, 0)
                .observeOn(Schedulers.io())
                .flatMapCompletable(v -> needsImport(v)
                        ? Completable.fromAction(this::importNow)
                                .andThen(settings.put(SettingsStore.MOCK_IMPORTED_VERSION, MOCK_VERSION))
                        : Completable.complete())
                .subscribeOn(Schedulers.io());
    }

    private boolean needsImport(int importedVersion) {
        if (importedVersion < MOCK_VERSION) return true;
        return db.pestDao().countSync() == 0 || db.productDao().countSharedSync() == 0;
    }

    /** 同步导入（io 线程）：先读取并解析全部文件，再在一个事务中写入 */
    @VisibleForTesting
    public void importNow() throws IOException {
        long now = System.currentTimeMillis();
        MockBundle bundle = new MockBundle();
        try (Reader r = open(MockJson.PESTS)) {
            bundle.pests = MockJson.parsePests(gson, r);
        }
        try (Reader r = open(MockJson.PRODUCTS)) {
            bundle.products = MockJson.parseProducts(gson, r, now);
        }
        // 后续：farmers.json、posts.json、questions.json、mall_items.json 在这里读取
        db.runInTransaction(() -> writeAll(bundle));
    }

    private Reader open(String asset) throws IOException {
        return new InputStreamReader(app.getAssets().open(asset), StandardCharsets.UTF_8);
    }

    private void writeAll(MockBundle bundle) {
        importPests(bundle.pests);
        importProducts(bundle.products);
        // importFarmers(bundle.farmers); importPosts(bundle.posts); importQuestions(…); importMallItems(…);
    }

    /** 百科没有用户数据，按 id 整行覆盖 */
    private void importPests(List<PestEntity> pests) {
        db.pestDao().insertAllSync(pests);
    }

    /** 集市商品：按 id 覆盖共享商品，跳过与用户自己发布的商品 id 相同的行 */
    private void importProducts(List<ProductEntity> products) {
        Set<Long> mine = new HashSet<>(db.productDao().mineIdsSync());
        List<ProductEntity> shared = new ArrayList<>(products.size());
        for (ProductEntity p : products) {
            if (!mine.contains(p.id)) shared.add(p);
        }
        db.productDao().insertAllSync(shared);
    }

    /** 一次导入读取到的全部内容 */
    private static final class MockBundle {
        List<PestEntity> pests = new ArrayList<>();
        List<ProductEntity> products = new ArrayList<>();
    }
}
