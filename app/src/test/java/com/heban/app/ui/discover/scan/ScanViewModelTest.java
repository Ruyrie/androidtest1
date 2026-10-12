package com.heban.app.ui.discover.scan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.gson.Gson;
import com.heban.app.data.local.entity.ScanRecordEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.ScanMode;
import com.heban.app.data.model.ScanOutcome;
import com.heban.app.data.model.ScanResult;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.ui.crop.CropsFakeRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.SingleSubject;

public class ScanViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private ScanFakes.FakeVision vision;
    private ScanFakes.FakePests pests;
    private CropsFakeRepository crops;
    private ScanViewModel vm;
    private final List<Long> opened = new ArrayList<>();
    private final List<String> toasts = new ArrayList<>();
    private final List<String> savedTo = new ArrayList<>();
    private final List<ScanViewModel.Phase> phases = new ArrayList<>();

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        vision = new ScanFakes.FakeVision();
        pests = new ScanFakes.FakePests();
        crops = new CropsFakeRepository();
        crops.cards.onNext(Arrays.asList(
                CropsFakeRepository.card(1, "番茄", "樱桃番茄", "1号大棚", 1, "GREENHOUSE", "结果期", 68, 110, null),
                CropsFakeRepository.card(2, "黄瓜", "津优", "2号大棚", 1, "GREENHOUSE", "结果期", 40, 90, null)));
        vm = new ScanViewModel(null, vision, crops, pests, new Gson());
        vm.openResult.observeForever(opened::add);
        vm.toast.observeForever(toasts::add);
        vm.savedTo.observeForever(savedTo::add);
        vm.phase.observeForever(phases::add);
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    @Test
    public void recognize_showsLoadingThenSuccess() {
        Bitmap photo = ScanFakes.bitmap();
        vm.selectMode(ScanMode.CROP);
        vm.recognize(photo);

        assertEquals(ScanViewModel.Phase.LOADING, vm.phase.getValue());
        assertEquals(Long.valueOf(0), opened.get(0));
        assertSame(photo, vm.photo.getValue());
        assertEquals(ScanMode.CROP, vision.recognizeModes.get(0));
        assertEquals("番茄、黄瓜", vision.recognizeHints.get(0));

        pests.ids.put("黄瓜细菌性角斑病", 21L);
        vision.pending.onSuccess(ScanFakes.outcome(5, ScanFakes.sampleResult()));

        assertEquals(ScanViewModel.Phase.SUCCESS, vm.phase.getValue());
        assertEquals("黄瓜霜霉病", vm.outcome.getValue().result.top.name);
        assertEquals(Long.valueOf(11), vm.pestId());
        assertEquals(Long.valueOf(21), vm.otherPestId("黄瓜细菌性角斑病"));
        assertNull(vm.otherPestId("缺镁症（生理性）"));
        assertFalse(vm.saved.getValue());
        // 识别出黄瓜：默认保存到「黄瓜 · 2号大棚」
        assertEquals(2, vm.saveTarget().id);
    }

    @Test
    public void recognize_failure_showsAppErrorMessage() {
        vm.recognize(ScanFakes.bitmap());
        vision.pending.onError(new BusinessException("NO_KEY", "未配置识图模型，请到「AI 助手设置 → 模型服务」填写 API Key"));

        assertEquals(ScanViewModel.Phase.ERROR, vm.phase.getValue());
        assertEquals("未配置识图模型，请到「AI 助手设置 → 模型服务」填写 API Key", vm.errorMessage.getValue());
    }

    @Test
    public void recognize_networkFailure_usesFriendlyMessage() {
        vm.recognize(ScanFakes.bitmap());
        vision.pending.onError(new IOException("boom"));

        assertEquals(ScanViewModel.Phase.ERROR, vm.phase.getValue());
        assertEquals("网络不太好，检查后再试一次", vm.errorMessage.getValue());
    }

    @Test
    public void recognize_decodeFailure_isError() {
        vm.recognizeFrom(() -> {
            throw new IOException("unreadable");
        });
        assertEquals(ScanViewModel.Phase.ERROR, vm.phase.getValue());
        assertTrue(vision.recognizePhotos.isEmpty());
    }

    @Test
    public void imageNotOk_showsImageBad() {
        vm.recognize(ScanFakes.bitmap());
        ScanResult r = new ScanResult();
        r.imageOk = false;
        r.imageIssue = "照片过暗，看不清叶片";
        vision.pending.onSuccess(ScanFakes.outcome(6, r));

        assertEquals(ScanViewModel.Phase.IMAGE_BAD, vm.phase.getValue());
        assertEquals("照片过暗，看不清叶片", vm.outcome.getValue().result.imageIssue);
    }

    @Test
    public void missingTop_isParseError() {
        vm.recognize(ScanFakes.bitmap());
        ScanResult r = new ScanResult();
        vision.pending.onSuccess(ScanFakes.outcome(7, r));

        assertEquals(ScanViewModel.Phase.ERROR, vm.phase.getValue());
        assertEquals("返回内容无法识别", vm.errorMessage.getValue());
    }

    @Test
    public void retry_afterFailure_resubmitsSamePhoto() {
        Bitmap photo = ScanFakes.bitmap();
        vm.recognize(photo);
        vision.pending.onError(new IOException());
        vision.pending = SingleSubject.create();

        vm.retry();
        assertEquals(ScanViewModel.Phase.LOADING, vm.phase.getValue());
        assertEquals(2, vision.recognizePhotos.size());
        assertSame(photo, vision.recognizePhotos.get(1));

        vision.pending.onSuccess(ScanFakes.outcome(8, ScanFakes.sampleResult()));
        assertEquals(ScanViewModel.Phase.SUCCESS, vm.phase.getValue());
        assertNull(vm.errorMessage.getValue());
    }

    @Test
    public void retry_withRecord_usesRepositoryRetry() {
        vm.recognize(ScanFakes.bitmap());
        vision.pending.onSuccess(ScanFakes.outcome(9, ScanFakes.sampleResult()));
        vision.pending = SingleSubject.create();

        vm.retry();
        assertEquals(Arrays.asList(9L), vision.retried);
        assertEquals(ScanViewModel.Phase.LOADING, vm.phase.getValue());
        // 识别中再次刷新不会重复提交
        vm.retry();
        assertEquals(1, vision.retried.size());
    }

    @Test
    public void openRecord_showsSavedOutcomeOnce() {
        ScanOutcome o = ScanFakes.outcome(12, ScanFakes.sampleResult());
        o.cropId = 2L;
        vision.records.put(12L, o);

        vm.openRecord(12);
        assertEquals(ScanViewModel.Phase.SUCCESS, vm.phase.getValue());
        assertTrue(vm.saved.getValue());
        assertNull(vm.photo.getValue());

        vm.openRecord(12);
        assertEquals(1, vision.loaded.size());
    }

    @Test
    public void openHistory_navigatesWithRecordId() {
        vision.records.put(13L, ScanFakes.outcome(13, ScanFakes.sampleResult()));
        vm.openHistory(13);
        assertEquals(Long.valueOf(13), opened.get(opened.size() - 1));
        assertEquals(ScanViewModel.Phase.SUCCESS, vm.phase.getValue());
    }

    @Test
    public void saveToCrop_marksSaved() {
        vm.recognize(ScanFakes.bitmap());
        vision.pending.onSuccess(ScanFakes.outcome(14, ScanFakes.sampleResult()));

        CropCard target = vm.saveTarget();
        vm.saveToCrop(target);
        assertEquals(14, vision.savedToCrop.get(0)[0]);
        assertEquals(2, vision.savedToCrop.get(0)[1]);
        assertTrue(vm.saved.getValue());
        assertEquals(Arrays.asList("黄瓜 · 2号大棚"), savedTo);

        // 已保存后不再重复保存
        vm.saveToCrop(target);
        assertEquals(1, vision.savedToCrop.size());
    }

    @Test
    public void saveToCrop_failure_showsToast() {
        vision.saveError = new BusinessException("NOT_LOGGED_IN", "请先登录");
        vm.recognize(ScanFakes.bitmap());
        vision.pending.onSuccess(ScanFakes.outcome(15, ScanFakes.sampleResult()));

        vm.saveToCrop(vm.saveTarget());
        assertFalse(vm.saved.getValue());
        assertFalse(vm.saving.getValue());
        assertEquals(1, toasts.size());
    }

    @Test
    public void newRecognition_resetsPreviousResult() {
        vision.records.put(16L, ScanFakes.outcome(16, ScanFakes.sampleResult()));
        vm.openHistory(16);
        vm.recognize(ScanFakes.bitmap());

        assertNull(vm.outcome.getValue());
        assertEquals(ScanViewModel.Phase.LOADING, vm.phase.getValue());
        assertFalse(vm.saved.getValue());
    }

    @Test
    public void aiPromptAndPublishText_comeFromOutcome() {
        vm.recognize(ScanFakes.bitmap());
        vision.pending.onSuccess(ScanFakes.outcome(17, ScanFakes.sampleResult()));

        assertEquals("拍照识别结果：黄瓜霜霉病（中度，匹配度 92%），依据：叶面受叶脉限制的黄色多角形斑，叶背有灰黑色霉层。"
                + "请给出详细防治方案。", vm.aiPrompt());
        assertTrue(vm.publishText().startsWith("这是黄瓜霜霉病吗？"));
    }

    @Test
    public void history_parsesRecords() {
        ScanRecordEntity ok = new ScanRecordEntity();
        ok.id = 1;
        ok.mode = "CROP";
        ok.resultJson = new Gson().toJson(ScanFakes.sampleResult());
        ok.cropId = 2L;
        ScanRecordEntity broken = new ScanRecordEntity();
        broken.id = 2;
        broken.resultJson = "{not json";
        vision.history.onNext(Arrays.asList(ok, broken));

        List<ScanViewModel.HistoryItem> items = vm.history().getValue();
        assertEquals(2, items.size());
        assertEquals(ScanMode.CROP, items.get(0).mode);
        assertTrue(items.get(0).saved);
        assertEquals("黄瓜霜霉病", ScanFormat.historyTitle(items.get(0).result, "照片不可用"));
        assertEquals("照片不可用", ScanFormat.historyTitle(items.get(1).result, "照片不可用"));

        vm.deleteRecord(2);
        assertEquals(Arrays.asList(2L), vision.deleted);
    }

    @Test
    public void cameraPermission_requestedOncePerFlow() {
        assertTrue(vm.shouldRequestPermission());
        assertFalse(vm.shouldRequestPermission());
        vm.setCameraDenied(true);
        assertTrue(vm.cameraDenied.getValue());
    }

    @Test
    public void flashToggle() {
        vm.toggleFlash();
        assertTrue(vm.flashOn.getValue());
        vm.toggleFlash();
        assertFalse(vm.flashOn.getValue());
    }
}
