package com.djcrontab.code.controlsurface

import android.os.Looper.loop
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket


class BitwigConnection(
    val sendToBitwig: Channel<String>,
    val host: String,
    val port: Int
) {
    private var clientSocket: Socket? = null

    fun makeByteArray(s: String): ByteArray {
        val asByteArray = s.toByteArray()
        val header = asByteArray.let {
            val byteArray = ByteArray(4)
            var size = it.size
            for (i in 3 downTo 0) {
                byteArray[i] = (size and 0xff).toByte()
                size /= 256
            }
            byteArray
        }
        return header + asByteArray
    }

    private val readerChannel = Channel<String>()

    fun reader(): Flow<String> {
        return flow {
            for (line in readerChannel) {
                emit(line)
            }
        }
    }

    fun close() {
        if (clientSocket != null) {
            try {
                clientSocket!!.close()
            } catch (e: IOException) {

            }
        }
    }

    suspend fun start() {
        Log.v("ControlSurface", "connecting.. $host $port")
        val connected = MutableStateFlow(false)

        val writerThread = Thread {
            runBlocking {
                while (true) try {
                    clientSocket = Socket()
                    clientSocket!!.connect(InetSocketAddress(host, port))

                    connected.value = true
                    val outputStream = clientSocket!!.getOutputStream()

                    for (message in sendToBitwig) {
                        outputStream.write(makeByteArray(message))
                    }
                } catch (e: IOException) {
                    connected.value = false
                    close()
                    print("Not connected... retrying")
                    delay(1000)
                }
            }
        }

        val readerThread = Thread() {
            runBlocking {
                while(true) {
                    while (true) {
                        if (connected.first()) break
                    }
                    try {
                        val stream = clientSocket!!.getInputStream()
                        val reader = stream.bufferedReader()
                        while (true) {
                            val line = reader.readLine()
                            if (line != null) {
                                readerChannel.send(line)
                            } else {
                                break
                            }
                        }
                    } catch (_: IOException) {
                    }
                    close()
                }
            }
        }

        writerThread.start()
        readerThread.start()
    }
}