package ani.saikouc.others

import ani.saikouc.FileUrl
import ani.saikouc.Mapper
import ani.saikouc.client
import ani.saikouc.media.anime.Episode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

object Anify {
    suspend fun fetchAndParseMetadata(id: Int): Map<String, Episode> {
        val response = client.get("https://anify.eltik.cc/content-metadata/$id")
            .parsed<JsonArray>().map {
                Mapper.json.decodeFromJsonElement<AnifyElement>(it)
            }
        return response.firstOrNull()?.data?.associate {
            it.number.toString() to Episode(
                number = it.number.toString(),
                title = it.title,
                desc = it.description,
                thumb = FileUrl[it.img],
            )
        } ?: emptyMap()
    }

    @Serializable
    data class AnifyElement(
        @SerialName("providerId")
        val providerID: String? = null,
        val data: List<Datum>? = null
    )

    @Serializable
    data class Datum(
        val id: String? = null,
        val description: String? = null,
        val hasDub: Boolean? = null,
        val img: String? = null,
        val isFiller: Boolean? = null,
        val number: Long? = null,
        val title: String? = null,
        val updatedAt: Long? = null,
        val rating: Double? = null
    )
}
