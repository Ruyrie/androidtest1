package com.heban.app.di;

import android.content.Context;

import com.google.gson.Gson;
import com.heban.app.BuildConfig;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.mock.MockDataImporter;
import com.heban.app.data.local.prefs.SecureStore;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.remote.NetworkModule;
import com.heban.app.data.remote.sms.SmsService;
import com.heban.app.data.remote.sms.SmsServices;
import com.heban.app.data.repository.AiRepository;
import com.heban.app.data.repository.CommunityRepository;
import com.heban.app.data.repository.CropRepository;
import com.heban.app.data.repository.FavoriteRepository;
import com.heban.app.data.repository.LedgerRepository;
import com.heban.app.data.repository.LlmAiRepository;
import com.heban.app.data.repository.LlmVisionRepository;
import com.heban.app.data.repository.LocalCommunityRepository;
import com.heban.app.data.repository.LocalCropRepository;
import com.heban.app.data.repository.LocalFavoriteRepository;
import com.heban.app.data.repository.LocalLedgerRepository;
import com.heban.app.data.repository.LocalMallRepository;
import com.heban.app.data.repository.LocalMarketRepository;
import com.heban.app.data.repository.LocalMessageRepository;
import com.heban.app.data.repository.LocalModelServiceRepository;
import com.heban.app.data.repository.LocalPestRepository;
import com.heban.app.data.repository.LocalPointsRepository;
import com.heban.app.data.repository.LocalTaskRepository;
import com.heban.app.data.repository.MallRepository;
import com.heban.app.data.repository.MarketRepository;
import com.heban.app.data.repository.MessageRepository;
import com.heban.app.data.repository.ModelServiceRepository;
import com.heban.app.data.repository.PestRepository;
import com.heban.app.data.repository.PointsRepository;
import com.heban.app.data.repository.QWeatherRepository;
import com.heban.app.data.repository.TaskRepository;
import com.heban.app.data.repository.UserRepository;
import com.heban.app.data.repository.VisionRepository;
import com.heban.app.data.repository.WeatherRepository;
import com.heban.app.util.Notifier;

import okhttp3.OkHttpClient;

/**
 * 手写依赖容器（技术准备文档 5.2）：在 Application 中集中创建单例对象。
 * 各阶段新增的数据库表、网络服务和 Repository 在这里装配。
 */
public final class AppContainer {
    public final Context app;
    public final Gson gson = new Gson();
    public final AppDatabase db;
    public final SettingsStore settings;
    public final SecureStore secureStore = new SecureStore();
    public final OkHttpClient baseClient;
    public final CurrentUser currentUser;
    public final Notifier notifier;
    public final MockDataImporter mockImporter;
    /** 当前 flavor 的数据来源："mock" 或 "remote" */
    public final String dataSource = RepositoryModule.DATA_SOURCE;

    public final SmsService smsService;
    public final UserRepository userRepository;     // 随 flavor 变化

    // 以下 Repository 两个 flavor 共用本地实现；不加 final，便于测试替换为假数据
    public CropRepository cropRepository;
    public TaskRepository taskRepository;
    public LedgerRepository ledgerRepository;
    public WeatherRepository weatherRepository;
    public PointsRepository pointsRepository;
    public FavoriteRepository favoriteRepository;
    public PestRepository pestRepository;
    public MarketRepository marketRepository;
    public ModelServiceRepository modelServiceRepository;
    public AiRepository aiRepository;
    public VisionRepository visionRepository;
    public MessageRepository messageRepository;
    public CommunityRepository communityRepository;
    public MallRepository mallRepository;

    public AppContainer(Context context) {
        app = context.getApplicationContext();
        db = AppDatabase.get(app);
        settings = new SettingsStore(app);
        baseClient = NetworkModule.baseClient(BuildConfig.DEBUG);
        currentUser = new CurrentUser(settings);
        notifier = new Notifier(app, settings);
        mockImporter = new MockDataImporter(app, db, settings, gson);

        smsService = SmsServices.create(baseClient, gson);   // aliyun 或 fake

        userRepository = RepositoryModule.userRepository(this);

        pointsRepository = new LocalPointsRepository(db, currentUser);
        cropRepository = new LocalCropRepository(app, db, currentUser, gson);
        taskRepository = new LocalTaskRepository(app, db, currentUser, pointsRepository);
        ledgerRepository = new LocalLedgerRepository(db, currentUser, pointsRepository);
        weatherRepository = new QWeatherRepository(app, baseClient, gson, db, settings,
                userRepository, cropRepository);
        favoriteRepository = new LocalFavoriteRepository(db, currentUser);
        pestRepository = new LocalPestRepository(db);
        marketRepository = new LocalMarketRepository(db, currentUser, pointsRepository);
        modelServiceRepository = new LocalModelServiceRepository(baseClient, gson, settings, secureStore);
        aiRepository = new LlmAiRepository(app, baseClient, gson, db, settings, secureStore, currentUser,
                userRepository, cropRepository, weatherRepository);
        visionRepository = new LlmVisionRepository(app, baseClient, gson, db, settings, secureStore,
                currentUser, pestRepository, cropRepository);
        messageRepository = new LocalMessageRepository(app, db, currentUser, gson);
        communityRepository = new LocalCommunityRepository(app, db, currentUser, gson, userRepository,
                pointsRepository, aiRepository, messageRepository);
        mallRepository = new LocalMallRepository(db, currentUser, gson, pointsRepository);
    }
}
