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
import com.google.api.services.youtubereporting.YouTubeReporting
import com.google.api.services.youtubereporting.model.Job
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * This sample creates a reporting job by:
 *
 * 1. Listing the available report types using the "reportTypes.list" method.
 * 2. Creating a reporting job using the "jobs.create" method.
 *
 * @author Ibrahim Ulukaya
 */
object CreateReportingJob {

    /**
     * Define a global instance of a YouTube Reporting object, which will be used to make
     * YouTube Reporting API requests.
     */
    private var youtubeReporting: YouTubeReporting? = null

    /*
     * Prompt the user to enter a name for the job. Then return the name.
     */
    private// If nothing is entered, defaults to "javaTestJob".
    val nameFromUser: String
        @Throws(IOException::class)
        get() {
            print("Please enter the name for the job [javaTestJob]: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            var name = bReader.readLine()

            if (name.length < 1) {
                name = "javaTestJob"
            }

            println("You chose $name as the name for the job.")
            return name
        }

    /*
     * Prompt the user to enter a report type id for the job. Then return the id.
     */
    private val reportTypeIdFromUser: String
        @Throws(IOException::class)
        get() {
            print("Please enter the reportTypeId for the job: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            var id = bReader.readLine()

            println("You chose $id as the report type Id for the job.")
            return id
        }


    /**
     * Create a reporting job.
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
            val credential = Auth.authorize(scopes, "createreportingjob")

            // This object is used to make YouTube Reporting API requests.
            youtubeReporting = YouTubeReporting.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-createreportingjob-sample").build()

            // Prompt the user to specify the name of the job to be created.
            val name = nameFromUser

            if (listReportTypes()) {
                createReportingJob(reportTypeIdFromUser, name)
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
     * Lists report types. (reportTypes.listReportTypes)
     * @return true if at least one report type exists
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun listReportTypes(): Boolean {
        // Call the YouTube Reporting API's reportTypes.list method to retrieve report types.
        val reportTypesListResponse = youtubeReporting!!.reportTypes().list()
                .execute()
        val reportTypeList = reportTypesListResponse.reportTypes

        if (reportTypeList == null || reportTypeList.isEmpty()) {
            println("No report types found.")
            return false
        } else {
            // Print information from the API response.
            println("\n================== Report Types ==================\n")
            for (reportType in reportTypeList) {
                println("  - Id: " + reportType.id)
                println("  - Name: " + reportType.name)
                println("\n-------------------------------------------------------------\n")
            }
        }
        return true
    }

    /**
     * Creates a reporting job. (jobs.create)
     *
     * @param reportTypeId Id of the job's report type.
     * @param name name of the job.
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun createReportingJob(reportTypeId: String, name: String) {
        // Create a reporting job with a name and a report type id.
        val job = Job()
        job.reportTypeId = reportTypeId
        job.name = name

        // Call the YouTube Reporting API's jobs.create method to create a job.
        val createdJob = youtubeReporting!!.jobs().create(job).execute()

        // Print information from the API response.
        println("\n================== Created reporting job ==================\n")
        println("  - ID: " + createdJob.id)
        println("  - Name: " + createdJob.name)
        println("  - Report Type Id: " + createdJob.reportTypeId)
        println("  - Create Time: " + createdJob.createTime)
        println("\n-------------------------------------------------------------\n")
    }
}
