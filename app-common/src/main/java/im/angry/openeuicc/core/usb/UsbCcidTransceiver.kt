package im.angry.openeuicc.core.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.os.SystemClock
import android.util.Log
import im.angry.openeuicc.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder


/**
 * Provides raw, APDU-agnostic transmission to the CCID reader
 * Adapted from <https://github.com/open-keychain/open-keychain/blob/master/OpenKeychain/src/main/java/org/sufficientlysecure/keychain/securitytoken/usb/CcidTransceiver.java>
 */
@Suppress("unused")
class UsbCcidTransceiver(
    private val usbConnection: UsbDeviceConnection,
    private val usbBulkIn: UsbEndpoint,
    private val usbBulkOut: UsbEndpoint,
    private val usbCcidDescription: UsbCcidDescription
) {
    companion object {
        private const val TAG = "UsbCcidTransceiver"

        private const val CCID_HEADER_LENGTH = 10

        private const val MESSAGE_TYPE_RDR_TO_PC_DATA_BLOCK = 0x80
        private const val MESSAGE_TYPE_PC_TO_RDR_ICC_POWER_ON = 0x62
        private const val MESSAGE_TYPE_PC_TO_RDR_ICC_POWER_OFF = 0x63
        private const val MESSAGE_TYPE_PC_TO_RDR_XFR_BLOCK = 0x6f

        private const val COMMAND_STATUS_SUCCESS: Byte = 0
        private const val COMMAND_STATUS_TIME_EXTENSION_RQUESTED: Byte = 2

        /**
         * Level Parameter: APDU is a single command.
         *
         * "the command APDU begins and ends with this command"
         * -- DWG Smart-Card USB Integrated Circuit(s) Card Devices rev 1.0
         * § 6.1.1.3
         */
        const val LEVEL_PARAM_START_SINGLE_CMD_APDU: Short = 0x0000

        /**
         * Level Parameter: First APDU in a multi-command APDU.
         *
         * "the command APDU begins with this command, and continue in the
         * next PC_to_RDR_XfrBlock"
         * -- DWG Smart-Card USB Integrated Circuit(s) Card Devices rev 1.0
         * § 6.1.1.3
         */
        const val LEVEL_PARAM_START_MULTI_CMD_APDU: Short = 0x0001

        /**
         * Level Parameter: Final APDU in a multi-command APDU.
         *
         * "this abData field continues a command APDU and ends the command APDU"
         * -- DWG Smart-Card USB Integrated Circuit(s) Card Devices rev 1.0
         * § 6.1.1.3
         */
        const val LEVEL_PARAM_END_MULTI_CMD_APDU: Short = 0x0002

        /**
         * Level Parameter: Next command in a multi-command APDU.
         *
         * "the abData field continues a command APDU and another block is to follow"
         * -- DWG Smart-Card USB Integrated Circuit(s) Card Devices rev 1.0
         * § 6.1.1.3
         */
        const val LEVEL_PARAM_CONTINUE_MULTI_CMD_APDU: Short = 0x0003

        /**
         * Level Parameter: Request the device continue sending APDU.
         *
         * "empty abData field, continuation of response APDU is expected in the next
         * RDR_to_PC_DataBlock"
         * -- DWG Smart-Card USB Integrated Circuit(s) Card Devices rev 1.0
         * § 6.1.1.3
         */
        const val LEVEL_PARAM_CONTINUE_RESPONSE: Short = 0x0010

        private const val SLOT_NUMBER = 0x00

        private const val ICC_STATUS_SUCCESS: Byte = 0

        private const val DEVICE_COMMUNICATE_TIMEOUT_MILLIS = 5000
        private const val DEVICE_SKIP_TIMEOUT_MILLIS = 100
    }

    data class UsbCcidErrorException(val msg: String, val errorResponse: CcidDataBlock) :
        Exception(msg)

    data class CcidDataBlock(
        val dwLength: Int,
        val bSlot: Byte,
        val bSeq: Byte,
        val bStatus: Byte,
        val bError: Byte,
        val bChainParameter: Byte,
        val data: ByteArray?
    ) {
        companion object {
            fun parseHeaderFromBytes(headerBytes: ByteArray): CcidDataBlock {
                val buf = ByteBuffer.wrap(headerBytes)
                buf.order(ByteOrder.LITTLE_ENDIAN)

                val type = buf.get()
                require(type == MESSAGE_TYPE_RDR_TO_PC_DATA_BLOCK.toByte()) { "Header has incorrect type value!" }
                val dwLength = buf.int
                val bSlot = buf.get()
                val bSeq = buf.get()
                val bStatus = buf.get()
                val bError = buf.get()
                val bChainParameter = buf.get()

                return CcidDataBlock(dwLength, bSlot, bSeq, bStatus, bError, bChainParameter, null)
            }
        }

        fun withData(d: ByteArray): CcidDataBlock {
            require(data == null) { "Cannot add data twice" }
            return CcidDataBlock(dwLength, bSlot, bSeq, bStatus, bError, bChainParameter, d)
        }

        val iccStatus: Byte
            get() = (bStatus.toInt() and 0x03).toByte()

        val commandStatus: Byte
            get() = ((bStatus.toInt() shr 6) and 0x03).toByte()

        val isStatusTimeoutExtensionRequest: Boolean
            get() = commandStatus == COMMAND_STATUS_TIME_EXTENSION_RQUESTED

        val isStatusSuccess: Boolean
            get() = iccStatus == ICC_STATUS_SUCCESS && commandStatus == COMMAND_STATUS_SUCCESS
    }

    val hasAutomaticPps = usbCcidDescription.hasAutomaticPps

    private val inputBuffer = ByteArray(usbBulkIn.maxPacketSize)

    private var currentSequenceNumber: Byte = 0

    private fun sendRaw(data: ByteArray, offset: Int, length: Int) {
        val tr1 = usbConnection.bulkTransfer(
            usbBulkOut, data, offset, length, DEVICE_COMMUNICATE_TIMEOUT_MILLIS
        )
        if (tr1 != length) {
            throw UsbTransportException(
                "USB error - failed to transmit data ($tr1/$length)"
            )
        }
    }

    private fun receiveDataBlock(expectedSequenceNumber: Byte): CcidDataBlock {
        var response: CcidDataBlock?
        do {
            response = receiveDataBlockImmediate(expectedSequenceNumber)
        } while (response!!.isStatusTimeoutExtensionRequest)
        if (!response.isStatusSuccess) {
            throw UsbCcidErrorException("USB-CCID error!", response)
        }
        return response
    }

    private fun receiveDataBlockImmediate(expectedSequenceNumber: Byte): CcidDataBlock {
        /*
         * Some USB CCID devices (notably NitroKey 3) may time-out and need a subsequent poke to
         * carry on communications.  No particular reason why the number 3 was chosen.  If we get a
         * zero-sized reply (or a time-out), we try again.  Clamped retries prevent an infinite loop
         * if things really turn sour.
         */
        var attempts = 3
        Log.d(TAG, "Receive data block immediate seq=$expectedSequenceNumber")
        var readBytes: Int
        do {
            readBytes = usbConnection.bulkTransfer(
                usbBulkIn, inputBuffer, inputBuffer.size, DEVICE_COMMUNICATE_TIMEOUT_MILLIS
            )
            Log.d(TAG, "Received " + readBytes + " bytes: " + inputBuffer.encodeHex())
        } while (readBytes <= 0 && attempts-- > 0)
        if (readBytes < CCID_HEADER_LENGTH) {
            throw UsbTransportException("USB-CCID error - failed to receive CCID header")
        }
        if (inputBuffer[0] != MESSAGE_TYPE_RDR_TO_PC_DATA_BLOCK.toByte()) {
            if (expectedSequenceNumber != inputBuffer[6]) {
                throw UsbTransportException(
                    ((("USB-CCID error - bad CCID header, type " + inputBuffer[0]) + " (expected " +
                            MESSAGE_TYPE_RDR_TO_PC_DATA_BLOCK) + "), sequence number " + inputBuffer[6]
                            ) + " (expected " +
                            expectedSequenceNumber + ")"
                )
            }
            throw UsbTransportException(
                "USB-CCID error - bad CCID header type " + inputBuffer[0]
            )
        }
        var result = CcidDataBlock.parseHeaderFromBytes(inputBuffer)
        if (expectedSequenceNumber != result.bSeq) {
            throw UsbTransportException(
                ("USB-CCID error - expected sequence number " +
                        expectedSequenceNumber + ", got " + result)
            )
        }

        val dataBuffer = ByteArray(result.dwLength)
        var bufferedBytes = readBytes - CCID_HEADER_LENGTH
        System.arraycopy(inputBuffer, CCID_HEADER_LENGTH, dataBuffer, 0, bufferedBytes)
        while (bufferedBytes < dataBuffer.size) {
            readBytes = usbConnection.bulkTransfer(
                usbBulkIn, inputBuffer, inputBuffer.size, DEVICE_COMMUNICATE_TIMEOUT_MILLIS
            )
            if (readBytes < 0) {
                throw UsbTransportException(
                    "USB error - failed reading response data! Header: $result"
                )
            }
            System.arraycopy(inputBuffer, 0, dataBuffer, bufferedBytes, readBytes)
            bufferedBytes += readBytes
        }
        result = result.withData(dataBuffer)
        return result
    }


    private fun skipAvailableInput() {
        var ignoredBytes: Int
        do {
            ignoredBytes = usbConnection.bulkTransfer(
                usbBulkIn, inputBuffer, inputBuffer.size, DEVICE_SKIP_TIMEOUT_MILLIS
            )
            if (ignoredBytes > 0) {
                Log.e(TAG, "Skipped $ignoredBytes bytes")
            }
        } while (ignoredBytes > 0)
    }

    /**
     * Receives a continued XfrBlock. Should be called when a multiblock response is indicated
     * 6.1.4 PC_to_RDR_XfrBlock
     */
    fun receiveContinuedResponse(): CcidDataBlock {
        return sendXfrBlock(ByteArray(0), LEVEL_PARAM_CONTINUE_RESPONSE)
    }

    /**
     * Transmits XfrBlock
     * 6.1.4 PC_to_RDR_XfrBlock
     *
     * @param payload payload to transmit
     * @param levelParam Level parameter
     */
    fun sendXfrBlock(
        payload: ByteArray,
        levelParam: Short = LEVEL_PARAM_START_SINGLE_CMD_APDU
    ): CcidDataBlock {
        val startTime = SystemClock.elapsedRealtime()
        val l = payload.size
        val sequenceNumber: Byte = currentSequenceNumber++
        val headerData = byteArrayOf(
            MESSAGE_TYPE_PC_TO_RDR_XFR_BLOCK.toByte(),
            l.toByte(),
            (l shr 8).toByte(),
            (l shr 16).toByte(),
            (l shr 24).toByte(),
            SLOT_NUMBER.toByte(),
            sequenceNumber,
            0x00.toByte(),
            (levelParam.toInt() and 0x00ff).toByte(),
            (levelParam.toInt() shr 8).toByte()
        )
        val data: ByteArray = headerData + payload
        var sentBytes = 0
        while (sentBytes < data.size) {
            val bytesToSend = usbBulkOut.maxPacketSize.coerceAtMost(data.size - sentBytes)
            sendRaw(data, sentBytes, bytesToSend)
            sentBytes += bytesToSend
        }
        val ccidDataBlock = receiveDataBlock(sequenceNumber)
        val elapsedTime = SystemClock.elapsedRealtime() - startTime
        Log.d(TAG, "USB XferBlock call took " + elapsedTime + "ms")
        return ccidDataBlock
    }

    fun iccPowerOn(): CcidDataBlock {
        val startTime = SystemClock.elapsedRealtime()
        skipAvailableInput()
        var response: CcidDataBlock? = null
        for (v in usbCcidDescription.voltages) {
            Log.v(TAG, "CCID: attempting to power on with voltage $v")
            response = try {
                iccPowerOnVoltage(v.powerOnValue)
            } catch (e: UsbCcidErrorException) {
                if (e.errorResponse.bError.toInt() == 7) { // Power select error
                    Log.v(TAG, "CCID: failed to power on with voltage $v")
                    iccPowerOff()
                    Log.v(TAG, "CCID: powered off")
                    continue
                }
                throw e
            }
            break
        }
        if (response == null) {
            throw UsbTransportException("Couldn't power up ICC2")
        }
        val elapsedTime = SystemClock.elapsedRealtime() - startTime
        Log.d(
            TAG,
            "Usb transport connected, took " + elapsedTime + "ms, ATR=" +
                    response.data?.encodeHex()
        )
        return response
    }

    private fun iccPowerOnVoltage(voltage: Byte): CcidDataBlock {
        val sequenceNumber = currentSequenceNumber++
        val iccPowerCommand = byteArrayOf(
            MESSAGE_TYPE_PC_TO_RDR_ICC_POWER_ON.toByte(),
            0x00, 0x00, 0x00, 0x00,
            SLOT_NUMBER.toByte(),
            sequenceNumber,
            voltage,
            0x00, 0x00 // reserved for future use
        )
        sendRaw(iccPowerCommand, 0, iccPowerCommand.size)
        return receiveDataBlock(sequenceNumber)
    }

    private fun iccPowerOff() {
        val sequenceNumber = currentSequenceNumber++
        val iccPowerCommand = byteArrayOf(
            MESSAGE_TYPE_PC_TO_RDR_ICC_POWER_OFF.toByte(),
            0x00, 0x00, 0x00, 0x00,
            0x00,
            sequenceNumber,
            0x00
        )
        sendRaw(iccPowerCommand, 0, iccPowerCommand.size)
    }
}