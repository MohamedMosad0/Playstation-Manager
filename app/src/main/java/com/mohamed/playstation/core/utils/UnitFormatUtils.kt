package com.mohamed.playstation.core.utils

import androidx.annotation.StringRes
import com.mohamed.playstation.R

enum class UnitType(
    val rawDbValue: String,
    @param:StringRes val singularRes: Int,
    @param:StringRes val pluralRes: Int,
    @param:StringRes val defPluralRes: Int,
    @param:StringRes val defSingularRes: Int
) {
    CUP("كوب", R.string.unit_cup, R.string.unit_cup_plural, R.string.unit_cup_def_plural, R.string.unit_cup_def_sing),
    BOX("علبة", R.string.unit_box, R.string.unit_box_plural, R.string.unit_box_def_plural, R.string.unit_box_def_sing),
    BOTTLE("زجاجة", R.string.unit_bottle, R.string.unit_bottle_plural, R.string.unit_bottle_def_plural, R.string.unit_bottle_def_sing),
    BAG("كيس", R.string.unit_bag, R.string.unit_bag_plural, R.string.unit_bag_def_plural, R.string.unit_bag_def_sing),
    PIECE("قطعة", R.string.unit_piece, R.string.unit_piece_plural, R.string.unit_piece_def_plural, R.string.unit_piece_def_sing),
    PLATE("طبق", R.string.unit_plate, R.string.unit_plate_plural, R.string.unit_plate_def_plural, R.string.unit_plate_def_sing),
    UNIT("وحدة", R.string.unit_unit, R.string.unit_unit_plural, R.string.unit_unit_def_plural, R.string.unit_unit_def_sing),
    PACK("عبوة", R.string.unit_pack, R.string.unit_pack_plural, R.string.unit_pack_def_plural, R.string.unit_pack_def_sing),
    CARTON("كرتونة", R.string.unit_carton, R.string.unit_carton_plural, R.string.unit_carton_def_plural, R.string.unit_carton_def_sing),
    TRAY("صينية", R.string.unit_tray, R.string.unit_tray_plural, R.string.unit_tray_def_plural, R.string.unit_tray_def_sing),
    BUNDLE("ربطة", R.string.unit_bundle, R.string.unit_bundle_plural, R.string.unit_bundle_def_plural, R.string.unit_bundle_def_sing);

    companion object {
        fun fromRaw(raw: String): UnitType? {
            return values().find { it.rawDbValue == raw }
        }
    }
}

object UnitFormatUtils {
    @StringRes
    fun getPluralUnitRes(unitLabel: String): Int {
        return UnitType.fromRaw(unitLabel)?.pluralRes ?: R.string.unit_unit_plural
    }

    @StringRes
    fun getDefinitePluralUnitRes(unitLabel: String): Int {
        return UnitType.fromRaw(unitLabel)?.defPluralRes ?: R.string.unit_unit_def_plural
    }

    @StringRes
    fun getDefiniteSingularUnitRes(unitLabel: String): Int {
        return UnitType.fromRaw(unitLabel)?.defSingularRes ?: R.string.unit_unit_def_sing
    }

    fun getLocalizedName(context: android.content.Context, rawDbValue: String): String {
        val resId = UnitType.fromRaw(rawDbValue)?.singularRes
        return if (resId != null) context.getString(resId) else rawDbValue
    }
}
