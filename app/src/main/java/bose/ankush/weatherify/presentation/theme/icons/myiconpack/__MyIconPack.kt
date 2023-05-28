package bose.ankush.weatherify.presentation.theme.icons.myiconpack

import androidx.compose.ui.graphics.vector.ImageVector
import bose.ankush.weatherify.presentation.theme.icons.myiconpack.myiconpack.Account
import bose.ankush.weatherify.presentation.theme.icons.myiconpack.myiconpack.Home
import bose.ankush.weatherify.presentation.theme.icons.myiconpack.myiconpack.Radar
import kotlin.collections.List as ____KtList

object MyIconPack

private var __AllIcons: ____KtList<ImageVector>? = null

val MyIconPack.AllIcons: ____KtList<ImageVector>
  get() {
    if (__AllIcons != null) {
      return __AllIcons!!
    }
    __AllIcons= listOf(Account, Home, Radar)
    return __AllIcons!!
  }
