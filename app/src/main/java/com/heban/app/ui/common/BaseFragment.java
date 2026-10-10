package com.heban.app.ui.common;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.heban.app.HeBanApp;
import com.heban.app.di.AppContainer;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

/** 页面基类：持有 CompositeDisposable（onDestroyView 时清除），提供 ViewModel 与导航的便捷方法。 */
public abstract class BaseFragment extends Fragment {
    protected final CompositeDisposable disposables = new CompositeDisposable();

    protected AppContainer container() {
        return HeBanApp.container(requireContext());
    }

    protected <T extends ViewModel> T viewModel(Class<T> modelClass) {
        return new ViewModelProvider(this, HeBanApp.factory(requireContext())).get(modelClass);
    }

    protected NavController nav() {
        return NavHostFragment.findNavController(this);
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        super.onDestroyView();
    }
}
