package com.example.tagmoa.view

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes

data class CurvedBottomNavItem(
    @IdRes val id: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val contentDescription: Int? = null
)
