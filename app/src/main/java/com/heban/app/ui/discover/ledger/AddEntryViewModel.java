package com.heban.app.ui.discover.ledger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.R;
import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.data.repository.LedgerRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * D11 记一笔（技术准备文档 12.4）：自定义数字键盘维护金额字符串（{@link AmountInput}，最多两位小数）；
 * 支出 8 类、收入 4 类，切换收支时重置类别；可关联作物、选择日期（不能晚于今天）、添加备注。
 * 编辑（entryId != 0）时预填原有内容，保存即更新，并可删除。
 */
public class AddEntryViewModel extends BaseViewModel {
    /** 备注最多 30 个字 */
    public static final int NOTE_MAX_LENGTH = 30;

    public final MutableLiveData<String> type = new MutableLiveData<>(LedgerEntryEntity.TYPE_OUT);
    public final MutableLiveData<String> category =
            new MutableLiveData<>(LedgerRepository.EXPENSE_CATEGORIES[0]);
    /** 键盘输入的金额字符串，如「60」「12.5」「0.」 */
    public final MutableLiveData<String> amount = new MutableLiveData<>(AmountInput.ZERO);
    /** 「支出 · 化肥」 */
    public final MediatorLiveData<String> headerText = new MediatorLiveData<>();
    /** 「−¥60」「+¥960」 */
    public final MediatorLiveData<String> amountText = new MediatorLiveData<>();

    /** 当前账号的作物（作物 Chip 的选项，加载前为 null） */
    public final MutableLiveData<List<CropCard>> crops = new MutableLiveData<>();
    /** 关联的作物；null 表示不关联作物 */
    public final MutableLiveData<CropCard> crop = new MutableLiveData<>(null);
    public final MutableLiveData<LocalDate> date = new MutableLiveData<>();
    public final MutableLiveData<String> note = new MutableLiveData<>("");

    /** 编辑已有账目（顶栏显示「删除」） */
    public final MutableLiveData<Boolean> editing = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);

    public final SingleLiveEvent<Integer> messageRes = new SingleLiveEvent<>();
    /** 保存成功：true 表示更新了已有账目 */
    public final SingleLiveEvent<Boolean> saved = new SingleLiveEvent<>();
    /** 删除成功 */
    public final SingleLiveEvent<Boolean> deleted = new SingleLiveEvent<>();
    /** 无法编辑（账目不存在 / 来自农事记录）：带提示关闭页面 */
    public final SingleLiveEvent<Integer> finish = new SingleLiveEvent<>();

    private final LedgerRepository ledger;
    private final CropRepository cropRepo;
    private final Clock clock;

    private boolean started;
    private long entryId;
    /** 编辑时的原条目（保留 userId、createdAt、来源等） */
    @Nullable private LedgerEntryEntity original;
    /** 预选 / 原条目关联的作物 id，作物列表加载后匹配 */
    private long wantedCropId;
    /** 编辑时保留原来的时刻；新增时为 null，保存时取当前时刻 */
    @Nullable private LocalTime time;

    public AddEntryViewModel(AppContainer c) {
        this(c, c.ledgerRepository, c.cropRepository, Clock.systemDefaultZone());
    }

    @VisibleForTesting
    AddEntryViewModel(AppContainer c, LedgerRepository ledger, CropRepository cropRepo, Clock clock) {
        super(c);
        this.ledger = ledger;
        this.cropRepo = cropRepo;
        this.clock = clock;
        date.setValue(LocalDate.now(clock));
        headerText.addSource(type, v -> updateTexts());
        headerText.addSource(category, v -> updateTexts());
        amountText.addSource(type, v -> updateTexts());
        amountText.addSource(amount, v -> updateTexts());
        updateTexts();
    }

    /**
     * 页面参数（只在第一次调用时生效）：entryId != 0 时编辑；entryType 为 IN / OUT 时预选收支；
     * cropId > 0 时预选作物。
     */
    public void start(long entryId, @Nullable String entryType, long cropId) {
        if (started) return;
        started = true;
        this.entryId = entryId;
        wantedCropId = cropId;
        if (LedgerEntryEntity.TYPE_IN.equals(entryType) || LedgerEntryEntity.TYPE_OUT.equals(entryType)) {
            selectType(entryType);
        }
        add(cropRepo.observeCropCards(null)
                .onErrorReturnItem(Collections.emptyList())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCrops, e -> { }));
        if (entryId != 0) {
            editing.setValue(true);
            add(ledger.getEntry(entryId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onEntry, e -> finish.setValue(R.string.add_entry_not_found)));
        }
    }

    public boolean isEditing() {
        return entryId != 0;
    }

    @NonNull
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** 当前收支类型的类别 */
    @NonNull
    public String[] categories() {
        return LedgerCategoryIcons.categories(type.getValue());
    }

    /** 切换收支：类别重置为该类型的第一个（技术准备文档 12.4） */
    public void selectType(@NonNull String t) {
        if (t.equals(type.getValue())) return;
        type.setValue(t);
        category.setValue(LedgerCategoryIcons.categories(t)[0]);
    }

    public void selectCategory(@NonNull String name) {
        if (Arrays.asList(categories()).contains(name) && !name.equals(category.getValue())) {
            category.setValue(name);
        }
    }

    /** 数字键盘：「0」–「9」、「.」、「00」 */
    public void press(@NonNull String key) {
        String next = AmountInput.press(amount.getValue(), key);
        if (!next.equals(amount.getValue())) amount.setValue(next);
    }

    public void backspace() {
        amount.setValue(AmountInput.backspace(amount.getValue()));
    }

    public void clear() {
        amount.setValue(AmountInput.clear());
    }

    /** 关联作物；null 表示不关联作物 */
    public void selectCrop(@Nullable CropCard c) {
        wantedCropId = c == null ? 0 : c.id;
        crop.setValue(c);
    }

    /** 选择日期：不能晚于今天 */
    public void setDate(@NonNull LocalDate d) {
        LocalDate today = today();
        date.setValue(d.isAfter(today) ? today : d);
    }

    public void setNote(@Nullable String text) {
        String t = text == null ? "" : text.trim();
        if (t.length() > NOTE_MAX_LENGTH) t = t.substring(0, NOTE_MAX_LENGTH);
        note.setValue(t);
    }

    /** 保存：金额必须大于 0；新增（id = 0）或更新（id != 0） */
    public void save() {
        if (Boolean.TRUE.equals(saving.getValue())) return;
        long cents = AmountInput.toCents(amount.getValue());
        if (cents <= 0) {
            messageRes.setValue(R.string.add_entry_amount_required);
            return;
        }
        LedgerEntryEntity e = buildEntry(cents);
        boolean update = e.id != 0;
        saving.setValue(true);
        add(ledger.addEntry(e)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(id -> {
                    saving.setValue(false);
                    saved.setValue(update);
                }, err -> {
                    saving.setValue(false);
                    toast.setValue(AppError.from(err).message);
                }));
    }

    /** 删除正在编辑的账目 */
    public void delete() {
        if (entryId == 0 || Boolean.TRUE.equals(saving.getValue())) return;
        saving.setValue(true);
        add(ledger.deleteEntry(entryId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    saving.setValue(false);
                    deleted.setValue(true);
                }, err -> {
                    saving.setValue(false);
                    toast.setValue(AppError.from(err).message);
                }));
    }

    @VisibleForTesting
    @NonNull
    LedgerEntryEntity buildEntry(long cents) {
        LedgerEntryEntity e = new LedgerEntryEntity();
        LedgerEntryEntity o = original;
        if (o != null) {
            e.id = o.id;
            e.userId = o.userId;
            e.createdAt = o.createdAt;
            e.source = o.source;
            e.recordId = o.recordId;
        } else {
            e.source = LedgerEntryEntity.SOURCE_MANUAL;
        }
        String t = type.getValue();
        e.type = LedgerEntryEntity.TYPE_IN.equals(t) ? LedgerEntryEntity.TYPE_IN : LedgerEntryEntity.TYPE_OUT;
        String cat = category.getValue();
        e.category = cat == null || cat.isEmpty() ? LedgerCategoryIcons.categories(e.type)[0] : cat;
        e.amountCent = cents;
        CropCard c = crop.getValue();
        e.cropId = c == null ? null : c.id;
        LocalDate d = date.getValue() == null ? today() : date.getValue();
        LocalTime tm = time != null ? time : LocalTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        e.date = LocalDateTime.of(d, tm).atZone(clock.getZone()).toInstant().toEpochMilli();
        String n = note.getValue() == null ? "" : note.getValue().trim();
        e.note = n.isEmpty() ? null : n;
        return e;
    }

    private void onEntry(LedgerEntryEntity e) {
        if (LedgerFormat.isFromRecord(e)) {
            finish.setValue(R.string.add_entry_record_locked);
            return;
        }
        original = e;
        String t = LedgerEntryEntity.TYPE_IN.equals(e.type) ? LedgerEntryEntity.TYPE_IN : LedgerEntryEntity.TYPE_OUT;
        type.setValue(t);
        boolean known = Arrays.asList(LedgerCategoryIcons.categories(t)).contains(e.category);
        category.setValue(known ? e.category : LedgerCategoryIcons.categories(t)[0]);
        amount.setValue(AmountInput.fromCents(e.amountCent));
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(e.date), clock.getZone());
        date.setValue(dt.toLocalDate());
        time = dt.toLocalTime();
        note.setValue(e.note == null ? "" : e.note);
        wantedCropId = e.cropId == null ? 0 : e.cropId;
        matchCrop();
    }

    private void onCrops(List<CropCard> list) {
        crops.setValue(list == null ? Collections.emptyList() : list);
        matchCrop();
    }

    private void matchCrop() {
        List<CropCard> list = crops.getValue();
        if (list == null) return;
        CropCard match = null;
        if (wantedCropId > 0) {
            for (CropCard c : list) {
                if (c != null && c.id == wantedCropId) {
                    match = c;
                    break;
                }
            }
        }
        crop.setValue(match);
    }

    private void updateTexts() {
        headerText.setValue(LedgerFormat.entryHeader(type.getValue(), category.getValue()));
        amountText.setValue(LedgerFormat.amountDisplay(type.getValue(), amount.getValue()));
    }
}
