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
import com.google.api.client.http.InputStreamContent
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Channel
import com.google.api.services.youtube.model.InvideoBranding
import com.google.api.services.youtube.model.InvideoPromotion
import com.google.api.services.youtube.model.InvideoTiming
import com.google.api.services.youtube.model.PromotedItem
import com.google.api.services.youtube.model.PromotedItemId
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.IOException
import java.math.BigInteger

/**
 * Add a featured video to a channel.
 *
 * @author Ikai Lan <ikai></ikai>@google.com>
 */
object InvideoProgramming {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /**
     * This sample video introduces WebM on YouTube Developers Live.
     */
    private val FEATURED_VIDEO_ID = "w4eiUiauo2w"

    /**
     * This code sample demonstrates different ways that the API can be used to
     * promote your channel content. It includes code for the following tasks:
     *
     *  1. Feature a video.
     *  1. Feature a link to a social media channel.
     *  1. Set a watermark for videos on your channel.
     *
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
            val credential = Auth.authorize(scopes, "invideoprogramming")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-invideoprogramming-sample")
                    .build()

            // Construct a request to retrieve the current user's channel ID.
            // In the API response, only include channel information needed for
            // this use case. The channel's uploads playlist identifies the
            // channel's most recently uploaded video.
            // See https://developers.google.com/youtube/v3/docs/channels/list
            val channelListResponse = youtube!!.channels().list("id,contentDetails")
                    .setMine(true)
                    .setFields("items(contentDetails/relatedPlaylists/uploads,id)")
                    .execute()

            // The user's default channel is the first item in the list. If the
            // user does not have a channel, this code should throw a
            // GoogleJsonResponseException explaining the issue.
            val myChannel = channelListResponse.items[0]
            val channelId = myChannel.id

            // The promotion appears 15000ms (15 seconds) before the video ends.
            val invideoTiming = InvideoTiming()
            invideoTiming.offsetMs = BigInteger.valueOf(15000L)
            invideoTiming.type = "offsetFromEnd"

            // This is one type of promotion and promotes a video.
            val promotedItemId = PromotedItemId()
            promotedItemId.type = "video"
            promotedItemId.videoId = FEATURED_VIDEO_ID

            // Set a custom message providing additional information about the
            // promoted video or your channel.
            var promotedItem = PromotedItem()
            promotedItem.customMessage = "Check out this video about WebM!"
            promotedItem.id = promotedItemId

            // Construct an object representing the invideo promotion data, and
            // add it to the channel.
            val invideoPromotion = InvideoPromotion()
            invideoPromotion.defaultTiming = invideoTiming
            invideoPromotion.items = Lists.newArrayList(promotedItem)

            val channel = Channel()
            channel.id = channelId
            channel.invideoPromotion = invideoPromotion

            var updateChannelResponse = youtube!!.channels()
                    .update("invideoPromotion", channel)
                    .execute()

            // Print data from the API response.
            println("\n================== Updated Channel Information ==================\n")
            println("\t- Channel ID: " + updateChannelResponse.id)

            var promotions = updateChannelResponse.invideoPromotion
            promotedItem = promotions.items[0] // We only care about the first item
            println("\t- Invideo promotion video ID: " + promotedItem
                    .id
                    .videoId)
            println("\t- Promotion message: " + promotedItem.customMessage)

            // In-video programming can also be used to feature links to
            // associated websites, merchant sites, or social networking sites.
            // The code below overrides the promotional video set above by
            // featuring a link to the YouTube Developers Twitter feed.
            val promotedTwitterFeed = PromotedItemId()
            promotedTwitterFeed.type = "website"
            promotedTwitterFeed.websiteUrl = "https://twitter.com/youtubedev"

            promotedItem = PromotedItem()
            promotedItem.customMessage = "Follow us on Twitter!"
            promotedItem.id = promotedTwitterFeed

            invideoPromotion.items = Lists.newArrayList(promotedItem)
            channel.invideoPromotion = invideoPromotion

            // Call the API to set the in-video promotion data.
            updateChannelResponse = youtube!!.channels()
                    .update("invideoPromotion", channel)
                    .execute()

            // Print data from the API response.
            println("\n================== Updated Channel Information ==================\n")
            println("\t- Channel ID: " + updateChannelResponse.id)

            promotions = updateChannelResponse.invideoPromotion
            promotedItem = promotions.items[0]
            println("\t- Invideo promotion URL: " + promotedItem
                    .id
                    .websiteUrl)
            println("\t- Promotion message: " + promotedItem.customMessage)

            // This example sets a custom watermark for the channel. The image
            // used is the watermark.jpg file in the "resources/" directory.
            val mediaContent = InputStreamContent("image/jpeg",
                    InvideoProgramming::class.java.getResourceAsStream("/watermark.jpg"))

            // Indicate that the watermark should display during the last 15
            // seconds of the video.
            val watermarkTiming = InvideoTiming()
            watermarkTiming.type = "offsetFromEnd"
            watermarkTiming.durationMs = BigInteger.valueOf(15000L)
            watermarkTiming.offsetMs = BigInteger.valueOf(15000L)

            val invideoBranding = InvideoBranding()
            invideoBranding.timing = watermarkTiming
            youtube!!.watermarks().set(channelId, invideoBranding, mediaContent).execute()

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
