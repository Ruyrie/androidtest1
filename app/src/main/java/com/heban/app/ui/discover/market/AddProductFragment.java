package com.heban.app.ui.discover.market;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavOptions;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.heban.app.R;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.repository.PointsRules;
import com.heban.app.databinding.FragmentAddProductBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;
import com.heban.app.util.ImagePaths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** D09 发布商品（原型 AddProduct.dc.html）。发布成功后进入新商品的 D08 并移出本页。 */
public class AddProductFragment extends BaseFragment {
    private static final String[] CATEGORIES = {ProductEntity.CAT_PRODUCE, ProductEntity.CAT_SUPPLY,
            ProductEntity.CAT_SEEDLING, ProductEntity.CAT_MACHINE};
    private static final int[] SUB_ARRAYS = {R.array.add_product_sub_produce, R.array.add_product_sub_supply,
            R.array.add_product_sub_seedling, R.array.add_product_sub_machine};

    private FragmentAddProductBinding binding;
    private AddProductViewModel vm;
    /** 分类下拉的每一项：{大类, 细分} */
    private final List<String[]> categoryItems = new ArrayList<>();
    private Chip[] tradeChips;

    /** 系统照片选择器，一次最多 9 张（超出剩余名额的部分由 ViewModel 丢弃并提示） */
    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhotos = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(AddProductViewModel.MAX_PHOTOS),
            uris -> viewModel().addPhotos(uris));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddProductBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel();
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());
        binding.btnPublish.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            hideKeyboard();
            vm.publish();
        });

        setUpPhotos();
        setUpTitle();
        setUpCategory();
        setUpQuantityAndPrice();
        setUpTradeWays();
        setUpLocation();
        setUpPin();
        setUpRecord();

        vm.toast.observe(getViewLifecycleOwner(), this::showMessage);
        vm.messageRes.observe(getViewLifecycleOwner(), res -> showMessage(getString(res)));
        vm.photoLimit.observe(getViewLifecycleOwner(),
                max -> showMessage(getString(R.string.add_product_photos_limit, max)));
        vm.published.observe(getViewLifecycleOwner(), this::onPublished);

        // 游客只能浏览：打开即提示登录
        if (savedInstanceState == null && !container().currentUser.isSignedIn()) LoginRequired.check(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tradeChips = null;
        binding = null;
    }

    private AddProductViewModel viewModel() {
        if (vm == null) vm = viewModel(AddProductViewModel.class);
        return vm;
    }

    private void showMessage(@Nullable String text) {
        if (binding != null && text != null) Snackbars.show(binding.getRoot(), text);
    }

    private void hideKeyboard() {
        View root = binding.getRoot();
        InputMethodManager imm = ContextCompat.getSystemService(requireContext(), InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
        View focus = root.findFocus();
        if (focus != null) focus.clearFocus();
    }

    /** 发布成功：进入新商品详情（移出 D09），Snackbar「已发布」 */
    private void onPublished(Long id) {
        if (id == null) return;
        hideKeyboard();
        View content = requireActivity().findViewById(android.R.id.content);
        Bundle args = new Bundle();
        args.putLong(NavArgs.PRODUCT_ID, id);
        nav().navigate(R.id.productFragment, args, new NavOptions.Builder()
                .setPopUpTo(R.id.addProductFragment, true)
                .build());
        content.post(() -> Snackbar.make(content, getString(R.string.add_product_published), Snackbar.LENGTH_LONG)
                .show());
    }

    // ---------- 图片 ----------

    private void setUpPhotos() {
        binding.tileAdd.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            if (vm.remainingPhotos() <= 0) {
                showMessage(getString(R.string.add_product_photos_limit, AddProductViewModel.MAX_PHOTOS));
                return;
            }
            pickPhotos.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
        binding.tileVideo.setOnClickListener(v -> showMessage(getString(R.string.feature_not_open)));
        vm.photos.observe(getViewLifecycleOwner(), this::renderPhotos);
        vm.photoBusy.observe(getViewLifecycleOwner(), busy -> {
            boolean on = Boolean.TRUE.equals(busy);
            binding.photoProgress.setVisibility(on ? View.VISIBLE : View.GONE);
            binding.tvPhotoCount.setVisibility(on ? View.GONE : View.VISIBLE);
            binding.tileAdd.setEnabled(!on);
        });
        vm.photosError.observe(getViewLifecycleOwner(), err -> {
            binding.tvPhotoHint.setText(err != null ? err : R.string.add_product_photo_hint);
            binding.tvPhotoHint.setTextColor(MarketStyles.color(binding.tvPhotoHint, err != null
                    ? androidx.appcompat.R.attr.colorError
                    : com.google.android.material.R.attr.colorOnSurfaceVariant));
        });
    }

    private void renderPhotos(@Nullable List<String> list) {
        List<String> photos = list == null ? Collections.emptyList() : list;
        ViewGroup strip = binding.photoStrip;
        strip.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < photos.size(); i++) {
            View tile = inflater.inflate(R.layout.add_product_item_photo, strip, false);
            ImageView image = tile.findViewById(R.id.iv_photo);
            Glide.with(image).load(ImagePaths.toUri(photos.get(i)))
                    .placeholder((Drawable) null)               // 方块底色 + 图片图标即占位
                    .error((Drawable) null)
                    .centerCrop()
                    .into(image);
            tile.findViewById(R.id.tv_cover).setVisibility(i == 0 ? View.VISIBLE : View.GONE);
            tile.setContentDescription(getString(R.string.add_product_photo_item, i + 1)
                    + (i == 0 ? getString(R.string.product_sep) + getString(R.string.add_product_cover) : ""));
            int index = i;
            tile.setOnClickListener(v -> showPhotoActions(index));
            strip.addView(tile);
        }
        binding.tvPhotoCount.setText(getString(R.string.add_product_count, photos.size(),
                AddProductViewModel.MAX_PHOTOS));
        binding.tileAdd.setVisibility(photos.size() < AddProductViewModel.MAX_PHOTOS ? View.VISIBLE : View.GONE);
    }

    private void showPhotoActions(int index) {
        List<String> items = new ArrayList<>();
        if (index > 0) items.add(getString(R.string.add_product_photo_set_cover));
        items.add(getString(R.string.add_product_photo_delete));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.add_product_photo_item, index + 1))
                .setItems(items.toArray(new String[0]), (d, which) -> {
                    if (index > 0 && which == 0) vm.setCover(index);
                    else vm.removePhoto(index);
                })
                .show();
    }

    // ---------- 标题 ----------

    private void setUpTitle() {
        vm.titleLength.observe(getViewLifecycleOwner(), n -> binding.tilTitle.setSuffixText(
                getString(R.string.add_product_count, n == null ? 0 : n, AddProductViewModel.TITLE_MAX)));
        vm.title.observe(getViewLifecycleOwner(), t -> vm.onTitleChanged());
        vm.titleError.observe(getViewLifecycleOwner(), err -> renderError(binding.tilTitle, err));
    }

    // ---------- 分类 ----------

    private void setUpCategory() {
        categoryItems.clear();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < CATEGORIES.length; i++) {
            for (String sub : getResources().getStringArray(SUB_ARRAYS[i])) {
                categoryItems.add(new String[]{CATEGORIES[i], sub});
                labels.add(getString(R.string.add_product_category_value, CATEGORIES[i], sub));
            }
        }
        binding.etCategory.setSimpleItems(labels.toArray(new String[0]));
        binding.etCategory.setOnItemClickListener((parent, v, position, id) -> {
            String[] item = categoryItems.get(position);
            vm.selectCategory(item[0], item[1]);
        });
        vm.category.observe(getViewLifecycleOwner(), c -> renderCategory());
        vm.subCategory.observe(getViewLifecycleOwner(), c -> renderCategory());
        vm.categoryError.observe(getViewLifecycleOwner(), err -> renderError(binding.tilCategory, err));
    }

    private void renderCategory() {
        String cat = vm.category.getValue();
        String sub = vm.subCategory.getValue();
        String text = cat == null ? "" : (sub == null ? cat : getString(R.string.add_product_category_value, cat, sub));
        if (!text.contentEquals(binding.etCategory.getText())) binding.etCategory.setText(text, false);
    }

    // ---------- 起售量 / 价格 / 单位 ----------

    private void setUpQuantityAndPrice() {
        String[] units = getResources().getStringArray(R.array.add_product_units);
        binding.etUnit.setSimpleItems(units);
        binding.etUnit.setOnItemClickListener((parent, v, position, id) -> vm.selectUnit(units[position]));
        vm.unit.observe(getViewLifecycleOwner(), u -> {
            String text = u == null ? "" : u;
            if (!text.contentEquals(binding.etUnit.getText())) binding.etUnit.setText(text, false);
            binding.tilMinQty.setSuffixText(text.isEmpty() ? null : text);
        });
        vm.price.observe(getViewLifecycleOwner(), p -> vm.onPriceChanged());
        // 「¥」与金额之间 8dp（原型）
        TextView prefix = binding.tilPrice.getPrefixTextView();
        prefix.setPaddingRelative(0, 0, Math.round(8 * getResources().getDisplayMetrics().density), 0);
        vm.priceError.observe(getViewLifecycleOwner(), err -> renderError(binding.tilPrice, err));
    }

    // ---------- 交易方式 ----------

    private void setUpTradeWays() {
        SpannableStringBuilder label = new SpannableStringBuilder(getString(R.string.add_product_trade));
        label.append(' ');
        int start = label.length();
        label.append(getString(R.string.add_product_trade_multi));
        label.setSpan(new TypefaceSpan("sans-serif"), start, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        label.setSpan(new ForegroundColorSpan(MarketStyles.color(binding.tvTrade,
                        com.google.android.material.R.attr.colorOnSurfaceVariant)),
                start, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.tvTrade.setText(label);

        tradeChips = new Chip[]{binding.chipPickup, binding.chipLocal, binding.chipExpress, binding.chipTalk};
        for (int i = 0; i < tradeChips.length; i++) {
            String way = AddProductViewModel.WAYS[i];
            tradeChips[i].setText(way);
            tradeChips[i].setOnCheckedChangeListener((c, on) -> vm.toggleTradeWay(way, on));
        }
        vm.tradeWays.observe(getViewLifecycleOwner(), this::renderTradeWays);
    }

    private void renderTradeWays(@Nullable Set<String> ways) {
        if (tradeChips == null) return;
        for (int i = 0; i < tradeChips.length; i++) {
            boolean on = ways != null && ways.contains(AddProductViewModel.WAYS[i]);
            if (tradeChips[i].isChecked() != on) tradeChips[i].setChecked(on);
        }
    }

    // ---------- 交易地点 ----------

    private void setUpLocation() {
        View.OnClickListener edit = v -> editLocation();
        binding.etLocation.setOnClickListener(edit);
        binding.tilLocation.setEndIconOnClickListener(edit);
    }

    private void editLocation() {
        View content = getLayoutInflater().inflate(R.layout.add_product_dialog_location, null, false);
        EditText input = content.findViewById(R.id.et_location_input);
        String current = vm.location.getValue();
        input.setText(current);
        if (current != null) input.setSelection(current.length());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_product_location_edit)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, w) -> vm.setLocation(input.getText().toString()))
                .show();
    }

    // ---------- 置顶 24 小时 ----------

    private void setUpPin() {
        binding.swPin.setOnCheckedChangeListener((b, on) -> {
            vm.setPin(on);
            renderSwitchIcon(binding.swPin, on);
        });
        vm.pin.observe(getViewLifecycleOwner(), on -> {
            boolean checked = Boolean.TRUE.equals(on);
            if (binding.swPin.isChecked() != checked) binding.swPin.setChecked(checked);
            renderSwitchIcon(binding.swPin, checked);
            renderPinText();
        });
        vm.pinEnabled.observe(getViewLifecycleOwner(), on -> {
            binding.swPin.setEnabled(Boolean.TRUE.equals(on));
            renderPinText();
        });
        vm.balance.observe(getViewLifecycleOwner(), b -> renderPinText());
    }

    private void renderPinText() {
        Integer b = vm.balance.getValue();
        int balance = b == null ? 0 : b;
        TextView tv = binding.tvPinDesc;
        if (!Boolean.TRUE.equals(vm.pinEnabled.getValue())) {
            tv.setText(getString(R.string.add_product_pin_insufficient, PointsRules.PIN_COST,
                    MarketFormat.points(balance)));
        } else if (Boolean.TRUE.equals(vm.pin.getValue())) {
            tv.setText(getString(R.string.add_product_pin_on, PointsRules.PIN_COST,
                    MarketFormat.points(vm.balanceAfterPin())));
        } else {
            tv.setText(getString(R.string.add_product_pin_off, PointsRules.PIN_COST, MarketFormat.points(balance)));
        }
    }

    // ---------- 附上种植记录 ----------

    private void setUpRecord() {
        binding.swRecord.setOnCheckedChangeListener((b, on) -> {
            vm.setAttachRecord(on);
            renderSwitchIcon(binding.swRecord, on);
        });
        vm.attachRecord.observe(getViewLifecycleOwner(), on -> {
            boolean checked = Boolean.TRUE.equals(on);
            if (binding.swRecord.isChecked() != checked) binding.swRecord.setChecked(checked);
            renderSwitchIcon(binding.swRecord, checked);
        });
        vm.crops.observe(getViewLifecycleOwner(), list -> renderRecord());
        vm.crop.observe(getViewLifecycleOwner(), c -> renderRecord());
        binding.recordText.setOnClickListener(v -> chooseCrop());
    }

    private void renderRecord() {
        List<CropCard> list = vm.crops.getValue();
        CropCard c = vm.crop.getValue();
        boolean has = list != null && !list.isEmpty() && c != null;
        binding.cardRecord.setVisibility(has ? View.VISIBLE : View.GONE);
        if (has) binding.tvRecordDesc.setText(getString(R.string.add_product_record_desc, cropLabel(c)));
        binding.recordText.setClickable(list != null && list.size() > 1);
    }

    /** 「草莓 · 2号大棚」 */
    private String cropLabel(CropCard c) {
        boolean hasPlot = c.plotName != null && !c.plotName.trim().isEmpty();
        return hasPlot ? getString(R.string.add_product_category_value, c.name, c.plotName.trim()) : c.name;
    }

    private void chooseCrop() {
        List<CropCard> list = vm.crops.getValue();
        if (list == null || list.size() < 2) return;
        String[] labels = new String[list.size()];
        int checked = -1;
        CropCard current = vm.crop.getValue();
        for (int i = 0; i < list.size(); i++) {
            labels[i] = cropLabel(list.get(i));
            if (current != null && current.id == list.get(i).id) checked = i;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_product_record_choose)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    vm.selectCrop(list.get(which));
                    d.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ---------- 通用 ----------

    /** 开关打开时滑块内显示勾（原型），关闭时不放图标 */
    private static void renderSwitchIcon(@NonNull MaterialSwitch sw, boolean checked) {
        sw.setThumbIconDrawable(checked ? ContextCompat.getDrawable(sw.getContext(), R.drawable.ic_check) : null);
    }

    private void renderError(@NonNull TextInputLayout til, @Nullable Integer err) {
        boolean on = err != null;
        til.setError(on ? getString(err) : null);
        til.setErrorEnabled(on);
    }
}
