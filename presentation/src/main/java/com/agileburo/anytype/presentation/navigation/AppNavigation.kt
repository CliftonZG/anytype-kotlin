package com.agileburo.anytype.presentation.navigation

interface AppNavigation {

    fun startLogin()
    fun createProfile()
    fun enterKeychain()
    fun choosePinCode()
    fun confirmPinCode(pin: String)
    fun setupNewAccount()
    fun setupSelectedAccount(id: String)
    fun congratulation()
    fun chooseAccount()
    fun workspace()
    fun openProfile()
    fun openDocument(id: String)
    fun startDesktopFromSplash()
    fun startDesktopFromLogin()
    fun startSplashFromDesktop()
    fun openKeychainScreen()
    fun openContacts()
    fun openDatabaseViewAddView()
    fun openEditDatabase()
    fun openSwitchDisplayView()
    fun openCustomizeDisplayView()
    fun openKanban()
    fun openGoals()
    fun exit()

    sealed class Command {

        object Exit : Command()

        object OpenStartLoginScreen : Command()
        object OpenCreateAccount : Command()
        object ChoosePinCodeScreen : Command()
        object SetupNewAccountScreen : Command()
        data class SetupSelectedAccountScreen(val id: String) : Command()
        data class ConfirmPinCodeScreen(val code: String) : Command()
        object CongratulationScreen : Command()
        object SelectAccountScreen : Command()
        object EnterKeyChainScreen : Command()
        object WorkspaceScreen : Command()
        data class OpenPage(val id: String) : Command()
        object OpenProfile : Command()
        object OpenKeychainScreen : Command()
        object OpenPinCodeScreen : Command()
        object StartDesktopFromSplash : Command()
        object StartDesktopFromLogin : Command()
        object StartSplashFromDesktop : Command()
        object OpenContactsScreen : Command()
        object OpenDatabaseViewAddView: Command()
        object OpenEditDatabase: Command()
        object OpenSwitchDisplayView: Command()
        object OpenCustomizeDisplayView: Command()
        object OpenKanbanScreen: Command()
        object OpenGoalsScreen : Command()
    }

    interface Provider {
        fun nav(): AppNavigation
    }
}