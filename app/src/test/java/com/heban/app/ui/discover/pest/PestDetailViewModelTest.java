package com.heban.app.ui.discover.pest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.gson.Gson;
import com.heban.app.data.local.entity.FavoriteEntity;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.ui.discover.DiscoverFakes;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PestDetailViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final String REMINDER = "按产品标签用量使用、注意安全间隔期";

    private DiscoverFakes.Pests repo;
    private DiscoverFakes.Favorites favorites;
    private PestDetailViewModel vm;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        repo = new DiscoverFakes.Pests();
        favorites = new DiscoverFakes.Favorites();
        PestEntity p = DiscoverFakes.pest(1, "黄瓜霜霉病", "病害", "卵菌病害", "黄瓜", "高发");
        p.images = "[\"images/pests/cucumber_downy_mildew_1.jpg\",\"images/pests/cucumber_downy_mildew_2.jpg\"]";
        p.imageCaptions = "[\"叶片正面症状\",\"叶背霉层\"]";
        p.preventionAgri = "[\"选用抗病品种，培育无病壮苗\",\"膜下滴灌，上午通风排湿，降低棚内湿度\",\"及时摘除病叶，带出棚外处理\"]";
        p.preventionChem = "[\"发病初期选用烯酰吗啉、霜脲·锰锌等药剂喷施\"]";
        repo.byId.put(1L, p);
        vm = new PestDetailViewModel(null, repo, favorites, new Gson());
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    @Test
    public void load_parsesJsonFields() {
        vm.start(1, REMINDER);
        assertTrue(vm.loaded.getValue());
        assertFalse(vm.notFound.getValue());
        assertEquals("黄瓜霜霉病", vm.name());

        assertEquals(2, vm.images.getValue().size());
        assertEquals("叶片正面症状", vm.images.getValue().get(0).caption);
        assertEquals("images/pests/cucumber_downy_mildew_2.jpg", vm.images.getValue().get(1).path);

        assertEquals(3, vm.agri.getValue().size());
        assertEquals(Arrays.asList("发病初期选用烯酰吗啉、霜脲·锰锌等药剂喷施", REMINDER), vm.chem.getValue());
    }

    @Test
    public void segment_switchesSteps() {
        vm.start(1, REMINDER);
        vm.steps.observeForever(s -> { });
        assertFalse(vm.chemSelected.getValue());
        assertEquals(vm.agri.getValue(), vm.steps.getValue());

        vm.selectChem(true);
        assertEquals(vm.chem.getValue(), vm.steps.getValue());
        assertEquals(REMINDER, vm.steps.getValue().get(vm.steps.getValue().size() - 1));

        vm.selectChem(false);
        assertEquals("选用抗病品种，培育无病壮苗", vm.steps.getValue().get(0));
    }

    @Test
    public void reminderNotDuplicated() {
        repo.byId.get(1L).preventionChem = "[\"选用烯酰吗啉等药剂，按标签用量喷施\"]";
        vm.start(1, REMINDER);
        assertEquals(Collections.singletonList("选用烯酰吗啉等药剂，按标签用量喷施"), vm.chem.getValue());
    }

    @Test
    public void missingImages_onePlaceholder() {
        PestEntity p = repo.byId.get(1L);
        p.images = null;
        p.imageCaptions = "not json";
        p.preventionAgri = null;
        p.preventionChem = "[]";
        vm.start(1, REMINDER);
        assertEquals(1, vm.images.getValue().size());
        assertNull(vm.images.getValue().get(0).path);
        assertTrue(vm.agri.getValue().isEmpty());
        assertTrue(vm.chem.getValue().isEmpty());
    }

    @Test
    public void notFound() {
        vm.start(99, REMINDER);
        assertTrue(vm.notFound.getValue());
        assertFalse(vm.loaded.getValue());
        assertNull(vm.name());
    }

    @Test
    public void favorite_observedAndToggled() {
        vm.start(1, REMINDER);
        assertEquals(FavoriteEntity.TYPE_PEST, favorites.observedType);
        assertEquals(1L, favorites.observedId);
        assertFalse(vm.favorite.getValue());

        vm.toggleFavorite();
        assertEquals(1, favorites.toggles);
        assertTrue(vm.favorite.getValue());

        vm.toggleFavorite();
        assertFalse(vm.favorite.getValue());
    }

    @Test
    public void favorite_errorShowsToast() {
        vm.start(1, REMINDER);
        favorites.toggleError = new BusinessException("NOT_LOGGED_IN", "请先登录");
        vm.toggleFavorite();
        assertEquals("请先登录", vm.toast.getValue());
        assertFalse(vm.favorite.getValue());
    }

    @Test
    public void start_onlyOncePerId() {
        vm.start(1, REMINDER);
        vm.selectChem(true);
        vm.start(1, REMINDER);
        assertTrue("重建视图时保留分段选择", vm.chemSelected.getValue());
    }
}
