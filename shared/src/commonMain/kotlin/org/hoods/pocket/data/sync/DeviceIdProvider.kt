package org.hoods.pocket.data.sync

interface DeviceIdProvider {
    fun getDeviceId(): String
}

class FixedDeviceIdProvider(private val deviceId: String) : DeviceIdProvider {
    override fun getDeviceId(): String = deviceId
}
