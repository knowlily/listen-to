package com.knowlily.browser.model

data class TabItem(
    val id: Int,
    val title: String = "新标签页",
    val url: String = "",
    val isIncognito: Boolean = false
)
