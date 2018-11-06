/*
 * Copyright (c) 2015 Google Inc.
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
import com.google.api.client.googleapis.media.MediaHttpDownloader
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener
import com.google.api.client.http.InputStreamContent
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Caption
import com.google.api.services.youtube.model.CaptionSnippet
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * This sample creates and manages caption tracks by:
 *
 * 1. Uploading a caption track for a video via "captions.insert" method.
 * 2. Getting the caption tracks for a video via "captions.list" method.
 * 3. Updating an existing caption track via "captions.update" method.
 * 4. Download a caption track via "captions.download" method.
 * 5. Deleting an existing caption track via "captions.delete" method.
 *
 * @author Ibrahim Ulukaya
 */
object Captions {

    /**
     * Define a global instance of a YouTube object, which will be used to make
     * YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /**
     * Define a global variable that specifies the MIME type of the caption
     * being uploaded.
     */
    private val CAPTION_FILE_FORMAT = "*/*"

    /**
     * Define a global variable that specifies the caption download format.
     */
    private val SRT = "srt"

    /*
     * Prompt the user to enter a caption track ID. Then return the ID.
     */
    private val captionIDFromUser: String
        @Throws(IOException::class)
        get() {
            print("Please enter a caption track id: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            var captionId = bReader.readLine()

            println("You chose $captionId.")
            return captionId
        }

    /*
     * Prompt the user to enter a video ID. Then return the ID.
     */
    private val videoId: String
        @Throws(IOException::class)
        get() {
            print("Please enter a video id: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            var videoId = bReader.readLine()

            println("You chose $videoId for captions.")
            return videoId
        }

    /*
     * Prompt the user to enter a name for the caption track. Then return the name.
     */
    private// If nothing is entered, defaults to "YouTube For Developers".
    val name: String
        @Throws(IOException::class)
        get() {
            print("Please enter a caption track name: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            var name = bReader.readLine()

            if (name.length < 1) {
                name = "YouTube for Developers"
            }

            println("You chose $name as caption track name.")
            return name
        }

    /*
     * Prompt the user to enter a language for the caption track. Then return the language.
     */
    private// If nothing is entered, defaults to "en".
    val language: String
        @Throws(IOException::class)
        get() {
            print("Please enter the caption language: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            var language = bReader.readLine()

            if (language.length < 1) {
                language = "en"
            }

            println("You chose $language as caption track language.")
            return language
        }

    /*
     * Prompt the user to enter the path for the caption track file being uploaded.
     */
    private// Exit if the user does not provide a path to the file.
    val captionFromUser: File
        @Throws(IOException::class)
        get() {
            print("Please enter the path of the caption track file to upload: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            var path = bReader.readLine()

            if (path.length < 1) {
                print("Path can not be empty!")
                System.exit(1)
            }

            val captionFile = File(path)
            println("You chose $captionFile to upload.")

            return captionFile
        }

    /*
     * Prompt the user to enter the path for the caption track file being replaced.
     */
    private val updateCaptionFromUser: File?
        @Throws(IOException::class)
        get() {
            print("Please enter the path of the new caption track file to upload" + " (Leave empty if you don't want to upload a new file.):")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            var path = bReader.readLine()

            if (path.length < 1) {
                return null
            }

            val captionFile = File(path)
            println("You chose $captionFile to upload.")

            return captionFile
        }

    /*
     * Prompt the user to enter an action. Then return the action.
     */
    private val actionFromUser: String
        @Throws(IOException::class)
        get() {
            print("Please choose action to be accomplished: ")
            print("Options are: 'upload', 'list', 'update', 'download', 'delete'," + " and 'all' ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            var action = bReader.readLine()

            return action
        }


    /**
     * Upload, list, update, download, and delete caption tracks.
     *
     * @param args command line args (not used).
     */
    @JvmStatic
    fun main(args: Array<String>) {

        // This OAuth 2.0 access scope allows for full read/write access to the
        // authenticated user's account and requires requests to use an SSL connection.
        val scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.force-ssl")

        try {
            // Authorize the request.
            val credential = Auth.authorize(scopes, "captions")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-captions-sample").build()

            // Prompt the user to specify the action of the be achieved.
            val actionString = actionFromUser
            println("You chose $actionString.")

            val action = Action.valueOf(actionString.toUpperCase())
            when (action) {
                Action.UPLOAD -> uploadCaption(videoId, language, name, captionFromUser)
                Action.LIST -> listCaptions(videoId)
                Action.UPDATE -> updateCaption(captionIDFromUser, updateCaptionFromUser)
                Action.DOWNLOAD -> downloadCaption(captionIDFromUser)
                Action.DELETE -> deleteCaption(captionIDFromUser)
                else -> {
                    // All the available methods are used in sequence just for the sake
                    // of an example.

                    //Prompt the user to specify a video to upload the caption track for and
                    // a language, a name, a binary file for the caption track. Then upload the
                    // caption track with the values that are selected by the user.
                    val videoId = videoId
                    uploadCaption(videoId, language, name, captionFromUser)
                    val captions = listCaptions(videoId)
                    if (captions.isEmpty()) {
                        println("Can't get video caption tracks.")
                    } else {
                        // Retrieve the first uploaded caption track.
                        val firstCaptionId = captions[0].id

                        updateCaption(firstCaptionId, null)
                        downloadCaption(firstCaptionId)
                        deleteCaption(firstCaptionId)
                    }
                }
            }
        } catch (e: GoogleJsonResponseException) {
            System.err.println("GoogleJsonResponseException code: " + e.details.code
                    + " : " + e.details.message)
            e.printStackTrace()

        } catch (e: IOException) {
            System.err.println("IOException: " + e.message)
            e.printStackTrace()
        } catch (t: Throwable) {
            System.err.println("Throwable: " + t.message)
            t.printStackTrace()
        }

    }

    /**
     * Deletes a caption track for a YouTube video. (captions.delete)
     *
     * @param captionId The id parameter specifies the caption ID for the resource
     * that is being deleted. In a caption resource, the id property specifies the
     * caption track's ID.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun deleteCaption(captionId: String) {
        // Call the YouTube Data API's captions.delete method to
        // delete an existing caption track.
        youtube!!.captions().delete(captionId)
        println("  -  Deleted caption: $captionId")
    }

    /**
     * Downloads a caption track for a YouTube video. (captions.download)
     *
     * @param captionId The id parameter specifies the caption ID for the resource
     * that is being downloaded. In a caption resource, the id property specifies the
     * caption track's ID.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun downloadCaption(captionId: String) {
        // Create an API request to the YouTube Data API's captions.download
        // method to download an existing caption track.
        val captionDownload = youtube!!.captions().download(captionId).setTfmt(SRT)

        // Set the download type and add an event listener.
        val downloader = captionDownload.mediaHttpDownloader

        // Indicate whether direct media download is enabled. A value of
        // "True" indicates that direct media download is enabled and that
        // the entire media content will be downloaded in a single request.
        // A value of "False," which is the default, indicates that the
        // request will use the resumable media download protocol, which
        // supports the ability to resume a download operation after a
        // network interruption or other transmission failure, saving
        // time and bandwidth in the event of network failures.
        downloader.isDirectDownloadEnabled = false

        // Set the download state for the caption track file.
        val downloadProgressListener = MediaHttpDownloaderProgressListener { downloader ->
            when (downloader.downloadState) {
                MediaHttpDownloader.DownloadState.MEDIA_IN_PROGRESS -> {
                    println("Download in progress")
                    println("Download percentage: " + downloader.progress)
                }
                // This value is set after the entire media file has
                //  been successfully downloaded.
                MediaHttpDownloader.DownloadState.MEDIA_COMPLETE -> println("Download Completed!")
                // This value indicates that the download process has
                //  not started yet.
                MediaHttpDownloader.DownloadState.NOT_STARTED -> println("Download Not Started!")
            }
        }
        downloader.progressListener = downloadProgressListener

        val outputFile = FileOutputStream("captionFile.srt")
        // Download the caption track.
        captionDownload.executeAndDownloadTo(outputFile)
    }

    /**
     * Updates a caption track's draft status to publish it.
     * Updates the track with a new binary file as well if it is present.  (captions.update)
     *
     * @param captionId The id parameter specifies the caption ID for the resource
     * that is being updated. In a caption resource, the id property specifies the
     * caption track's ID.
     * @param captionFile caption track binary file.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun updateCaption(captionId: String, captionFile: File?) {
        // Modify caption's isDraft property to unpublish a caption track.
        val updateCaptionSnippet = CaptionSnippet()
        updateCaptionSnippet.isDraft = false
        val updateCaption = Caption()
        updateCaption.id = captionId
        updateCaption.snippet = updateCaptionSnippet

        val captionUpdateResponse: Caption

        if (captionFile == null) {
            // Call the YouTube Data API's captions.update method to update an existing caption track.
            captionUpdateResponse = youtube!!.captions().update("snippet", updateCaption).execute()

        } else {
            // Create an object that contains the caption file's contents.
            val mediaContent = InputStreamContent(
                    CAPTION_FILE_FORMAT, BufferedInputStream(FileInputStream(captionFile)))
            mediaContent.length = captionFile.length()

            // Create an API request that specifies that the mediaContent
            // object is the caption of the specified video.
            val captionUpdate = youtube!!.captions().update("snippet", updateCaption, mediaContent)

            // Set the upload type and add an event listener.
            val uploader = captionUpdate.mediaHttpUploader

            // Indicate whether direct media upload is enabled. A value of
            // "True" indicates that direct media upload is enabled and that
            // the entire media content will be uploaded in a single request.
            // A value of "False," which is the default, indicates that the
            // request will use the resumable media upload protocol, which
            // supports the ability to resume an upload operation after a
            // network interruption or other transmission failure, saving
            // time and bandwidth in the event of network failures.
            uploader.isDirectUploadEnabled = false

            // Set the upload state for the caption track file.
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

            // Upload the caption track.
            captionUpdateResponse = captionUpdate.execute()
            println("\n================== Uploaded New Caption Track ==================\n")
        }

        // Print information from the API response.
        println("\n================== Updated Caption Track ==================\n")
        val snippet = captionUpdateResponse.snippet
        println("  - ID: " + captionUpdateResponse.id)
        println("  - Name: " + snippet.name)
        println("  - Language: " + snippet.language)
        println("  - Draft Status: " + snippet.isDraft!!)
        println("\n-------------------------------------------------------------\n")
    }

    /**
     * Returns a list of caption tracks. (captions.listCaptions)
     *
     * @param videoId The videoId parameter instructs the API to return the
     * caption tracks for the video specified by the video id.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun listCaptions(videoId: String): List<Caption> {
        // Call the YouTube Data API's captions.list method to
        // retrieve video caption tracks.
        val captionListResponse = youtube!!.captions().list("snippet", videoId).execute()

        val captions = captionListResponse.items
        // Print information from the API response.
        println("\n================== Returned Caption Tracks ==================\n")
        var snippet: CaptionSnippet
        for (caption in captions) {
            snippet = caption.snippet
            println("  - ID: " + caption.id)
            println("  - Name: " + snippet.name)
            println("  - Language: " + snippet.language)
            println("\n-------------------------------------------------------------\n")
        }

        return captions
    }

    /**
     * Uploads a caption track in draft status that matches the API request parameters.
     * (captions.insert)
     *
     * @param videoId the YouTube video ID of the video for which the API should
     * return caption tracks.
     * @param captionLanguage language of the caption track.
     * @param captionName name of the caption track.
     * @param captionFile caption track binary file.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun uploadCaption(videoId: String, captionLanguage: String,
                              captionName: String, captionFile: File) {
        // Add extra information to the caption before uploading.
        val captionObjectDefiningMetadata = Caption()

        // Most of the caption's metadata is set on the CaptionSnippet object.
        var snippet = CaptionSnippet()

        // Set the video, language, name and draft status of the caption.
        snippet.videoId = videoId
        snippet.language = captionLanguage
        snippet.name = captionName
        snippet.isDraft = true

        // Add the completed snippet object to the caption resource.
        captionObjectDefiningMetadata.snippet = snippet

        // Create an object that contains the caption file's contents.
        val mediaContent = InputStreamContent(
                CAPTION_FILE_FORMAT, BufferedInputStream(FileInputStream(captionFile)))
        mediaContent.length = captionFile.length()

        // Create an API request that specifies that the mediaContent
        // object is the caption of the specified video.
        val captionInsert = youtube!!.captions().insert("snippet", captionObjectDefiningMetadata, mediaContent)

        // Set the upload type and add an event listener.
        val uploader = captionInsert.mediaHttpUploader

        // Indicate whether direct media upload is enabled. A value of
        // "True" indicates that direct media upload is enabled and that
        // the entire media content will be uploaded in a single request.
        // A value of "False," which is the default, indicates that the
        // request will use the resumable media upload protocol, which
        // supports the ability to resume an upload operation after a
        // network interruption or other transmission failure, saving
        // time and bandwidth in the event of network failures.
        uploader.isDirectUploadEnabled = false

        // Set the upload state for the caption track file.
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

        // Upload the caption track.
        val uploadedCaption = captionInsert.execute()

        // Print the metadata of the uploaded caption track.
        println("\n================== Uploaded Caption Track ==================\n")
        snippet = uploadedCaption.snippet
        println("  - ID: " + uploadedCaption.id)
        println("  - Name: " + snippet.name)
        println("  - Language: " + snippet.language)
        println("  - Status: " + snippet.status)
        println("\n-------------------------------------------------------------\n")
    }

    enum class Action {
        UPLOAD,
        LIST,
        UPDATE,
        DOWNLOAD,
        DELETE,
        ALL
    }
}
