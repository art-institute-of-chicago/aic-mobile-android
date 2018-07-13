package edu.artic.welcome

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import edu.artic.db.models.ArticTour

/**
 * @author Sameer Dhakal (Fuzz)
 */

class ToursAdapter(val tours: List<ArticTour>, private val context: Context) : RecyclerView.Adapter<ToursViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToursViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.tour_card_layout, parent, false)
        return ToursViewHolder(view)
    }

    override fun getItemCount(): Int {
        return tours.count()
    }

    override fun onBindViewHolder(holder: ToursViewHolder, position: Int) {
        val tour = tours[position]
        holder.tourTitle.text = tour.title
        holder.tourDescription.text = tour.description
        val count = tour.tourStops?.count() ?: 0
        holder.tourStopsCount.text = count.toString()
        holder.tourTime.text = tour.tourDuration

        Glide.with(context)
                .load(tour.imageUrl)
                .into(holder.tourImage)
    }

}


class ToursViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tourImage: ImageView = itemView.findViewById(R.id.image)
    val tourTitle: TextView = itemView.findViewById(R.id.tourTitle)
    val tourDescription: TextView = itemView.findViewById(R.id.tourDescription)
    val tourStopsCount: TextView = itemView.findViewById(R.id.stops)
    val tourTime: TextView = itemView.findViewById(R.id.tourTime)
}