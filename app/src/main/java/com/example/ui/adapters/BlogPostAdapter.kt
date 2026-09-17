package com.example.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.model.BlogPost

/**
 * RecyclerView ListAdapter dynamically receiving smart-merged BlogPost items
 * (Firebase Realtime Database items merged with local hardcoded items, deduplicated and sorted).
 */
class BlogPostAdapter(
    private val onItemClick: ((BlogPost) -> Unit)? = null
) : ListAdapter<BlogPost, BlogPostAdapter.BlogPostViewHolder>(BlogPostDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlogPostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_blog_post, parent, false)
        return BlogPostViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: BlogPostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BlogPostViewHolder(
        itemView: View,
        private val onItemClick: ((BlogPost) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView? = itemView.findViewById(R.id.blogTitleTextView)
        private val dateView: TextView? = itemView.findViewById(R.id.blogDateTextView)
        private val excerptView: TextView? = itemView.findViewById(R.id.blogExcerptTextView)

        fun bind(post: BlogPost) {
            titleView?.text = post.title
            dateView?.text = post.publishedDate
            excerptView?.text = post.plainTextExcerpt
            itemView.setOnClickListener {
                onItemClick?.invoke(post)
            }
        }
    }

    object BlogPostDiffCallback : DiffUtil.ItemCallback<BlogPost>() {
        override fun areItemsTheSame(oldItem: BlogPost, newItem: BlogPost): Boolean {
            return oldItem.id == newItem.id || (oldItem.url.isNotBlank() && oldItem.url == newItem.url)
        }

        override fun areContentsTheSame(oldItem: BlogPost, newItem: BlogPost): Boolean {
            return oldItem == newItem
        }
    }
}
