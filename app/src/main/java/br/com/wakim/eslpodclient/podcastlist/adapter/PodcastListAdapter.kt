package br.com.wakim.eslpodclient.podcastlist.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import br.com.wakim.eslpodclient.R
import br.com.wakim.eslpodclient.model.PodcastItem
import br.com.wakim.eslpodclient.podcastlist.view.PodcastListItemView
import java.util.*

class PodcastListAdapter : RecyclerView.Adapter<PodcastListAdapter.ViewHolder> {

    companion object {
        final const val LOADING_TYPE = 0
        final const val ITEM_TYPE = 1
    }

    val list : MutableList<PodcastItem> = mutableListOf()
    val layoutInflater : LayoutInflater

    var loading : Boolean = false
        set(value) {
            val old = field

            field = value

            if (old != value) {
                val size = list.size

                if (value)
                    notifyItemInserted(size)
                else
                    notifyItemRemoved(size)
            }
        }

    constructor(context: Context) : super() {
        layoutInflater = LayoutInflater.from(context)
    }

    override fun getItemViewType(position: Int): Int =
            if (loading && position == list.size) LOADING_TYPE else ITEM_TYPE

    override fun onCreateViewHolder(viewGroup: ViewGroup?, viewType: Int): PodcastListAdapter.ViewHolder? {
        return if (viewType == ITEM_TYPE) ViewHolder(layoutInflater.inflate(R.layout.podcast_list_item, viewGroup, false))
            else ViewHolder(layoutInflater.inflate(R.layout.list_item_loading, viewGroup, false))
    }

    override fun onBindViewHolder(viewHolder: PodcastListAdapter.ViewHolder?, position: Int) {
        if (viewHolder!!.itemViewType == LOADING_TYPE) {
            val lp = viewHolder.itemView.layoutParams as? RecyclerView.LayoutParams;
            lp?.height = if (list.size == 0) RecyclerView.LayoutParams.MATCH_PARENT else RecyclerView.LayoutParams.WRAP_CONTENT
        } else
            viewHolder.view()?.bind(list[position])
    }

    override fun getItemCount(): Int = list.size + if (loading) 1 else 0

    fun addAll(addition: ArrayList<PodcastItem>) {
        val previousSize = list.size

        list.addAll(addition)

        notifyItemRangeInserted(previousSize, addition.size)
    }

    class ViewHolder : RecyclerView.ViewHolder {
        constructor(view: View) : super(view)

        fun view() : PodcastListItemView? = itemView as? PodcastListItemView
    }
}
