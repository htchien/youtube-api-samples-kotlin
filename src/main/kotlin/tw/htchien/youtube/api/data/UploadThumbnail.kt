/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tw.htchien.youtube.api.data

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener
import com.google.api.client.http.InputStreamContent
import com.google.api.services.youtube.YouTube
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * This sample uses MediaHttpUploader to upload an image and then calls the
 * API's youtube.thumbnails.set method to set the image as the custom thumbnail
 * for a video.
 *
 * @author Ibrahim Ulukaya
 */
object UploadThumbnail {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /**
     * Define a global variable that specifies the MIME type of the image
     * being uploaded.
     */
    private val IMAGE_FILE_FORMAT = "image/png"

    /*
     * Prompts the user to enter a YouTube video ID and return the user input.
     */
    private// Exit if the user does not specify a video ID.
    val videoIdFromUser: String
        @Throws(IOException::class)
        get() {

            var inputVideoId = ""

            print("Please enter a video Id to update: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            inputVideoId = bReader.readLine()

            if (inputVideoId.length < 1) {
                print("Video Id can't be empty!")
                System.exit(1)
            }

            return inputVideoId
        }

    /*
     * Prompt the user to enter the path for the thumbnail image being uploaded.
     */
    private// Exit if the user does not provide a path to the image file.
    val imageFromUser: File
        @Throws(IOException::class)
        get() {

            var path = ""

            print("Please enter the path of the image file to upload: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            path = bReader.readLine()

            if (path.length < 1) {
                print("Path can not be empty!")
                System.exit(1)
            }

            return File(path)
        }

    /**
     * Prompt the user to specify a video ID and the path for a thumbnail
     * image. Then call the API to set the image as the thumbnail for the video.
     *
     * @param args command line args (not used).
     */
    @JvmStatic
    fun main(args: Array<String>) {

        // This OAuth 2.0 access scope allows for full read/write access to the
        // authenticated user's account.
        val scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube")

        try {
            // Authorize the request.
            val credential = Auth.authorize(scopes, "uploadthumbnail")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential).setApplicationName(
                    "youtube-cmdline-uploadthumbnail-sample").build()

            // Prompt the user to enter the video ID of the video being updated.
            val videoId = videoIdFromUser
            println("You chose $videoId to upload a thumbnail.")

            // Prompt the user to specify the location of the thumbnail image.
            val imageFile = imageFromUser
            println("You chose $imageFile to upload.")

            // Create an object that contains the thumbnail image file's
            // contents.
            val mediaContent = InputStreamContent(
                    IMAGE_FILE_FORMAT, BufferedInputStream(FileInputStream(imageFile)))
            mediaContent.length = imageFile.length()

            // Create an API request that specifies that the mediaContent
            // object is the thumbnail of the specified video.
            val thumbnailSet = youtube!!.thumbnails().set(videoId, mediaContent)

            // Set the upload type and add an event listener.
            val uploader = thumbnailSet.mediaHttpUploader

            // Indicate whether direct media upload is enabled. A value of
            // "True" indicates that direct media upload is enabled and that
            // the entire media content will be uploaded in a single request.
            // A value of "False," which is the default, indicates that the
            // request will use the resumable media upload protocol, which
            // supports the ability to resume an upload operation after a
            // network interruption or other transmission failure, saving
            // time and bandwidth in the event of network failures.
            uploader.isDirectUploadEnabled = false

            // Set the upload state for the thumbnail image.
            val progressListener = MediaHttpUploaderProgressListener { uploader ->
                when (uploader.uploadState) {
                    // This value is set before the initiation request is
                    // sent.
                    MediaHttpUploader.UploadState.INITIATION_STARTED -> println("Initiation Started")
                    // This value is set after the initiation request
                    //  completes.
                    MediaHttpUploader.UploadState.INITIATION_COMPLETE -> println("Initiation Completed")
                    // This value is set after a media file chunk is
                    // uploaded.
                    MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS -> {
                        println("Upload in progress")
                        println("Upload percentage: " + uploader.progress)
                    }
                    // This value is set after the entire media file has
                    //  been successfully uploaded.
                    MediaHttpUploader.UploadState.MEDIA_COMPLETE -> println("Upload Completed!")
                    // This value indicates that the upload process has
                    //  not started yet.
                    MediaHttpUploader.UploadState.NOT_STARTED -> println("Upload Not Started!")
                }
            }
            uploader.progressListener = progressListener

            // Upload the image and set it as the specified video's thumbnail.
            val setResponse = thumbnailSet.execute()

            // Print the URL for the updated video's thumbnail image.
            println("\n================== Uploaded Thumbnail ==================\n")
            println("  - Url: " + setResponse.items[0].default.url)

        } catch (e: GoogleJsonResponseException) {
            System.err.println("GoogleJsonResponseException code: " + e.details.code + " : "
                    + e.details.message)
            e.printStackTrace()

        } catch (e: IOException) {
            System.err.println("IOException: " + e.message)
            e.printStackTrace()
        }

    }
}
