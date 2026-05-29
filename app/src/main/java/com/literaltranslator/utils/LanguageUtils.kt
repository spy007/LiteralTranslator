package com.literaltranslator.utils

import java.util.Locale

fun getLanguageName(code: String): String {
    val locale = Locale(code)
    // Получаем имя языка на текущем языке системы телефона
    return locale.getDisplayName(Locale.getDefault())
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
