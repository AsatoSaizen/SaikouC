package ani.saikouc.settings.saving.internal

import kotlin.reflect.KClass


data class Pref(
    val prefLocation: Location,
    val type: KClass<*>,
    val default: Any
)

enum class Location(val location: String, val exportable: Boolean) {
    General("ani.saikouc.general", true),
    UI("ani.saikouc.ui", true),
    Player("ani.saikouc.player", true),
    Reader("ani.saikouc.reader", true),
    NovelReader("ani.saikouc.novelReader", true),
    Irrelevant("ani.saikouc.irrelevant", false),
    AnimeDownloads("animeDownloads", false),  //different for legacy reasons
    Protected("ani.saikouc.protected", true),
}
