package com.heban.app.di;

import com.heban.app.data.repository.RemoteUserRepository;
import com.heban.app.data.repository.UserRepository;

/**
 * remote 渠道（后端接入后）：与 mock 渠道同名同签名，返回调用后端接口的实现（技术准备文档 5.3）。
 */
public final class RepositoryModule {
    public static final String DATA_SOURCE = "remote";

    private RepositoryModule() {
    }

    public static UserRepository userRepository(AppContainer c) {
        return new RemoteUserRepository(c.db, c.settings, c.secureStore, c.smsService);
    }
}
