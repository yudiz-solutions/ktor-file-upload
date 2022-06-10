package me.yudiz.application

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.coobird.thumbnailator.Thumbnails
import java.io.File
import java.util.*

private val ACCEPTED_FILE_EXTENSIONS = listOf("PNG", "JPG", "JPEG")

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        routing {
            /**
             * Upload File as Byte Array
             */
            post("/upload/binary") {
                try {
                    val fileBytes = call.receive<ByteArray>()
                    if(fileBytes.isEmpty()){
                        call.respond(HttpStatusCode.BadRequest, "Invalid File ❌")
                        return@post
                    }
                    File("uploads/BinaryFile.png").writeBytes(fileBytes)
                    call.respond(HttpStatusCode.OK,"File Uploaded ✅")
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError,e.message ?: "Something went wrong")
                }
            }

            /**
             * Upload multipart file in local folder
             */
            post("/upload/file") {
                var hasFile = false
                try {
                    val multipartData = call.receiveMultipart()
                    multipartData.forEachPart { part ->
                        when (part) {
                            /** Form Data - Key and Values */
                            is PartData.FormItem -> {
                                println("key ${part.name}, value ${part.value}")
                            }
                            /** Save in Uploads folder */
                            is PartData.FileItem -> {
                                val fileName = part.originalFileName
                                File("uploads/${fileName}").writeBytes(part.streamProvider().readBytes())
                                hasFile = true
                            }
                            else -> {
                                println("Unknown request field")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError,e.message ?: "Something went wrong")
                }
                if(hasFile)
                    call.respondText("Upload Success ✅")
                else
                    call.respond(HttpStatusCode.BadRequest,"Missing file in request")
            }

            /**
             * Upload file with random file name
             */
            post("/upload/file/random_name") {
                var hasFile = false
                try {
                    val multipartData = call.receiveMultipart()
                    multipartData.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                println("key ${part.name}, value ${part.value}")
                            }
                            is PartData.FileItem -> {
                                val fileName = UUID.randomUUID()
                                File("uploads/${fileName}.png").writeBytes(part.streamProvider().readBytes())
                                hasFile = true
                            }
                            else -> {
                                println("Unknown request field")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError,e.message ?: "Something went wrong")
                }
                if(hasFile)
                    call.respondText("Upload Success ✅")
                else
                    call.respond(HttpStatusCode.BadRequest,"Missing file in request")
            }

            /**
             * Upload file temporarily
             */
            post("/upload/file/temp") {
                var hasFile = false
                try {
                    val multipartData = call.receiveMultipart()
                    multipartData.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                println("key ${part.name}, value ${part.value}")
                            }
                            is PartData.FileItem -> {
                                withContext(Dispatchers.IO) {
                                    val file = File.createTempFile("img", ".png", null)
                                    file
                                }.writeBytes(part.streamProvider().readBytes())
                                hasFile = true
                            }
                            else -> {
                                println("Unknown request field")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError,e.message ?: "Something went wrong")
                }
                if(hasFile)
                    call.respondText("Upload Success ✅")
                else
                    call.respond(HttpStatusCode.BadRequest,"Missing file in request")
            }

            /**
             * Upload Image and Compress
             */
            post("/upload/image") {
                var hasFile = false
                try {
                    val multipartData = call.receiveMultipart()
                    multipartData.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                println("key ${part.name}, value ${part.value}")
                            }
                            is PartData.FileItem -> {
                                val fileName = UUID.randomUUID()
                                val mFile = File("uploads/${fileName}.png");
                                mFile.writeBytes(part.streamProvider().readBytes())
                                Thumbnails.of(mFile).size(200, 200).outputQuality(0.5).toFile("uploads/${mFile.nameWithoutExtension}_thumbnail.png")
                                hasFile = true
                            }
                            else -> {
                                println("Unknown request field")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError,e.message ?: "Something went wrong")
                }
                if(hasFile)
                    call.respondText("Upload Success ✅")
                else
                    call.respond(HttpStatusCode.BadRequest,"Missing file in request")
            }

            /**
             * Upload Image and Validate
             */
            post("/upload/file/validation") {
                var hasFile = false
                try {
                    val multipartData = call.receiveMultipart()
                    multipartData.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                println("key ${part.name}, value ${part.value}")
                            }
                            is PartData.FileItem -> {
                                val contentLengthBytes = call.request.header(HttpHeaders.ContentLength)?.toDouble() ?: 0.0
                                val contentLengthMB = contentLengthBytes / 1024 / 1024
                                if(contentLengthMB > 1.0){
                                    call.respond(HttpStatusCode.BadRequest, "File is too large, maximum allowed size is 1 MB ❌")
                                }
                                val fileExtension = part.contentType?.contentSubtype
                                if (fileExtension?.uppercase() !in ACCEPTED_FILE_EXTENSIONS) {
                                    call.respond(HttpStatusCode.BadRequest, "Invalid file type ❌")
                                }
                                hasFile = true
                                call.respond("Valid File ✅")
                            }
                            else -> {
                                println("Unknown request field")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError,e.message ?: "Something went wrong")
                }
                if(hasFile)
                    call.respondText("Upload Success ✅")
                else
                    call.respond(HttpStatusCode.BadRequest,"Missing file in request")
            }
        }
    }.start(wait = true)
}