package com.heban.app.ui.common;

import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * 验证码按钮倒计时（技术准备文档 8.3）：发送成功后 60 秒内不可再次获取。
 * 按钮文字：获取验证码 → 「42s 后重发」 → 重新获取。
 */
public final class SmsCountdown {
    public static final int SECONDS = 60;

    public final MutableLiveData<Integer> secondsLeft = new MutableLiveData<>(0);
    public final MutableLiveData<String> buttonText = new MutableLiveData<>("获取验证码");
    public final MutableLiveData<Boolean> canSend = new MutableLiveData<>(true);

    private Disposable timer;

    public void start(CompositeDisposable disposables) {
        cancel();
        canSend.setValue(false);
        timer = Observable.intervalRange(0, SECONDS + 1, 0, 1, TimeUnit.SECONDS)
                .map(i -> SECONDS - i.intValue())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(left -> {
                    secondsLeft.setValue(left);
                    if (left > 0) {
                        buttonText.setValue(left + "s 后重发");
                    } else {
                        buttonText.setValue("重新获取");
                        canSend.setValue(true);
                    }
                });
        disposables.add(timer);
    }

    public void cancel() {
        if (timer != null) timer.dispose();
    }

    public boolean isRunning() {
        Integer s = secondsLeft.getValue();
        return s != null && s > 0;
    }
}
