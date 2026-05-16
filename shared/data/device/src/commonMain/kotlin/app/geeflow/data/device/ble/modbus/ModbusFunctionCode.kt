package app.geeflow.data.device.ble.modbus

object ModbusFunctionCode {
    const val READ_COILS: Byte = 0x01
    const val READ_DISCRETE_INPUTS: Byte = 0x02
    const val READ_HOLDING_REGISTERS: Byte = 0x03
    const val READ_INPUT_REGISTERS: Byte = 0x04
    const val WRITE_SINGLE_COIL: Byte = 0x05
    const val WRITE_SINGLE_REGISTER: Byte = 0x06
    const val WRITE_MULTIPLE_COILS: Byte = 0x0F
    const val WRITE_MULTIPLE_REGISTERS: Byte = 0x10

    /** Exception responses set the high bit on the function code (fc | 0x80). */
    const val EXCEPTION_MASK: Byte = 0x80.toByte()
}
