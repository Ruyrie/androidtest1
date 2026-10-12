package com.heban.app.ui.discover.ledger;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.R;
import com.heban.app.data.repository.LedgerRepository;

/** 记账类别图标（原型 AddEntry）：支出 8 类、收入 4 类，顺序与 {@link LedgerRepository} 的类别一致。 */
public final class LedgerCategoryIcons {
    /** 化肥、农药、种子种苗、人工、水电灌溉、农机、地租、其他 */
    private static final int[] EXPENSE = {
            R.drawable.ic_compost, R.drawable.ic_sanitizer, R.drawable.ic_psychiatry, R.drawable.ic_groups,
            R.drawable.ic_water_drop, R.drawable.ic_agriculture, R.drawable.ic_landscape, R.drawable.ic_more_horiz
    };
    /** 农产品销售、政府补贴、代耕作业、其他 */
    private static final int[] INCOME = {
            R.drawable.ic_sell, R.drawable.ic_account_balance, R.drawable.ic_agriculture, R.drawable.ic_more_horiz
    };

    private LedgerCategoryIcons() {
    }

    /** 收支类型对应的类别列表 */
    @NonNull
    public static String[] categories(@Nullable String type) {
        return LedgerFormat.isIncome(type) ? LedgerRepository.INCOME_CATEGORIES : LedgerRepository.EXPENSE_CATEGORIES;
    }

    @DrawableRes
    public static int icon(@Nullable String type, @Nullable String category) {
        String[] names = categories(type);
        int[] icons = LedgerFormat.isIncome(type) ? INCOME : EXPENSE;
        for (int i = 0; i < names.length && i < icons.length; i++) {
            if (names[i].equals(category)) return icons[i];
        }
        return R.drawable.ic_more_horiz;
    }
}
