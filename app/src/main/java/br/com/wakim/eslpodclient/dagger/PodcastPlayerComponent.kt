package br.com.wakim.eslpodclient.dagger

import br.com.wakim.eslpodclient.dagger.module.PodcastPlayerModule
import br.com.wakim.eslpodclient.dagger.scope.PodcastPlayerScope
import br.com.wakim.eslpodclient.ui.podcastlist.downloaded.view.DownloadedListFragment
import br.com.wakim.eslpodclient.ui.podcastlist.favorited.view.FavoritedListFragment
import br.com.wakim.eslpodclient.ui.podcastlist.view.PodcastListFragment
import dagger.Subcomponent

@PodcastPlayerScope
@Subcomponent(modules = arrayOf(PodcastPlayerModule::class))
interface PodcastPlayerComponent {
    fun inject(podcastListFragment: PodcastListFragment)
    fun inject(favoritedListFragment: FavoritedListFragment)
    fun inject(downloadedListFragment: DownloadedListFragment)
}
