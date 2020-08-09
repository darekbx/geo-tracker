package com.darekbx.geotracker.ui.tracks

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.darekbx.geotracker.databinding.AdapterTrackBinding
import com.darekbx.geotracker.model.Track

class TrackAdapter(val context: Context?)
    : RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

    var items = listOf<Track>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val binding = AdapterTrackBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val part = items.get(position)
        viewHolder.bind(part)
    }

    val inflater by lazy { LayoutInflater.from(context) }

    class ViewHolder(val binding: AdapterTrackBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(track: Track) {
            binding.track = track
            binding.executePendingBindings()
        }
    }
}