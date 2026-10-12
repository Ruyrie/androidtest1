package com.heban.app.ui.discover.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.data.local.entity.ChatSessionEntity;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.AiRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/** 历史会话底部弹窗（D06 溢出菜单 / M11「历史会话」）：会话列表与删除。 */
public class AiChatHistoryViewModel extends BaseViewModel {
    private final AiRepository repo;
    private boolean started;

    /** null 表示加载中 */
    public final MutableLiveData<List<ChatSessionEntity>> sessions = new MutableLiveData<>(null);
    /** 已删除的会话 id */
    public final SingleLiveEvent<Long> deleted = new SingleLiveEvent<>();

    public AiChatHistoryViewModel(AppContainer c) {
        this(c, c.aiRepository);
    }

    AiChatHistoryViewModel(@Nullable AppContainer c, @NonNull AiRepository repo) {
        super(c);
        this.repo = repo;
    }

    public void start() {
        if (started) return;
        started = true;
        add(repo.observeSessions()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sessions::setValue, e -> toast.setValue(AppError.from(e).message)));
    }

    public void delete(long sessionId) {
        add(repo.deleteSession(sessionId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> deleted.setValue(sessionId), e -> toast.setValue(AppError.from(e).message)));
    }
}
