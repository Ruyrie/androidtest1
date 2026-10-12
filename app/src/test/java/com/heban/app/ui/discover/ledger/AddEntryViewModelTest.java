package com.heban.app.ui.discover.ledger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.R;
import com.heban.app.data.local.entity.LedgerEntryEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AddEntryViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    /** 2026-10-10 10:00:00（北京时间） */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-10T02:00:00Z"), LedgerFakes.ZONE);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    private LedgerFakes.Ledger ledger;
    private LedgerFakes.Crops crops;
    private AddEntryViewModel vm;
    private Integer lastMessage;
    private String lastToast;
    private Boolean savedEvent;
    private Boolean deletedEvent;
    private Integer finishEvent;

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        ledger = new LedgerFakes.Ledger();
        crops = new LedgerFakes.Crops();
        crops.cards = Arrays.asList(LedgerFakes.card(1, "番茄", "1号大棚"), LedgerFakes.card(2, "黄瓜", null));
        vm = new AddEntryViewModel(null, ledger, crops, CLOCK);
        vm.headerText.observeForever(v -> { });
        vm.amountText.observeForever(v -> { });
        vm.messageRes.observeForever(v -> lastMessage = v);
        vm.toast.observeForever(v -> lastToast = v);
        vm.saved.observeForever(v -> savedEvent = v);
        vm.deleted.observeForever(v -> deletedEvent = v);
        vm.finish.observeForever(v -> finishEvent = v);
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private void type(String keys) {
        for (char ch : keys.toCharArray()) vm.press(String.valueOf(ch));
    }

    @Test
    public void defaults() {
        vm.start(0, null, 0);
        assertEquals(LedgerEntryEntity.TYPE_OUT, vm.type.getValue());
        assertEquals("化肥", vm.category.getValue());
        assertEquals("0", vm.amount.getValue());
        assertEquals("支出 · 化肥", vm.headerText.getValue());
        assertEquals("−¥0", vm.amountText.getValue());
        assertEquals(TODAY, vm.date.getValue());
        assertNull(vm.crop.getValue());
        assertEquals(2, vm.crops.getValue().size());
        assertFalse(vm.editing.getValue());
        assertEquals(8, vm.categories().length);
    }

    @Test
    public void presetIncomeType() {
        vm.start(0, LedgerEntryEntity.TYPE_IN, 0);
        assertEquals(LedgerEntryEntity.TYPE_IN, vm.type.getValue());
        assertEquals("农产品销售", vm.category.getValue());
        assertEquals(4, vm.categories().length);
        type("960");
        assertEquals("+¥960", vm.amountText.getValue());
        assertEquals("收入 · 农产品销售", vm.headerText.getValue());
    }

    @Test
    public void presetCrop() {
        vm.start(0, null, 1);
        assertEquals(1, vm.crop.getValue().id);
    }

    @Test
    public void switchingType_resetsCategory() {
        vm.start(0, null, 0);
        vm.selectCategory("人工");
        assertEquals("人工", vm.category.getValue());
        vm.selectType(LedgerEntryEntity.TYPE_IN);
        assertEquals("农产品销售", vm.category.getValue());
        vm.selectCategory("政府补贴");
        vm.selectType(LedgerEntryEntity.TYPE_OUT);
        assertEquals("化肥", vm.category.getValue());
        // 「其他」两边都有，切换时也重置
        vm.selectCategory("其他");
        vm.selectType(LedgerEntryEntity.TYPE_IN);
        assertEquals("农产品销售", vm.category.getValue());
        // 选同一类型不变
        vm.selectCategory("代耕作业");
        vm.selectType(LedgerEntryEntity.TYPE_IN);
        assertEquals("代耕作业", vm.category.getValue());
    }

    @Test
    public void selectCategory_ignoresOtherTypesCategories() {
        vm.start(0, null, 0);
        vm.selectCategory("农产品销售");
        assertEquals("化肥", vm.category.getValue());
    }

    @Test
    public void keypad() {
        vm.start(0, null, 0);
        type("60");
        assertEquals("−¥60", vm.amountText.getValue());
        vm.press("00");
        assertEquals("6000", vm.amount.getValue());
        vm.backspace();
        assertEquals("600", vm.amount.getValue());
        vm.clear();
        assertEquals("0", vm.amount.getValue());
        type("1.234");
        assertEquals("1.23", vm.amount.getValue());
    }

    @Test
    public void save_requiresPositiveAmount() {
        vm.start(0, null, 0);
        vm.save();
        assertEquals(Integer.valueOf(R.string.add_entry_amount_required), lastMessage);
        assertTrue(ledger.added.isEmpty());
        type("0.");
        vm.save();
        assertTrue(ledger.added.isEmpty());
        type("00");
        vm.save();
        assertTrue(ledger.added.isEmpty());
        assertNull(savedEvent);
    }

    @Test
    public void save_newEntry() {
        vm.start(0, null, 0);
        vm.selectCategory("农药");
        type("60.5");
        vm.selectCrop(crops.cards.get(0));
        vm.setNote("  百菌清 2 瓶  ");
        vm.setDate(TODAY.minusDays(1));
        vm.save();
        assertEquals(1, ledger.added.size());
        LedgerEntryEntity e = ledger.added.get(0);
        assertEquals(0, e.id);
        assertEquals(LedgerEntryEntity.TYPE_OUT, e.type);
        assertEquals("农药", e.category);
        assertEquals(6050, e.amountCent);
        assertEquals(Long.valueOf(1), e.cropId);
        assertEquals("百菌清 2 瓶", e.note);
        assertEquals(LedgerEntryEntity.SOURCE_MANUAL, e.source);
        assertEquals(LocalDateTime.of(2026, 10, 9, 10, 0), LocalDateTime.ofInstant(
                Instant.ofEpochMilli(e.date), LedgerFakes.ZONE));
        assertEquals(Boolean.FALSE, savedEvent);
        assertFalse(vm.saving.getValue());
    }

    @Test
    public void save_withoutCropOrNote() {
        vm.start(0, LedgerEntryEntity.TYPE_IN, 0);
        type("960");
        vm.save();
        LedgerEntryEntity e = ledger.added.get(0);
        assertEquals(LedgerEntryEntity.TYPE_IN, e.type);
        assertEquals("农产品销售", e.category);
        assertEquals(96000, e.amountCent);
        assertNull(e.cropId);
        assertNull(e.note);
    }

    @Test
    public void save_error_showsMessage() {
        ledger.failWrite = true;
        vm.start(0, null, 0);
        type("60");
        vm.save();
        assertEquals("请先登录", lastToast);
        assertNull(savedEvent);
        assertFalse(vm.saving.getValue());
    }

    @Test
    public void futureDate_clampedToToday() {
        vm.start(0, null, 0);
        vm.setDate(TODAY.plusDays(3));
        assertEquals(TODAY, vm.date.getValue());
    }

    @Test
    public void note_trimmedAndLimited() {
        vm.start(0, null, 0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40; i++) sb.append('字');
        vm.setNote(sb.toString());
        assertEquals(AddEntryViewModel.NOTE_MAX_LENGTH, vm.note.getValue().length());
    }

    @Test
    public void edit_prefillsAndUpdates() {
        LedgerEntryEntity stored = LedgerFakes.entry(42, LedgerEntryEntity.TYPE_IN, "政府补贴", 120050,
                TODAY.minusDays(2), "种粮补贴", LedgerEntryEntity.SOURCE_MANUAL);
        stored.cropId = 2L;
        ledger.stored = stored;
        vm.start(42, null, 0);
        assertTrue(vm.editing.getValue());
        assertTrue(vm.isEditing());
        assertEquals(LedgerEntryEntity.TYPE_IN, vm.type.getValue());
        assertEquals("政府补贴", vm.category.getValue());
        assertEquals("1200.5", vm.amount.getValue());
        assertEquals("+¥1200.5", vm.amountText.getValue());
        assertEquals(TODAY.minusDays(2), vm.date.getValue());
        assertEquals("种粮补贴", vm.note.getValue());
        assertEquals(2, vm.crop.getValue().id);

        vm.backspace();
        type("8");
        vm.save();
        LedgerEntryEntity e = ledger.added.get(0);
        assertEquals(42, e.id);
        assertEquals(1, e.userId);
        assertEquals(1234L, e.createdAt);
        assertEquals(120080, e.amountCent);
        // 保留原来的时刻
        assertEquals(stored.date, e.date);
        assertEquals(Boolean.TRUE, savedEvent);
    }

    @Test
    public void edit_missingEntry_finishes() {
        vm.start(7, null, 0);
        assertEquals(Integer.valueOf(R.string.add_entry_not_found), finishEvent);
    }

    @Test
    public void edit_recordEntry_finishes() {
        ledger.stored = LedgerFakes.entry(5, LedgerEntryEntity.TYPE_OUT, "化肥", 6000, TODAY, "硫酸钾 5 kg",
                LedgerEntryEntity.SOURCE_RECORD);
        vm.start(5, null, 0);
        assertEquals(Integer.valueOf(R.string.add_entry_record_locked), finishEvent);
    }

    @Test
    public void delete() {
        ledger.stored = LedgerFakes.entry(42, LedgerEntryEntity.TYPE_OUT, "地租", 50000, TODAY, null,
                LedgerEntryEntity.SOURCE_MANUAL);
        vm.start(42, null, 0);
        vm.delete();
        assertEquals(Arrays.asList(42L), ledger.deleted);
        assertEquals(Boolean.TRUE, deletedEvent);
    }

    @Test
    public void delete_ignoredWhenNew() {
        vm.start(0, null, 0);
        vm.delete();
        assertTrue(ledger.deleted.isEmpty());
        assertNull(deletedEvent);
    }

    @Test
    public void unselectCrop() {
        vm.start(0, null, 1);
        vm.selectCrop(null);
        assertNull(vm.crop.getValue());
        type("5");
        vm.save();
        assertNull(ledger.added.get(0).cropId);
    }
}
