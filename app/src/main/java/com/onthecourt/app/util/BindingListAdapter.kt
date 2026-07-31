package com.onthecourt.app.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

// Every list adapter in the app (friends, requests, search results, games, players,
// chat messages, courts) repeated the same inflate/bind/DiffUtil boilerplate. This base
// class does that part once; subclasses only say how to inflate the row and how to fill it.
abstract class BindingListAdapter<T : Any, VB : ViewBinding>(
    private val inflate: (LayoutInflater, ViewGroup, Boolean) -> VB,
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BindingListAdapter.BindingViewHolder<VB>>(diffCallback) {

    abstract fun bind(binding: VB, item: T)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder<VB> {
        return BindingViewHolder(inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: BindingViewHolder<VB>, position: Int) {
        bind(holder.binding, getItem(position))
    }

    class BindingViewHolder<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)
}

// Shorthand for the "same shape every time" DiffUtil callback: items match by some key
// (usually an id), contents match by full data-class equality.
private class KeyDiffCallback<T : Any>(private val itemsSame: (T, T) -> Boolean) : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T) = itemsSame(oldItem, newItem)
    override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
}

fun <T : Any> diffCallbackBy(itemsSame: (T, T) -> Boolean): DiffUtil.ItemCallback<T> =
    KeyDiffCallback(itemsSame)
