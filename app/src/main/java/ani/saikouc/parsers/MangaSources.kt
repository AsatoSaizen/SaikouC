package ani.saikouc.parsers

import ani.saikouc.Lazier
import ani.saikouc.lazyList
import ani.saikouc.settings.saving.PrefManager
import ani.saikouc.settings.saving.PrefName
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

object MangaSources : MangaReadSources() {
    override var list: List<Lazier<BaseParser>> = emptyList()
    var pinnedMangaSources: List<String> = emptyList()
    var isInitialized = false

    suspend fun init(fromExtensions: StateFlow<List<MangaExtension.Installed>>) {
        pinnedMangaSources =
            PrefManager.getNullableVal<List<String>>(PrefName.MangaSourcesOrder, null)
                ?: emptyList()

        // Initialize with the first value from StateFlow
        val initialExtensions = fromExtensions.first()
        list = createParsersFromExtensions(initialExtensions) + Lazier(
            { OfflineMangaParser() },
            "Downloaded"
        )
        isInitialized = true

        // Update as StateFlow emits new values
        fromExtensions.collect { extensions ->
            list = sortPinnedMangaSources(
                createParsersFromExtensions(extensions),
                pinnedMangaSources
            ) + Lazier(
                { OfflineMangaParser() },
                "Downloaded"
            )
        }
    }

    fun performReorderMangaSources() {
        //remove the downloaded source from the list to avoid duplicates
        list = list.filter { it.name != "Downloaded" }
        list = sortPinnedMangaSources(list, pinnedMangaSources) + Lazier(
            { OfflineMangaParser() },
            "Downloaded"
        )
    }

    private fun createParsersFromExtensions(extensions: List<MangaExtension.Installed>): List<Lazier<BaseParser>> {
        return extensions.map { extension ->
            val name = extension.name
            Lazier({ DynamicMangaParser(extension) }, name)
        }
    }

    private fun sortPinnedMangaSources(
        sources: List<Lazier<BaseParser>>,
        pinnedMangaSources: List<String>
    ): List<Lazier<BaseParser>> {
        val pinnedSourcesMap = sources.filter { pinnedMangaSources.contains(it.name) }
            .associateBy { it.name }
        val orderedPinnedSources = pinnedMangaSources.mapNotNull { name ->
            pinnedSourcesMap[name]
        }
        val unpinnedSources = sources.filterNot { pinnedMangaSources.contains(it.name) }
        return orderedPinnedSources + unpinnedSources
    }
}

object HMangaSources : MangaReadSources() {
    private val aList: List<Lazier<BaseParser>> = lazyList()
    override val list = listOf(aList, MangaSources.list).flatten()
}
