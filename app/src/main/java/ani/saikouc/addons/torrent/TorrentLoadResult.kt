package ani.saikouc.addons.torrent

import ani.saikouc.addons.LoadResult

open class TorrentLoadResult : LoadResult() {
    class Success(val extension: TorrentAddon.Installed) : TorrentLoadResult()
}