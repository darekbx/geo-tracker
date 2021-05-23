package com.darekbx.geotracker.ui.calendar

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.darekbx.geotracker.R
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.viewmodels.TrackViewModel
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_activity_calendar.*
import java.time.DayOfWeek
import java.time.YearMonth
import java.util.Calendar

class DayViewContainer(view: View) : ViewContainer(view) {
    val dayTextView = view.findViewById<TextView>(R.id.calendarDayText)
    val kilometersTextView = view.findViewById<TextView>(R.id.calendarDayKilometersText)
}

class MonthHeaderContainer(view: View) : ViewContainer(view) {
    val textView = view.findViewById<TextView>(R.id.calendarMonthHeaderText)
}

@AndroidEntryPoint
class ActivityCalendarFragment : Fragment(R.layout.fragment_activity_calendar) {

    private val tracksViewModel: TrackViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tracksViewModel.fetchTracks()
        tracksViewModel.tracks.observe(viewLifecycleOwner, Observer { yearTracks ->
            val tracks = yearTracks.flatMap { it.value }
            createCalendar(tracks)
        })
    }

    private fun createCalendar(tracks: List<Track>) {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)

        calendar_view.dayBinder = dayBinder(dayOfYear, year, tracks)
        calendar_view.monthHeaderBinder = monthHeaderBinder()

        val currentMonth = YearMonth.now()
        val firstMonth = currentMonth.minusMonths(10)
        val lastMonth = currentMonth.plusMonths(1)
        calendar_view.setup(firstMonth, lastMonth, DayOfWeek.MONDAY)
        calendar_view.scrollToMonth(currentMonth)
    }

    private fun dayBinder(
        dayOfYear: Int,
        year: Int,
        tracks: List<Track>
    ) = object : DayBinder<DayViewContainer> {

        override fun create(view: View) = DayViewContainer(view)

        override fun bind(container: DayViewContainer, day: CalendarDay) {
            container.dayTextView.text = day.date.dayOfMonth.toString()
            styleDay(day, container)
        }

        private fun styleDay(
            day: CalendarDay,
            container: DayViewContainer
        ) {
            if (day.owner == DayOwner.THIS_MONTH) {
                markCurrentDay(day, container)
                markOnBikeDay(day, container)
                container.dayTextView.setTextColor(Color.BLACK)
            } else {
                container.dayTextView.setTextColor(Color.LTGRAY)
            }
        }

        private fun markOnBikeDay(
            day: CalendarDay,
            container: DayViewContainer
        ) {
            val paddedMonth = day.date.monthValue.toString().padStart(2, '0')
            val paddedDay = day.date.dayOfMonth.toString().padStart(2, '0')
            val dateFormatted = "${day.date.year}-${paddedMonth}-${paddedDay}"

            // TODO move to view model, sun ascync, without group by year
            // TODO in month headere add sum distance, also from viewmodel
            val sumDistance = tracks
                .asSequence()
                .filter { it.startTimestamp?.startsWith(dateFormatted) ?: false }
                .sumByDouble { it.distance.toDouble() }

            if (sumDistance > 0) {
                container.dayTextView.setBackgroundColor(Color.argb(60, 40, 220, 80))
                container.kilometersTextView.setText("%.2fkm".format(sumDistance))
            }
        }

        private fun markCurrentDay(
            day: CalendarDay,
            container: DayViewContainer
        ) {
            if (day.date.dayOfYear == dayOfYear && day.date.year == year) {
                container.dayTextView.typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    private fun monthHeaderBinder() = object : MonthHeaderFooterBinder<MonthHeaderContainer> {

        override fun bind(container: MonthHeaderContainer, month: CalendarMonth) {
            container.textView.text = month.yearMonth.month.name
        }

        override fun create(view: View) = MonthHeaderContainer(view)
    }
}
