package ani.saikouc.media.anime

import ani.saikouc.media.MediaDetailsViewModel
import ani.saikouc.media.SourceAdapter
import ani.saikouc.media.SourceSearchDialogFragment
import ani.saikouc.parsers.ShowResponse
import kotlinx.coroutines.CoroutineScope

class AnimeSourceAdapter(
    sources: List<ShowResponse>,
    val model: MediaDetailsViewModel,
    val i: Int,
    val id: Int,
    fragment: SourceSearchDialogFragment,
    scope: CoroutineScope
) : SourceAdapter(sources, fragment, scope) {

    override suspend fun onItemClick(source: ShowResponse) {
        model.overrideEpisodes(i, source, id)
    }
}