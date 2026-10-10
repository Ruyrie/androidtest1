package com.heban.app.data.remote.sms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SmsServicesTest {

    @Test
    public void aliyun_onlyWhenProviderIsAliyunAndKeysConfigured() {
        assertTrue(SmsServices.useAliyun("aliyun", "LTAIxxxx", "secret"));
        assertTrue(SmsServices.useAliyun(" Aliyun ", "LTAIxxxx", "secret"));
    }

    @Test
    public void fake_whenProviderIsFakeOrKeysMissing() {
        assertFalse(SmsServices.useAliyun("fake", "LTAIxxxx", "secret"));
        assertFalse(SmsServices.useAliyun("", "LTAIxxxx", "secret"));
        assertFalse(SmsServices.useAliyun(null, "LTAIxxxx", "secret"));
        assertFalse(SmsServices.useAliyun("aliyun", "", "secret"));
        assertFalse(SmsServices.useAliyun("aliyun", "LTAIxxxx", " "));
        assertFalse(SmsServices.useAliyun("aliyun", null, null));
    }
}
