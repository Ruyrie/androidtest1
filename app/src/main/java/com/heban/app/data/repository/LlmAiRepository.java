package com.heban.app.data.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.ChatMessageEntity;
import com.heban.app.data.local.entity.ChatSessionEntity;
import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.local.prefs.SecureStore;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.WeatherBundle;
import com.heban.app.data.model.WeatherState;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.remote.llm.AiPrompts;
import com.heban.app.data.remote.llm.LlmClient;
import com.heban.app.data.remote.llm.LlmConfig;
import com.heban.app.data.remote.llm.LlmConfigProvider;
import com.heban.app.data.remote.llm.VisionRecognizer;
import com.heban.app.data.remote.llm.dto.ChatMessage;
import com.heban.app.data.remote.llm.dto.ChatRequest;
import com.heban.app.util.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;

/**
 * AiRepository 的大模型实现（DeepSeek 等 OpenAI 兼容服务，SSE 流式，技术准备文档 9.3–9.4）。
 *
 * <p>会话与消息按当前账号隔离：未登录时查询为空，写操作抛出 NOT_LOGGED_IN。提问时先写入用户消息，
 * 再按 M11 设置组装系统提示词（回答风格；「所在地区与天气」：区县级地区 + 天气缓存摘要；
 * 「我的作物与农事记录」：作物 + 最近 3 条农事），带最近 6 轮历史（单条截断 2000 字）流式请求。
 * 附带图片时改用识图模型非流式回答（按表 9-4 切换通义千问 VL 备用模型），一次发射全文。
 * 没有任何可用 Key 时以 BusinessException 结束（提示到「AI 助手设置 → 模型服务」配置），不给示例回答。
 */
public final class LlmAiRepository implements AiRepository {
    static final String SOLAR_TERMS_ASSET = "mock/solar_terms.json";
    /** 读取用户信息时等待天气缓存 / 用户资料的最长时间 */
    private static final long CONTEXT_TIMEOUT_MS = 3000;

    private final Context app;
    private final OkHttpClient baseClient;
    private final Gson gson;
    private final AppDatabase db;
    private final SettingsStore settings;
    private final SecureStore secureStore;
    private final CurrentUser user;
    private final UserRepository userRepository;
    private final CropRepository cropRepository;
    private final WeatherRepository weatherRepository;

    private final LlmClient client;
    private final LlmConfigProvider configs;
    private final VisionRecognizer vision;
    private volatile NavigableMap<LocalDate, String> solarTerms;

    public LlmAiRepository(Context app, OkHttpClient baseClient, Gson gson, AppDatabase db,
                           SettingsStore settings, SecureStore secureStore, CurrentUser user,
                           UserRepository userRepository, CropRepository cropRepository,
                           WeatherRepository weatherRepository) {
        this.app = app;
        this.baseClient = baseClient;
        this.gson = gson;
        this.db = db;
        this.settings = settings;
        this.secureStore = secureStore;
        this.user = user;
        this.userRepository = userRepository;
        this.cropRepository = cropRepository;
        this.weatherRepository = weatherRepository;
        this.client = new LlmClient(baseClient, gson);
        this.configs = new LlmConfigProvider(settings, secureStore);
        this.vision = new VisionRecognizer(client, gson);
    }

    // ———————————————————————— 会话与消息 ————————————————————————

    @Override
    public Flowable<List<ChatSessionEntity>> observeSessions() {
        return UserScope.observe(user, Collections::<ChatSessionEntity>emptyList,
                uid -> db.chatDao().observeSessions(uid));
    }

    @Override
    public Flowable<List<ChatMessageEntity>> observeMessages(long sessionId) {
        return UserScope.observe(user, Collections::<ChatMessageEntity>emptyList,
                uid -> db.chatDao().observeMessagesForUser(sessionId, uid));
    }

    @Override
    public Single<Long> createSession(String firstQuestion) {
        return UserScope.require(user).map(uid -> {
            long now = System.currentTimeMillis();
            ChatSessionEntity s = new ChatSessionEntity();
            s.userId = uid;
            s.title = AiPrompts.sessionTitle(firstQuestion);
            s.createdAt = now;
            s.updatedAt = now;
            return db.chatDao().insertSessionSync(s);
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Flowable<String> ask(long sessionId, String question, @Nullable String imagePath) {
        return UserScope.require(user)
                .map(uid -> {
                    requireSession(sessionId, uid);
                    String q = question == null ? "" : question.trim();
                    String stored = imagePath == null || imagePath.isEmpty() ? null : storeChatImage(imagePath);
                    if (q.isEmpty() && stored == null) throw emptyQuestion();
                    long now = System.currentTimeMillis();
                    ChatMessageEntity m = new ChatMessageEntity();
                    m.sessionId = sessionId;
                    m.role = ChatMessageEntity.ROLE_USER;
                    m.content = q;
                    m.imagePath = stored;
                    m.createdAt = now;
                    m.id = db.chatDao().insertMessageSync(m);
                    db.chatDao().touchSessionSync(sessionId, now);
                    return m;
                })
                .flatMapPublisher(m -> answer(sessionId, m))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Completable saveAnswer(long sessionId, String answer, boolean incomplete) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() -> {
            requireSession(sessionId, uid);
            String text = answer == null ? "" : answer;
            if (text.trim().isEmpty()) {
                // 没有任何内容时，只有在等待回答（最后一条是提问）时才保存「回答未完成」
                ChatMessageEntity last = db.chatDao().lastMessageSync(sessionId);
                if (last == null || !ChatMessageEntity.ROLE_USER.equals(last.role)) return;
            }
            long now = System.currentTimeMillis();
            ChatMessageEntity m = new ChatMessageEntity();
            m.sessionId = sessionId;
            m.role = ChatMessageEntity.ROLE_ASSISTANT;
            m.content = text;
            m.status = incomplete ? ChatMessageEntity.STATUS_INCOMPLETE : ChatMessageEntity.STATUS_OK;
            m.createdAt = now;
            db.runInTransaction(() -> {
                db.chatDao().insertMessageSync(m);
                db.chatDao().touchSessionSync(sessionId, now);
            });
        })).subscribeOn(Schedulers.io());
    }

    @Override
    public Flowable<String> regenerate(long sessionId, long assistantMessageId) {
        return UserScope.require(user)
                .map(uid -> {
                    requireSession(sessionId, uid);
                    ChatMessageEntity a = db.chatDao().getMessageSync(assistantMessageId, uid);
                    if (a == null || a.sessionId != sessionId || !ChatMessageEntity.ROLE_ASSISTANT.equals(a.role)) {
                        throw UserScope.notFound("这条回答不存在");
                    }
                    ChatMessageEntity question = previousUserMessage(db.chatDao().listMessagesSync(sessionId), a);
                    if (question == null) throw UserScope.notFound("找不到对应的问题");
                    db.chatDao().deleteMessageSync(a.id);
                    return question;
                })
                .flatMapPublisher(q -> answer(sessionId, q))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Completable setFeedback(long messageId, int feedback) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() ->
                db.chatDao().setFeedbackSync(messageId, uid, feedback))).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable deleteSession(long sessionId) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() -> {
            List<String> images = db.runInTransaction(() -> {
                if (db.chatDao().getSessionSync(sessionId, uid) == null) return Collections.<String>emptyList();
                List<String> paths = db.chatDao().imagePathsInSessionSync(sessionId);
                db.chatDao().deleteMessagesInSessionSync(sessionId);
                db.chatDao().deleteSessionSync(sessionId, uid);
                return paths;
            });
            deleteImages(images);
        })).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable clearAll() {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() -> {
            List<String> images = db.runInTransaction(() -> {
                List<String> paths = db.chatDao().imagePathsForUserSync(uid);
                db.chatDao().deleteMessagesForUserSync(uid);
                db.chatDao().deleteSessionsForUserSync(uid);
                return paths;
            });
            deleteImages(images);
        })).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<long[]> historyStats() {
        return user.userId().observeOn(Schedulers.io()).map(uid -> {
            if (uid <= 0) return new long[]{0, 0};
            long count = db.chatDao().countSessionsSync(uid);
            long bytes = db.chatDao().textBytesSync(uid);
            for (String p : db.chatDao().imagePathsForUserSync(uid)) {
                File f = ImageUtils.toFile(p);
                if (f != null && f.isFile()) bytes += f.length();
            }
            return new long[]{count, bytes};
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<String> quickAnswer(String question) {
        return Single.fromCallable(() -> {
                    String q = question == null ? "" : question.trim();
                    if (q.isEmpty()) throw emptyQuestion();
                    LlmConfig cfg = configs.chat();
                    if (cfg == null) throw LlmConfigProvider.notConfigured();
                    LocalDate today = LocalDate.now(zone());
                    AiPrompts.UserContext ctx = AiPrompts.UserContext.dateOnly(AiPrompts.formatDate(today),
                            AiPrompts.solarTermLabel(solarTerms(), today));
                    String system = AiPrompts.system(style(), ctx);
                    return new TextAsk(cfg, AiPrompts.quickRequest(cfg.model, system, q));
                })
                .flatMap(t -> client.complete(t.cfg, t.request))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Completable discardIfNotSaving(long sessionId) {
        return settings.get(SettingsStore.AI_SAVE_HISTORY, true)
                .flatMapCompletable(save -> {
                    if (save) return Completable.complete();
                    return user.userId().flatMapCompletable(uid -> uid > 0
                            ? deleteSession(sessionId) : Completable.complete());
                })
                .subscribeOn(Schedulers.io());
    }

    // ———————————————————————— 提问 ————————————————————————

    /**
     * 回答 question（已保存的用户消息）：历史取它之前的消息；带图片时用识图模型非流式回答。
     * 没有可用的模型配置时以 BusinessException 结束。
     */
    private Flowable<String> answer(long sessionId, ChatMessageEntity question) {
        return Single.fromCallable(() -> {
            List<ChatMessageEntity> all = db.chatDao().listMessagesSync(sessionId);
            List<ChatMessageEntity> before = new ArrayList<>();
            for (ChatMessageEntity m : all) {
                if (m.id == question.id) break;
                before.add(m);
            }
            List<ChatMessage> history = AiPrompts.history(before);
            boolean withImage = question.imagePath != null && !question.imagePath.isEmpty();
            if (withImage) {
                LlmConfigProvider.VisionPair pair = configs.vision();
                if (pair == null) throw LlmConfigProvider.notConfigured();
                String system = AiPrompts.system(style(), buildContext());
                String dataUrl = imageDataUrl(question.imagePath);
                return (Object) new ImageAsk(pair, system, history, question.content, dataUrl);
            }
            LlmConfig cfg = configs.chat();
            if (cfg == null) throw LlmConfigProvider.notConfigured();
            String system = AiPrompts.system(style(), buildContext());
            return (Object) new TextAsk(cfg, AiPrompts.chatRequest(cfg.model, system, history, question.content));
        }).subscribeOn(Schedulers.io()).flatMapPublisher(p -> {
            if (p instanceof ImageAsk) {
                ImageAsk a = (ImageAsk) p;
                return vision.answer(a.pair.primary, a.pair.backup,
                                cfg -> AiPrompts.imageChatRequest(cfg.model, a.system, a.history, a.question, a.dataUrl))
                        .map(ans -> ans.text)
                        .toFlowable();
            }
            TextAsk t = (TextAsk) p;
            return client.stream(t.cfg, t.request);
        });
    }

    private static final class TextAsk {
        final LlmConfig cfg;
        final ChatRequest request;

        TextAsk(LlmConfig cfg, ChatRequest request) {
            this.cfg = cfg;
            this.request = request;
        }
    }

    private static final class ImageAsk {
        final LlmConfigProvider.VisionPair pair;
        final String system;
        final List<ChatMessage> history;
        final String question;
        final String dataUrl;

        ImageAsk(LlmConfigProvider.VisionPair pair, String system, List<ChatMessage> history, String question,
                 String dataUrl) {
            this.pair = pair;
            this.system = system;
            this.history = history;
            this.question = question;
            this.dataUrl = dataUrl;
        }
    }

    @Nullable
    static ChatMessageEntity previousUserMessage(List<ChatMessageEntity> all, ChatMessageEntity assistant) {
        ChatMessageEntity found = null;
        for (ChatMessageEntity m : all) {
            if (m.id == assistant.id) break;
            if (ChatMessageEntity.ROLE_USER.equals(m.role)) found = m;
        }
        return found;
    }

    private static BusinessException emptyQuestion() {
        return new BusinessException("EMPTY_QUESTION", "请输入问题");
    }

    private void requireSession(long sessionId, long uid) {
        if (db.chatDao().getSessionSync(sessionId, uid) == null) throw UserScope.notFound("会话不存在");
    }

    // ———————————————————————— 用户信息 ————————————————————————

    private AiPrompts.Style style() {
        return AiPrompts.Style.of(settings.get(SettingsStore.AI_STYLE, AiPrompts.Style.DETAIL.name()).blockingGet());
    }

    /** 按 M11 开关组装【用户信息】；单项读取失败时省略该项 */
    private AiPrompts.UserContext buildContext() {
        ZoneId zone = zone();
        LocalDate today = LocalDate.now(zone);
        AiPrompts.UserContext ctx = new AiPrompts.UserContext();
        ctx.date = AiPrompts.formatDate(today);
        ctx.solarTerm = AiPrompts.solarTermLabel(solarTerms(), today);
        ctx.useRegionWeather = settings.get(SettingsStore.AI_USE_WEATHER, true).blockingGet();
        ctx.useCrops = settings.get(SettingsStore.AI_USE_CROPS, true).blockingGet();
        if (ctx.useRegionWeather) {
            ctx.region = AiPrompts.formatRegion(currentRegion());
            ctx.weather = AiPrompts.weatherSummary(cachedWeather(), today, zone);
        }
        if (ctx.useCrops) {
            List<CropCard> cards = cropCards();
            ctx.crops = AiPrompts.cropsText(cards);
            ctx.records = recentRecords(zone);
        }
        return ctx;
    }

    @Nullable
    private String currentRegion() {
        try {
            UserEntity u = userRepository.currentUser()
                    .firstElement()
                    .timeout(CONTEXT_TIMEOUT_MS, TimeUnit.MILLISECONDS, Maybe.empty())
                    .blockingGet();
            return u == null ? null : u.region;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** WeatherRepository 当前（缓存）的天气；没有缓存或超时时为 null */
    @Nullable
    private WeatherBundle cachedWeather() {
        try {
            WeatherState s = weatherRepository.observe()
                    .filter(st -> st.bundle != null || !st.loading)
                    .firstElement()
                    .timeout(CONTEXT_TIMEOUT_MS, TimeUnit.MILLISECONDS, Maybe.empty())
                    .blockingGet();
            return s == null ? null : s.bundle;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private List<CropCard> cropCards() {
        try {
            return cropRepository.observeCropCards(null)
                    .firstOrError()
                    .timeout(CONTEXT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .blockingGet();
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }

    /** 最近 3 条农事（所有作物），如「10/08 番茄 施肥 硫酸钾 5kg」 */
    @Nullable
    private String recentRecords(ZoneId zone) {
        try {
            List<CropEntity> crops = cropRepository.listCrops().blockingGet();
            if (crops == null || crops.isEmpty()) return null;
            Map<Long, String> names = new HashMap<>();
            List<FarmRecordEntity> all = new ArrayList<>();
            for (CropEntity c : crops) {
                names.put(c.id, c.name);
                List<FarmRecordEntity> list = db.farmRecordDao().listByCropSync(c.id);
                int n = Math.min(list.size(), AiPrompts.RECENT_RECORDS);
                all.addAll(list.subList(0, n));   // 每个作物已按日期倒序
            }
            return AiPrompts.recordsText(AiPrompts.latestRecords(all, AiPrompts.RECENT_RECORDS), names, zone);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private NavigableMap<LocalDate, String> solarTerms() {
        NavigableMap<LocalDate, String> t = solarTerms;
        if (t != null) return t;
        try (InputStream in = app.getAssets().open(SOLAR_TERMS_ASSET)) {
            t = AiPrompts.parseSolarTerms(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            t = new TreeMap<>();
        }
        solarTerms = t;
        return t;
    }

    private static ZoneId zone() {
        return ZoneId.systemDefault();
    }

    // ———————————————————————— 图片 ————————————————————————

    /**
     * 把用户附带的图片压缩（1280px、质量 80）后保存到 filesDir/chat/，返回 file:// 地址；
     * 已在该目录中的图片直接使用。
     */
    private String storeChatImage(String pathOrUri) throws IOException {
        File f = ImageUtils.toFile(pathOrUri);
        if (f != null && ImageUtils.isInDir(app, f, ImageUtils.DIR_CHAT) && f.isFile()) {
            return ImageUtils.toFileUri(f);
        }
        Bitmap bmp = f != null ? ImageUtils.decode(f) : ImageUtils.decode(app, Uri.parse(pathOrUri));
        File saved = ImageUtils.saveJpeg(app, bmp, ImageUtils.DIR_CHAT);
        return ImageUtils.toFileUri(saved);
    }

    private String imageDataUrl(String storedPath) throws IOException {
        File f = ImageUtils.toFile(storedPath);
        if (f == null || !f.isFile()) throw new BusinessException("IMAGE_MISSING", "图片已不存在，请重新选择");
        return ImageUtils.toDataUrl(ImageUtils.readBytes(f));
    }

    private void deleteImages(List<String> paths) {
        if (paths == null) return;
        for (String p : paths) ImageUtils.deleteInDir(app, p, ImageUtils.DIR_CHAT);
    }
}
