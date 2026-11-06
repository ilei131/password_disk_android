package com.passworddisk.app

import com.passworddisk.app.cloud.CloudApiService
import com.passworddisk.app.cloud.DeleteAccountRequest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicReference

/**
 * 守住 `DELETE /api/delete-account` 的请求契约。
 *
 * Retrofit 把 DELETE 当成「不能带请求体」的方法，写成 `@DELETE` + `@Body` 时编译期完全正常，
 * 只在第一次真正调用时抛 `IllegalArgumentException: Non-body HTTP method cannot contain @Body`。
 * 这类问题只能靠真的发一次请求才暴露，所以这里用裸 ServerSocket 起一个一次性 HTTP 服务，
 * 把请求行和请求体录下来逐项断言。
 *
 * 不用 MockWebServer 是因为项目没引入该依赖；也不用 `com.sun.net.httpserver`，
 * 因为 Android 单元测试编译时classpath 上是 android.jar，取不到这个 JDK 模块。
 */
class DeleteAccountContractTest {

    private lateinit var server: ServerSocket
    private lateinit var service: CloudApiService

    private val recordedMethod = AtomicReference<String>()
    private val recordedPath = AtomicReference<String>()
    private val recordedBody = AtomicReference<String>()
    private val serverFailure = AtomicReference<Throwable>()

    @Before
    fun setUp() {
        server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))

        Thread {
            try {
                server.accept().use { socket ->
                    val input = socket.getInputStream()
                    val raw = ByteArrayOutputStream()
                    val buffer = ByteArray(4096)

                    // 先读到请求头结束（空行），再把剩下的字节算作请求体。
                    // 注意不能判断「最后 4 个字节是 CRLFCRLF」——请求体常常和请求头一起到达，
                    // 那样会永远等不到结尾，客户端就在读响应时超时。
                    while (raw.indexOfHeaderEnd() < 0) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        raw.write(buffer, 0, read)
                    }
                    val headerEnd = raw.indexOfHeaderEnd()
                    val allBytes = raw.toByteArray()
                    val headerText = allBytes.copyOfRange(0, headerEnd.coerceAtLeast(0))
                        .toString(Charsets.UTF_8)
                    val requestLine = headerText.substringBefore("\r\n").split(" ")
                    recordedMethod.set(requestLine.getOrElse(0) { "" })
                    recordedPath.set(requestLine.getOrElse(1) { "" })

                    val contentLength = Regex("(?i)content-length:\\s*(\\d+)")
                        .find(headerText)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull()
                        ?: 0
                    val bodyBuffer = ByteArrayOutputStream()
                    if (headerEnd >= 0) {
                        bodyBuffer.write(allBytes, headerEnd + 4, allBytes.size - (headerEnd + 4))
                    }
                    while (bodyBuffer.size() < contentLength) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        bodyBuffer.write(buffer, 0, read)
                    }
                    recordedBody.set(bodyBuffer.toByteArray().toString(Charsets.UTF_8))

                    val payload = """{"success":true,"id":"u_1"}""".toByteArray()
                    val head = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: ${payload.size}\r\n" +
                        "Connection: close\r\n\r\n"
                    socket.getOutputStream().apply {
                        write(head.toByteArray())
                        write(payload)
                        flush()
                    }
                }
            } catch (t: Throwable) {
                serverFailure.set(t)
            }
        }.apply { isDaemon = true }.start()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://127.0.0.1:${server.localPort}/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        service = retrofit.create(CloudApiService::class.java)
    }

    @After
    fun tearDown() {
        server.close()
        serverFailure.get()?.let { throw AssertionError("本地测试服务出错", it) }
    }

    @Test
    fun deleteAccount_sendsCredentialsAsJsonBody() = runBlocking {
        val response = service.deleteAccount(DeleteAccountRequest("alice", "secret123"))

        assertTrue("服务端应返回 2xx", response.isSuccessful)
        assertEquals("请求方法必须是 DELETE", "DELETE", recordedMethod.get())
        assertEquals("请求路径应命中接口", "/api/delete-account", recordedPath.get())
        assertTrue(
            "用户名必须出现在请求体里：${recordedBody.get()}",
            recordedBody.get().contains("\"username\":\"alice\"")
        )
        assertTrue(
            "密码必须出现在请求体里：${recordedBody.get()}",
            recordedBody.get().contains("\"password\":\"secret123\"")
        )
    }

    /** 返回请求头结束（CRLFCRLF）的位置，找不到返回 -1 */
    private fun ByteArrayOutputStream.indexOfHeaderEnd(): Int {
        val bytes = toByteArray()
        for (i in 0..bytes.size - 4) {
            if (bytes[i] == '\r'.code.toByte() &&
                bytes[i + 1] == '\n'.code.toByte() &&
                bytes[i + 2] == '\r'.code.toByte() &&
                bytes[i + 3] == '\n'.code.toByte()
            ) {
                return i
            }
        }
        return -1
    }
}
