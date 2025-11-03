package com.example.vendorconnect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchResultAdapter(
    private val results: List<PlaceSearchResult>,
    private val onItemClick: (PlaceSearchResult) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeName: TextView = itemView.findViewById(R.id.textViewPlaceName)
        val placeAddress: TextView = itemView.findViewById(R.id.textViewPlaceAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        holder.placeName.text = result.name
        holder.placeAddress.text = result.address
        
        holder.itemView.setOnClickListener {
            onItemClick(result)
        }
    }

    override fun getItemCount(): Int = results.size
}
