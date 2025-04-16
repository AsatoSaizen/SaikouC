package ani.saikouc.addons.download

import ani.saikouc.addons.LoadResult

open class DownloadLoadResult : LoadResult() {
    class Success(val extension: DownloadAddon.Installed) : DownloadLoadResult()
}