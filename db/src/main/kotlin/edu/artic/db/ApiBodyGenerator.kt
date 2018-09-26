package edu.artic.db

/**
 * This file contains mechanisms for creating API calls to the general-purpose
 * [data API][edu.artic.db.models.ArticDataObject.dataApiUrl].
 *
 * For the moment, these are mostly of interest to functions that are
 * * retrieving [events][edu.artic.db.models.ArticEvent]
 * * retrieving [exhibitions][edu.artic.db.models.ArticExhibition]
 * * performing searches
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
sealed class ApiBodyGenerator {

    companion object {

        fun createExhibitionQueryBody(): MutableMap<String, Any> {
            val postParams = mutableMapOf<String, Any>()
            postParams["fields"] = listOf(
                    "id",
                    "title",
                    "short_description",
                    "legacy_image_mobile_url",
                    "legacy_image_desktop_url",
                    "gallery_id",
                    "web_url",
                    "aic_start_at",
                    "aic_end_at"
            )
            postParams["sort"] = listOf("aic_start_at", "aic_end_at")
            postParams["query"] = mutableMapOf<String, Any>().apply {
                //Boolean map
                this["bool"] = mutableMapOf<String, Any>().apply {
                    this["must"] = restrictToTimeFrame(
                            startKey = "aic_start_at",
                            endKey = "aic_end_at"
                    )

                    this["must_not"] = mutableListOf<Any>().apply {
                        this.add(mutableMapOf<String, Any>().apply {
                            //range
                            this["term"] = mutableMapOf<String, Any>().apply {
                                this["status"] = "Closed"
                            }
                        })
                    }
                }
            }
            return postParams
        }

        fun createSearchQueryBody(searchQuery: String): MutableList<MutableMap<String, Any>> {
            val artworkParams = createSearchArtworkQueryBody(searchQuery)

            val tourParams = createSearchTourQueryBody(searchQuery)


            val exhibitionParams = createSearchExhibitionQueryBody(searchQuery)


            return mutableListOf(artworkParams, tourParams, exhibitionParams)
        }

        fun createSearchArtworkQueryBody(searchQuery: String): MutableMap<String, Any> {
            val artworkParams = mutableMapOf<String, Any>()

            artworkParams["resources"] = "artworks"
            artworkParams["from"] = 0
            artworkParams["q"] = searchQuery
            artworkParams["fields"] = listOf(
                    "id",
                    "is_on_view",
                    "title",
                    "artist_display",
                    "image_id",
                    "gallery_id",
                    "latlon"
            )
            artworkParams["query"] = mutableMapOf<String, Any>().apply {
                this["term"] = mutableMapOf<String, Any>().apply {
                    this["is_on_view"] = "true"
                }
            }
            return artworkParams
        }

        fun createSearchTourQueryBody(searchQuery: String): MutableMap<String, Any> {
            val tourParams = mutableMapOf<String, Any>()

            tourParams["resources"] = "tours"
            tourParams["from"] = 0
            tourParams["q"] = searchQuery
            tourParams["fields"] = listOf(
                    "id"
            )
            tourParams["size"] = 99
            return tourParams
        }

        fun createSearchExhibitionQueryBody(searchQuery: String): MutableMap<String, Any> {
            val exhibitionParams = mutableMapOf<String, Any>()

            exhibitionParams["resources"] = "exhibitions"
            exhibitionParams["from"] = 0
            exhibitionParams["q"] = searchQuery
            exhibitionParams["fields"] = listOf(
                    "id",
                    "title",
                    "short_description",
                    "legacy_image_mobile_url",
                    "legacy_image_desktop_url",
                    "gallery_id",
                    "web_url",
                    "aic_start_at",
                    "aic_end_at"
            )
            exhibitionParams["size"] = 99
            exhibitionParams["query"] = mutableMapOf<String, Any>().apply {
                //Boolean map
                this["bool"] = mutableMapOf<String, Any>().apply {
                    this["must"] = restrictToTimeFrame(
                            startKey = "aic_start_at",
                            endKey = "aic_end_at"
                    )
                }
            }
            return exhibitionParams
        }

        fun createEventQueryBody(): MutableMap<String, Any> {
            val postParams = mutableMapOf<String, Any>()
            postParams["fields"] = listOf(
                    "id",
                    "title",
                    "description",
                    "short_description",
                    "image",
                    "image_url",
                    "location",
                    "start_at",
                    "end_at",
                    "button_text",
                    "button_url"
            )
            postParams["sort"] = listOf("start_at", "end_at")
            postParams["query"] = mutableMapOf<String, Any>().apply {
                //Boolean map
                this["bool"] = mutableMapOf<String, Any>().apply {
                    this["must"] = restrictToTimeFrame(
                            earliest = "now",
                            latest = "now+2w",
                            startKey = "start_at",
                            endKey = "end_at"
                    )
                }
            }
            return postParams
        }


        // Private utility functions only below this point

        /**
         * Use this to ensure that the API response only includes exhibitions
         * or events (or whatever) that are present in a certain timeframe.
         *
         * Times should be given in the appropriate format; by default, we
         * will restrict to whatever is active at the instant the API call
         * is made.
         */
        private fun restrictToTimeFrame(
                earliest: String = "now",
                latest: String = "now",
                startKey: String,
                endKey: String
        ): MutableList<Any> {

            return mutableListOf<Any>().apply {
                this.add(mutableMapOf<String, Any>().apply {
                    //range
                    this["range"] = mutableMapOf<String, Any>().apply {
                        this[startKey] = mutableMapOf<String, String>().apply {
                            this["lte"] = latest
                        }
                    }
                })

                this.add(mutableMapOf<String, Any>().apply {
                    this["range"] = mutableMapOf<String, Any>().apply {
                        this[endKey] = mutableMapOf<String, String>().apply {
                            this["gte"] = earliest
                        }
                    }
                })
            }
        }
    }
}