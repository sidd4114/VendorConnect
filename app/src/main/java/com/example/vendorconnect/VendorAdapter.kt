package com.example.vendorconnect

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VendorAdapter(
    private var vendors: List<Vendor>,
    private val onItemClick: (Vendor) -> Unit
) : RecyclerView.Adapter<VendorAdapter.VendorViewHolder>() {

    inner class VendorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vendorImage: ImageView = itemView.findViewById(R.id.vendorImage)
        val vendorName: TextView = itemView.findViewById(R.id.vendorName)
        val vendorCategory: TextView = itemView.findViewById(R.id.vendorCategory)
        val vendorLocality: TextView = itemView.findViewById(R.id.vendorLocality)
        val vendorStatus: TextView = itemView.findViewById(R.id.vendorStatus)
        val vendorRating: TextView = itemView.findViewById(R.id.vendorRating)
        val vendorDesc: TextView = itemView.findViewById(R.id.vendorDesc)
        val vendorDistance: TextView = itemView.findViewById(R.id.vendorDistance)
        val vendorPriceRange: TextView = itemView.findViewById(R.id.vendorPriceRange)
        val specialtiesContainer: LinearLayout = itemView.findViewById(R.id.specialtiesContainer)
        val specialty1: TextView = itemView.findViewById(R.id.specialty1)
        val specialty2: TextView = itemView.findViewById(R.id.specialty2)
        val specialty3: TextView = itemView.findViewById(R.id.specialty3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VendorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vendor, parent, false)
        return VendorViewHolder(view)
    }

    override fun onBindViewHolder(holder: VendorViewHolder, position: Int) {
        val vendor = vendors[position]
        
        // Basic info
        holder.vendorName.text = vendor.name
        holder.vendorDesc.text = vendor.description
        holder.vendorCategory.text = vendor.category
        holder.vendorLocality.text = vendor.locality.ifEmpty { "Unknown Location" }
        holder.vendorPriceRange.text = vendor.priceRange
        
        // Rating
        holder.vendorRating.text = if (vendor.rating > 0) String.format("%.1f ★", vendor.rating) else "N/A"
        
        // Distance
        holder.vendorDistance.text = if (vendor.distance > 0) {
            if (vendor.distance < 1) {
                String.format("%.0f m away", vendor.distance * 1000)
            } else {
                String.format("%.1f km away", vendor.distance)
            }
        } else "Distance unknown"
        
        // Status
        if (vendor.isOpen) {
            holder.vendorStatus.text = "OPEN"
            holder.vendorStatus.setTextColor(holder.itemView.context.getColor(R.color.success_green))
            holder.vendorStatus.setBackgroundResource(R.drawable.status_open_background)
        } else {
            holder.vendorStatus.text = "CLOSED"
            holder.vendorStatus.setTextColor(holder.itemView.context.getColor(R.color.text_secondary))
            holder.vendorStatus.setBackgroundResource(R.drawable.specialty_chip_background)
        }
        
        // Specialties
        setupSpecialties(holder, vendor.specialties)
        
        // Add animation
        animateCard(holder.itemView, position)
        
        // Click listener with animation
        holder.itemView.setOnClickListener {
            // Add subtle scale animation
            val scaleX = ObjectAnimator.ofFloat(holder.itemView, "scaleX", 1.0f, 0.95f, 1.0f)
            val scaleY = ObjectAnimator.ofFloat(holder.itemView, "scaleY", 1.0f, 0.95f, 1.0f)
            scaleX.duration = 150
            scaleY.duration = 150
            scaleX.start()
            scaleY.start()
            
            onItemClick(vendor)
        }
    }

    private fun animateCard(view: View, position: Int) {
        // Reset any previous animations
        view.clearAnimation()
        
        // Set initial state for animation
        view.alpha = 0f
        view.translationY = 50f
        
        // Animate in with stagger effect
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay(position * 50L) // Stagger animation
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun setupSpecialties(holder: VendorViewHolder, specialties: List<String>) {
        val specialtyViews = listOf(holder.specialty1, holder.specialty2, holder.specialty3)
        
        // Hide all specialty views first
        specialtyViews.forEach { it.visibility = View.GONE }
        
        // Show specialties up to 3
        specialties.take(3).forEachIndexed { index, specialty ->
            if (index < specialtyViews.size) {
                specialtyViews[index].text = specialty
                specialtyViews[index].visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int = vendors.size

    // ✅ Allows search to refresh list
    fun updateList(newList: List<Vendor>) {
        vendors = newList
        notifyDataSetChanged()
    }
}
