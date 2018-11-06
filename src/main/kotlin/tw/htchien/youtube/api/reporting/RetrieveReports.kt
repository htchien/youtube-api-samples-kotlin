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

package tw.htchien.youtube.api.reporting

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.GenericUrl
import com.google.api.services.youtubereporting.YouTubeReporting
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * This sample retrieves reports created by a specific job by:
 *
 * 1. Listing the jobs using the "jobs.list" method.
 * 2. Retrieving reports using the "reports.list" method.
 *
 * @author Ibrahim Ulukaya
 */
object RetrieveReports {

    /**
     * Define a global instance of a YouTube Reporting object, which will be used to make
     * YouTube Reporting API requests.
     */
    private var youtubeReporting: YouTubeReporting? = null

    /*
     * Prompt the user to enter a job id for report retrieval. Then return the id.
     */
    private val jobIdFromUser: String
        @Throws(IOException::class)
        get() {
            print("Please enter the job id for the report retrieval: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            var id = bReader.readLine()

            println("You chose $id as the job Id for the report retrieval.")
            return id
        }

    /*
     * Prompt the user to enter a URL for report download. Then return the URL.
     */
    private val reportUrlFromUser: String
        @Throws(IOException::class)
        get() {
            print("Please enter the report URL to download: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            var url = bReader.readLine()

            println("You chose $url as the URL to download.")
            return url
        }


    /**
     * Retrieve reports.
     *
     * @param args command line args (not used).
     */
    @JvmStatic
    fun main(args: Array<String>) {

        /*
         * This OAuth 2.0 access scope allows for read access to the YouTube Analytics monetary reports for
         * authenticated user's account. Any request that retrieves earnings or ad performance metrics must
         * use this scope.
         */
        val scopes = Lists.newArrayList("https://www.googleapis.com/auth/yt-analytics-monetary.readonly")

        try {
            // Authorize the request.
            val credential = Auth.authorize(scopes, "retrievereports")

            // This object is used to make YouTube Reporting API requests.
            youtubeReporting = YouTubeReporting.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-retrievereports-sample").build()

            if (listReportingJobs()) {
                if (retrieveReports(jobIdFromUser)) {
                    downloadReport(reportUrlFromUser)
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
     * Lists reporting jobs. (jobs.listJobs)
     * @return true if at least one reporting job exists
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun listReportingJobs(): Boolean {
        // Call the YouTube Reporting API's jobs.list method to retrieve reporting jobs.
        val jobsListResponse = youtubeReporting!!.jobs().list().execute()
        val jobsList = jobsListResponse.jobs

        if (jobsList == null || jobsList.isEmpty()) {
            println("No jobs found.")
            return false
        } else {
            // Print information from the API response.
            println("\n================== Reporting Jobs ==================\n")
            for (job in jobsList) {
                println("  - Id: " + job.id)
                println("  - Name: " + job.name)
                println("  - Report Type Id: " + job.reportTypeId)
                println("\n-------------------------------------------------------------\n")
            }
        }
        return true
    }

    /**
     * Lists reports created by a specific job. (reports.listJobsReports)
     *
     * @param jobId The ID of the job.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun retrieveReports(jobId: String): Boolean {
        // Call the YouTube Reporting API's reports.list method
        // to retrieve reports created by a job.
        val reportsListResponse = youtubeReporting!!.jobs().reports().list(jobId).execute()
        val reportslist = reportsListResponse.reports

        if (reportslist == null || reportslist.isEmpty()) {
            println("No reports found.")
            return false
        } else {
            // Print information from the API response.
            println("\n============= Reports for the job $jobId =============\n")
            for (report in reportslist) {
                println("  - Id: " + report.id)
                println("  - From: " + report.startTime)
                println("  - To: " + report.endTime)
                println("  - Download Url: " + report.downloadUrl)
                println("\n-------------------------------------------------------------\n")
            }
        }
        return true
    }

    /**
     * Download the report specified by the URL. (media.download)
     *
     * @param reportUrl The URL of the report to be downloaded.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun downloadReport(reportUrl: String): Boolean {
        // Call the YouTube Reporting API's media.download method to download a report.
        val request = youtubeReporting!!.media().download("")
        val fop = FileOutputStream(File("report"))
        request.mediaHttpDownloader.download(GenericUrl(reportUrl), fop)
        return true
    }
}

