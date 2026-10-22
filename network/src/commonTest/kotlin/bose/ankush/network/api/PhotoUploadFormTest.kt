package bose.ankush.network.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.test.runTest
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertTrue

class PhotoUploadFormTest {
    @Test
    fun theUploadIsOneMultipartFilePartTheServerAccepts() =
        runTest {
            var contentType = ""
            var body = ""
            val client =
                HttpClient(
                    MockEngine { request ->
                        contentType = request.body.contentType.toString()
                        val content = request.body as OutgoingContent.WriteChannelContent
                        val channel =
                            io.ktor.utils.io
                                .ByteChannel()
                        content.writeTo(channel)
                        channel.flushAndClose()
                        body = channel.readBuffer().readByteArray().decodeToString()
                        respondOk()
                    },
                )

            client.submitFormWithBinaryData(
                url = "https://example.test/account/photo",
                formData =
                    photoUploadForm(
                        byteArrayOf(0xFF.toByte(), 0xD8.toByte()),
                        "image/jpeg",
                        "profile.jpg",
                    ),
            )

            assertTrue(contentType.startsWith("multipart/form-data"), contentType)
            assertTrue("name=file" in body || "name=\"file\"" in body, body)
            assertTrue("Content-Type: image/jpeg" in body, body)
            assertTrue("filename=\"profile.jpg\"" in body, body)
        }
}
