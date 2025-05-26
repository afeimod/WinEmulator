package org.github.ewt45.winemulator.ui

import kotlin.reflect.KClass

enum class Destination(
    val title: String,
    val route: Any,
    val baseRoute: Any = route,
) {
    Terminal(
        "终端",
        RouteTerminal,
    ),
    X11(
        "x11",
        RouteX11,
    ),
    Settings(
        "设置",
        RouteSettings,
    )
}

/** 显示在appbar中的tab */
val appbarDestList = listOf(Destination.Terminal, Destination.Settings)