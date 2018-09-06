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
                    this["must"] = mutableListOf<Any>().apply {
                        this.add(mutableMapOf<String, Any>().apply {
                            //range
                            this["range"] = mutableMapOf<String, Any>().apply {
                                this["aic_start_at"] = mutableMapOf<String, String>().apply {
                                    this["lte"] = "now"
                                }
                            }
                        })

                        this.add(mutableMapOf<String, Any>().apply {
                            this["range"] = mutableMapOf<String, Any>().apply {
                                this["aic_end_at"] = mutableMapOf<String, String>().apply {
                                    this["gte"] = "now"
                                }
                            }
                        })
                    }

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

        fun createEventQueryBody(): MutableMap<String, Any> {
            val postParams = mutableMapOf<String, Any>()
            postParams["fields"] = listOf(
                    "id",
                    "title",
                    "description",
                    "short_description",
                    "image",
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
                    this["must"] = mutableListOf<Any>().apply {
                        this.add(mutableMapOf<String, Any>().apply {
                            //range
                            this["range"] = mutableMapOf<String, Any>().apply {
                                this["start_at"] = mutableMapOf<String, String>().apply {
                                    this["lte"] = "now+2w"
                                }
                            }
                        })

                        this.add(mutableMapOf<String, Any>().apply {
                            this["range"] = mutableMapOf<String, Any>().apply {
                                this["end_at"] = mutableMapOf<String, String>().apply {
                                    this["gte"] = "now"
                                }
                            }
                        })
                    }
                }
            }
            return postParams
        }
    }
}