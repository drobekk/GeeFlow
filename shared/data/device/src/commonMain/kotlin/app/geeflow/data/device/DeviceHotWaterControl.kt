package app.geeflow.data.device

interface DeviceHotWaterControl {
    suspend fun start()
    suspend fun stop()

    data object Unsupported : DeviceHotWaterControl {
        override suspend fun start(): Unit = throw UnsupportedOperationException("Hot water is not supported")
        override suspend fun stop(): Unit = throw UnsupportedOperationException("Hot water is not supported")
    }
}
