package com.darekbx.geotracker.ui.tracks

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import androidx.databinding.BindingAdapter
import com.darekbx.geotracker.databinding.AdapterTrackBinding
import com.darekbx.geotracker.databinding.AdapterYearSummaryBinding
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.model.YearSummary

@BindingAdapter("isBroken")
fun View.isBroken(isBroken: Boolean) {
    if (isBroken) {
        setBackgroundColor(Color.parseColor("#e53935"))
    }
}

class TrackAdapter(val context: Context?)
    : BaseExpandableListAdapter() {

    var onTrackClick: ((Track) -> Unit)? = null
    var onTrackLongClick: ((Track) -> Unit)? = null

    var items = mapOf<String?, List<Track>>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getGroupCount() = items.size

    override fun getChildrenCount(groupPosition: Int): Int {
        if (items.keys.isEmpty()) return 0
        val key = items.keys.elementAt(groupPosition)
        return items[key]?.size ?: 0
    }

    override fun getGroup(groupPosition: Int): List<Track>? {
        val key = items.keys.elementAt(groupPosition)
        return items[key]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Track? {
        val group = getGroup(groupPosition)
        return group?.get(childPosition)
    }

    override fun getGroupId(groupPosition: Int) = -1L

    override fun getChildId(groupPosition: Int, childPosition: Int) = -1L

    override fun hasStableIds() = false

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val binding = AdapterYearSummaryBinding.inflate(inflater, parent, false)
        getGroup(groupPosition)?.let { tracks ->
            var totalDistance = 0.0F
            for (track in tracks) {
                totalDistance += track.distance
            }
            val key = items.keys.elementAt(groupPosition)
            val yearSummary = YearSummary(
                key ?: "[Unknown]",
                tracks.size,
                totalDistance
            )
            binding.yearSummary = yearSummary
            binding.executePendingBindings()
        }
        return binding.root
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val binding = AdapterTrackBinding.inflate(inflater, parent, false)
        getChild(groupPosition, childPosition)?.let { track ->
            binding.track = track
            binding.rowContainer.setOnClickListener {
                onTrackClick?.run { this(track) }
            }
            binding.rowContainer.setOnLongClickListener {
                onTrackLongClick?.run { this(track) }
                true
            }
            binding.executePendingBindings()
        }
        return binding.root
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int) = false

    private val inflater: LayoutInflater by lazy { LayoutInflater.from(context) }
}
