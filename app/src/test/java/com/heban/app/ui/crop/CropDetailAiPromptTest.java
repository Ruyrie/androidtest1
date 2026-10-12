package com.heban.app.ui.crop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.CropDetail;

import org.junit.Test;

public class CropDetailAiPromptTest {

    private static CropDetail detail(String note) {
        CropDetail d = new CropDetail();
        d.crop = new CropEntity();
        d.crop.name = "番茄";
        d.crop.variety = "樱桃番茄";
        d.crop.attentionNote = note;
        d.card = new CropCard();
        d.card.stage = "结果期";
        d.card.daysPlanted = 68;
        d.plotName = "1号大棚";
        return d;
    }

    @Test
    public void noDetail_noPrompt() {
        assertNull(CropDetailViewModel.aiPrompt(null));
    }

    @Test
    public void normalCrop_asksForStageAdvice() {
        assertEquals("我种的番茄（樱桃番茄，结果期第 68 天，1号大棚）现在需要注意什么？请结合当前生长阶段给出管理建议。",
                CropDetailViewModel.aiPrompt(detail(null)));
    }

    @Test
    public void attentionCrop_asksForDiagnosis() {
        assertEquals("我种的番茄（樱桃番茄，结果期第 68 天，1号大棚）出现了「灰霉病风险」提示，请帮我诊断原因并给出防治建议。",
                CropDetailViewModel.aiPrompt(detail("灰霉病风险")));
    }
}
