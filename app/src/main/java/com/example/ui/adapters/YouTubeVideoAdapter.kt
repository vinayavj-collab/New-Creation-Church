package com.example.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.model.YouTubeVideo
import com.example.util.VideoPlatform
import com.example.util.VideoUrlParser

/**
 * RecyclerView Adapter dynamically receiving video items with Endless Scrolling / Pagination support.
 * Supports:
 * - Seamless appending with [appendVideos] using notifyItemRangeInserted (no scroll resets)
 * - Subtle loading spinner at the bottom when loading more items
 * - Smart Routing for YouTube and Dailymotion video items
 */
class YouTubeVideoAdapter(
    private val onItemClick: ((video: YouTubeVideo, platform: VideoPlatform) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_ITEM = 0
        const val VIEW_TYPE_LOADING = 1
    }

    private val items = mutableListOf<YouTubeVideo>()
    private var isLoadingFooter = false

    // Overloaded secondary constructor for backward compatibility
    constructor(onSingleClick: (YouTubeVideo) -> Unit) : this({ video, _ -> onSingleClick(video) })

    override fun getItemCount(): Int = items.size + if (isLoadingFooter) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if (isLoadingFooter && position == items.size) {
            VIEW_TYPE_LOADING
        } else {
            VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_LOADING) {
            val view = inflater.inflate(R.layout.item_video_loading, parent, false)
            LoadingViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_youtube_video, parent, false)
            YouTubeVideoViewHolder(view, onItemClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is YouTubeVideoViewHolder && position < items.size) {
            holder.bind(items[position])
        } else if (holder is LoadingViewHolder) {
            holder.bind()
        }
    }

    /**
     * Sets initial video list.
     */
    fun setVideos(newVideos: List<YouTubeVideo>) {
        val oldSize = items.size
        items.clear()
        items.addAll(newVideos)
        if (oldSize == 0) {
            notifyItemRangeInserted(0, newVideos.size)
        } else {
            notifyDataSetChanged()
        }
    }

    /**
     * Seamlessly appends new videos to the end of the existing list.
     * Uses notifyItemRangeInserted to strictly maintain scroll position.
     */
    fun appendVideos(newVideos: List<YouTubeVideo>) {
        if (newVideos.isEmpty()) return
        val existingIds = items.map { it.id }.toSet()
        val distinctNew = newVideos.filterNot { existingIds.contains(it.id) }
        if (distinctNew.isEmpty()) return

        val startPosition = items.size
        items.addAll(distinctNew)
        notifyItemRangeInserted(startPosition, distinctNew.size)
    }

    /**
     * Toggles subtle bottom loading spinner without resetting scroll position.
     */
    fun setLoadingFooter(loading: Boolean) {
        if (isLoadingFooter == loading) return
        isLoadingFooter = loading
        if (loading) {
            notifyItemInserted(items.size)
        } else {
            notifyItemRemoved(items.size)
        }
    }

    /**
     * Backward-compatible helper for ListAdapter submitList calls.
     */
    fun submitList(list: List<YouTubeVideo>?) {
        if (list == null) {
            val count = items.size
            items.clear()
            notifyItemRangeRemoved(0, count)
        } else if (items.isEmpty()) {
            setVideos(list)
        } else {
            // Check if it's an append or complete update
            val currentIds = items.map { it.id }.toSet()
            val hasNewItems = list.any { !currentIds.contains(it.id) }
            if (hasNewItems && list.size > items.size) {
                val newItems = list.filterNot { currentIds.contains(it.id) }
                appendVideos(newItems)
            } else {
                items.clear()
                items.addAll(list)
                notifyDataSetChanged()
            }
        }
    }

    fun getItems(): List<YouTubeVideo> = items.toList()

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val progressBar: ProgressBar? = itemView.findViewById(R.id.loadingProgressBar)
        fun bind() {
            progressBar?.visibility = View.VISIBLE
        }
    }

    class YouTubeVideoViewHolder(
        itemView: View,
        private val onItemClick: ((video: YouTubeVideo, platform: VideoPlatform) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val platformTagView: TextView? = itemView.findViewById(R.id.videoPlatformTagTextView)
        private val titleView: TextView? = itemView.findViewById(R.id.videoTitleTextView)
        private val channelView: TextView? = itemView.findViewById(R.id.videoChannelTextView)
        private val dateView: TextView? = itemView.findViewById(R.id.videoDateTextView)

        fun bind(video: YouTubeVideo) {
            titleView?.text = video.title
            channelView?.text = video.channelTitle
            dateView?.text = video.publishedAt

            // Smart Routing: Analyze URL format
            val parsed = VideoUrlParser.parse(if (video.videoUrl.isNotBlank()) video.videoUrl else video.id)
            when (parsed.platform) {
                VideoPlatform.DAILYMOTION -> {
                    platformTagView?.text = "DAILYMOTION"
                    platformTagView?.setBackgroundColor(Color.parseColor("#0066DC"))
                    channelView?.setTextColor(Color.parseColor("#0066DC"))
                }
                VideoPlatform.DIRECT_STREAM -> {
                    platformTagView?.text = "MEDIA"
                    platformTagView?.setBackgroundColor(Color.parseColor("#E65100"))
                    channelView?.setTextColor(Color.parseColor("#E65100"))
                }
                VideoPlatform.YOUTUBE -> {
                    platformTagView?.text = "YOUTUBE"
                    platformTagView?.setBackgroundColor(Color.parseColor("#D32F2F"))
                    channelView?.setTextColor(Color.parseColor("#D32F2F"))
                }
                VideoPlatform.UNKNOWN -> {
                    platformTagView?.text = "VIDEO"
                    platformTagView?.setBackgroundColor(Color.parseColor("#455A64"))
                    channelView?.setTextColor(Color.parseColor("#455A64"))
                }
            }

            // Intercept click event, route to player logic
            itemView.setOnClickListener {
                onItemClick?.invoke(video, parsed.platform)
            }
        }
    }

    object YouTubeVideoDiffCallback : DiffUtil.ItemCallback<YouTubeVideo>() {
        override fun areItemsTheSame(oldItem: YouTubeVideo, newItem: YouTubeVideo): Boolean {
            return oldItem.id == newItem.id || (oldItem.videoUrl.isNotBlank() && oldItem.videoUrl == newItem.videoUrl)
        }

        override fun areContentsTheSame(oldItem: YouTubeVideo, newItem: YouTubeVideo): Boolean {
            return oldItem == newItem
        }
    }
}

