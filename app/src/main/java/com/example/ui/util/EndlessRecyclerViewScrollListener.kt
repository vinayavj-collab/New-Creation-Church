package com.example.ui.util

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * Custom EndlessRecyclerViewScrollListener for continuous seamless pagination.
 * Supports LinearLayoutManager, GridLayoutManager, and StaggeredGridLayoutManager.
 * Detects when user scrolls near the bottom threshold and triggers [onLoadMore].
 */
abstract class EndlessRecyclerViewScrollListener : RecyclerView.OnScrollListener {
    // The minimum number of items to have below your current scroll position before loading more.
    private var visibleThreshold = 4

    // The current offset index of data you have loaded
    private var currentPage = 0

    // The total number of items in the dataset after the last load
    private var previousTotalItemCount = 0

    // True if we are still waiting for the last set of data to load.
    private var loading = true

    // Sets the starting page index
    private val startingPageIndex = 0

    private var layoutManager: RecyclerView.LayoutManager

    constructor(layoutManager: LinearLayoutManager, visibleThreshold: Int = 4) {
        this.layoutManager = layoutManager
        this.visibleThreshold = visibleThreshold
    }

    constructor(layoutManager: GridLayoutManager, visibleThreshold: Int = 4) {
        this.layoutManager = layoutManager
        this.visibleThreshold = visibleThreshold * layoutManager.spanCount
    }

    constructor(layoutManager: StaggeredGridLayoutManager, visibleThreshold: Int = 4) {
        this.layoutManager = layoutManager
        this.visibleThreshold = visibleThreshold * layoutManager.spanCount
    }

    private fun getLastVisibleItem(lastVisibleItemPositions: IntArray): Int {
        var maxSize = 0
        for (i in lastVisibleItemPositions.indices) {
            if (i == 0) {
                maxSize = lastVisibleItemPositions[i]
            } else if (lastVisibleItemPositions[i] > maxSize) {
                maxSize = lastVisibleItemPositions[i]
            }
        }
        return maxSize
    }

    // This happens many times a second during a scroll, so be wary of the code you place here.
    override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
        // If scrolling upwards or zero vertical scroll, ignore
        if (dy <= 0) return

        val totalItemCount = layoutManager.itemCount
        var lastVisibleItemPosition = 0

        when (val lm = layoutManager) {
            is StaggeredGridLayoutManager -> {
                val lastVisibleItemPositions = lm.findLastVisibleItemPositions(null)
                lastVisibleItemPosition = getLastVisibleItem(lastVisibleItemPositions)
            }
            is GridLayoutManager -> {
                lastVisibleItemPosition = lm.findLastVisibleItemPosition()
            }
            is LinearLayoutManager -> {
                lastVisibleItemPosition = lm.findLastVisibleItemPosition()
            }
        }

        // If the total item count is zero and the previous isn't, assume the
        // list is invalidated and should be reset back to initial state
        if (totalItemCount < previousTotalItemCount) {
            this.currentPage = this.startingPageIndex
            this.previousTotalItemCount = totalItemCount
            if (totalItemCount == 0) {
                this.loading = true
            }
        }

        // If it's still loading, we check to see if the dataset count has
        // changed, if so we conclude it has finished loading and update the current page
        // number and total item count.
        if (loading && (totalItemCount > previousTotalItemCount)) {
            loading = false
            previousTotalItemCount = totalItemCount
        }

        // If it isn't currently loading, we check to see if we have breached
        // the visibleThreshold and need to reload more data.
        // If we do need to reload some more data, we execute onLoadMore to fetch the data.
        if (!loading && (lastVisibleItemPosition + visibleThreshold) >= totalItemCount) {
            currentPage++
            onLoadMore(currentPage, totalItemCount, view)
            loading = true
        }
    }

    // Call this method whenever performing new searches or refreshing from scratch
    fun resetState() {
        this.currentPage = this.startingPageIndex
        this.previousTotalItemCount = 0
        this.loading = true
    }

    // Abstract method to implement in caller
    abstract fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?)
}
