# Task 16: Tree Editing Feature

## Summary
Add a feature allowing users to edit their trees, accessible via Settings.

## Flow
Settings -> User Select -> Tree List -> Tree Detail (edit note / delete)

## Files Created
- `treeedit/TreeEditUserSelectScreen.kt`
- `treeedit/TreeListScreen.kt`
- `treeedit/TreeListViewModel.kt`
- `treeedit/TreeDetailScreen.kt`
- `treeedit/TreeDetailViewModel.kt`

## Files Modified
- `database/TreeTrackerDAO.kt` - added `getTreesByUserWallet`, `deleteTreeById`
- `navigation/Routes.kt` - added `TreeEditUserSelectRoute`, `TreeListRoute`, `TreeDetailRoute`
- `settings/SettingsViewModel.kt` - added `NavigateToEditTrees` action
- `settings/SettingsScreen.kt` - added Edit Trees menu item and navigation
- `root/Host.kt` - registered 3 new routes
- `res/values/strings.xml` - added tree editing string resources
