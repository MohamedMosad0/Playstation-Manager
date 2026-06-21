package com.mohamed.playstation.core.utils

object UnitFormatUtils {
    fun getPluralUnit(unitLabel: String): String {
        return when (unitLabel) {
            "كوب" -> "أكواب"
            "علبة" -> "علب"
            "زجاجة" -> "زجاجات"
            "كيس" -> "أكياس"
            "قطعة" -> "قطع"
            "طبق" -> "أطباق"
            "وحدة" -> "وحدات"
            "عبوة" -> "عبوات"
            "كرتونة" -> "كراتين"
            "صينية" -> "صواني"
            "ربطة" -> "ربطات"
            else -> unitLabel
        }
    }

    fun getDefinitePluralUnit(unitLabel: String): String {
        return when (unitLabel) {
            "كوب" -> "الأكواب"
            "علبة" -> "العلب"
            "زجاجة" -> "الزجاجات"
            "كيس" -> "الأكياس"
            "قطعة" -> "القطع"
            "طبق" -> "الأطباق"
            "وحدة" -> "الوحدات"
            "عبوة" -> "العبوات"
            "كرتونة" -> "الكراتين"
            "صينية" -> "الصواني"
            "ربطة" -> "الربطات"
            else -> "ال$unitLabel" // Simple fallback
        }
    }

    fun getDefiniteSingularUnit(unitLabel: String): String {
        return when (unitLabel) {
            "كوب" -> "الكوب"
            "علبة" -> "العلبة"
            "زجاجة" -> "الزجاجة"
            "كيس" -> "الكيس"
            "قطعة" -> "القطعة"
            "طبق" -> "الطبق"
            "وحدة" -> "الوحدة"
            "عبوة" -> "العبوة"
            "كرتونة" -> "الكرتونة"
            "صينية" -> "الصينية"
            "ربطة" -> "الربطة"
            else -> "ال$unitLabel"
        }
    }
}
