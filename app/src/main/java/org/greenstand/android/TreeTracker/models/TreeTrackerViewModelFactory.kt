package org.greenstand.android.TreeTracker.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.greenstand.android.TreeTracker.capture.TreeImageReviewViewModel
import org.greenstand.android.TreeTracker.dashboard.DashboardViewModel
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListViewModel
import org.greenstand.android.TreeTracker.orgpicker.AddOrgViewModel
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerViewModel
import org.greenstand.android.TreeTracker.permissions.PermissionViewModel
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteViewModel
import org.greenstand.android.TreeTracker.signup.SignupViewModel
import org.greenstand.android.TreeTracker.splash.SplashScreenViewModel
import org.greenstand.android.TreeTracker.treeheight.TreeHeightSelectionViewModel
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.walletselect.WalletSelectViewModel
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Suppress("UNCHECKED_CAST")
class TreeTrackerViewModelFactory : ViewModelProvider.NewInstanceFactory(), KoinComponent {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(UserSelectViewModel::class.java) -> get<UserSelectViewModel>() as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> get<DashboardViewModel>() as T
            modelClass.isAssignableFrom(SignupViewModel::class.java) -> get<SignupViewModel>() as T
            modelClass.isAssignableFrom(LanguagePickerViewModel::class.java) -> get<LanguagePickerViewModel>() as T
            modelClass.isAssignableFrom(OrgPickerViewModel::class.java) -> get<OrgPickerViewModel>() as T
            modelClass.isAssignableFrom(WalletSelectViewModel::class.java) -> get<WalletSelectViewModel>() as T
            modelClass.isAssignableFrom(AddWalletViewModel::class.java) -> get<AddWalletViewModel>() as T
            modelClass.isAssignableFrom(SplashScreenViewModel::class.java) -> get<SplashScreenViewModel>() as T
            modelClass.isAssignableFrom(TreeImageReviewViewModel::class.java) -> get<TreeImageReviewViewModel>() as T
            modelClass.isAssignableFrom(PermissionViewModel::class.java) -> get<PermissionViewModel>() as T
            modelClass.isAssignableFrom(IndividualMessageListViewModel::class.java) -> get<IndividualMessageListViewModel>() as T
            modelClass.isAssignableFrom(TreeHeightSelectionViewModel::class.java) -> get<TreeHeightSelectionViewModel>() as T
            modelClass.isAssignableFrom(AddOrgViewModel::class.java) -> get<AddOrgViewModel>() as T
            modelClass.isAssignableFrom(SessionNoteViewModel::class.java) -> get<SessionNoteViewModel>() as T
            else -> throw RuntimeException("Unable to create instance of ${modelClass.simpleName}. Did you forget to update the TreeTrackerViewModelFactory?")
        }
    }
}
