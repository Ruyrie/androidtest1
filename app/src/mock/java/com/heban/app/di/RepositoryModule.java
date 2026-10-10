package com.heban.app.di;

import com.heban.app.data.repository.MockUserRepository;
import com.heban.app.data.repository.UserRepository;

/**
 * mock 渠道（现阶段）：由后端提供的数据先用本地模拟实现（技术准备文档 5.3）。
 */
public final class RepositoryModule {
    public static final String DATA_SOURCE = "mock";

    private RepositoryModule() {
    }

    public static UserRepository userRepository(AppContainer c) {
        return new MockUserRepository(c.db.userDao(), c.db.appealDao(), c.settings,
                c.secureStore, c.smsService, System::currentTimeMillis);
    }
}
