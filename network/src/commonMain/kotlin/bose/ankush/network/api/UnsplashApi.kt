package bose.ankush.network.api

import bose.ankush.network.model.UnsplashPhoto

interface UnsplashApi {
    suspend fun searchPhotos(
        query: String,
        perPage: Int = 1,
    ): List<UnsplashPhoto>

    suspend fun trackDownload(downloadLocation: String)
}
