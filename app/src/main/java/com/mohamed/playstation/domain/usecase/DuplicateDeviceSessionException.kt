package com.mohamed.playstation.domain.usecase

class DuplicateDeviceSessionException(
    val deviceType: String,
    val deviceNumber: Int
) : IllegalStateException("Duplicate active or paused session for $deviceType #$deviceNumber") {

    val deviceLabel: String = "$deviceType #$deviceNumber"
}
