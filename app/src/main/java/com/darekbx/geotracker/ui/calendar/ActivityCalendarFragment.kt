package com.darekbx.geotracker.ui.calendar

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.darekbx.geotracker.R
import com.darekbx.geotracker.model.DaySummary
import com.darekbx.geotracker.viewmodels.TrackViewModel
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.DayOfWeek
import java.time.YearMonth
import java.util.Calendar
import android.text.style.UnderlineSpan
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.ViewGroup
import com.darekbx.geotracker.databinding.FragmentActivityCalendarBinding

class DayViewContainer(view: View) : ViewContainer(view) {
    val dayTextView: TextView = view.findViewById(R.id.calendarDayText)
    val kilometersTextView: TextView = view.findViewById(R.id.calendarDayKilometersText)
}

class MonthHeaderContainer(view: View) : ViewContainer(view) {
    val textView: TextView = view.findViewById(R.id.calendarMonthHeaderText)
}

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ActivityCalendarFragment : Fragment(R.layout.fragment_activity_calendar) {

    private var _binding: FragmentActivityCalendarBinding? = null
    private val binding get() = _binding!!

    private val tracksViewModel: TrackViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loadingView.visibility = View.VISIBLE

        tracksViewModel.fetchDaySummaries()
        tracksViewModel.daySummaries.observe(viewLifecycleOwner) { daySummaries ->
            createCalendar(daySummaries)
            binding.loadingView.visibility = View.GONE
        }
    }

    private fun createCalendar(daySummaries: List<DaySummary>) {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)

        binding.calendarView.dayBinder = dayBinder(dayOfYear, year, daySummaries)
        binding.calendarView.monthHeaderBinder = monthHeaderBinder(daySummaries)

        val currentMonth = YearMonth.now()
        val firstMonth = currentMonth.minusMonths(10)
        val lastMonth = currentMonth.plusMonths(1)
        binding.calendarView.setup(firstMonth, lastMonth, DayOfWeek.MONDAY)
        binding.calendarView.scrollToMonth(currentMonth)

        val daysOnBike = daySummaries.filter { it.dateFormatted.take(4).toInt() == year }.size
        binding.yearHeader.text = getString(R.string.calendar_year_header, daysOnBike)
    }

    private fun dayBinder(
        dayOfYear: Int,
        year: Int,
        daySummaries: List<DaySummary>
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
                container.dayTextView.setTextColor(Color.WHITE)
                markCurrentDay(day, container)
                markOnBikeDay(day, container)
            } else {
                container.dayTextView.setTextColor(Color.DKGRAY)
                reset(container)
            }
        }

        private fun reset(container: DayViewContainer) {
            container.dayTextView.setBackgroundColor(Color.BLACK)
            container.kilometersTextView.text = ""
        }

        @SuppressLint("SetTextI18n")
        private fun markOnBikeDay(
            day: CalendarDay,
            container: DayViewContainer
        ) {
            val paddedMonth = day.date.monthValue.toString().padStart(2, '0')
            val paddedDay = day.date.dayOfMonth.toString().padStart(2, '0')
            val dateFormatted = "${day.date.year}-${paddedMonth}-${paddedDay}"
            val daySummary = daySummaries.firstOrNull { it.dateFormatted == dateFormatted }

            if (daySummary != null && daySummary.sumDistance > 0) {
                val color = obtainColor(daySummary.sumDistance)
                container.dayTextView.setBackgroundColor(color)
                container.kilometersTextView.text = "%.2fkm".format(daySummary.sumDistance)
            } else {
                reset(container)
            }
        }

        private fun obtainColor(sumDistance: Double) = when {
            sumDistance > 0 && sumDistance <= 10 -> Color.argb(60, 40, 220, 80)
            sumDistance > 10 && sumDistance <= 20 -> Color.argb(100, 40, 220, 80)
            sumDistance > 20 && sumDistance <= 30 -> Color.argb(140, 40, 220, 80)
            else -> Color.argb(180, 40, 220, 80)
        }

        private fun markCurrentDay(
            day: CalendarDay,
            container: DayViewContainer
        ) {
            if (day.date.dayOfYear == dayOfYear && day.date.year == year) {
                container.dayTextView.typeface = Typeface.DEFAULT_BOLD

                val content = SpannableString(container.dayTextView.text)
                content.setSpan(UnderlineSpan(), 0, content.length, 0)
                container.dayTextView.text = content
            }
        }
    }

    private fun monthHeaderBinder(daySummaries: List<DaySummary>) =
        object : MonthHeaderFooterBinder<MonthHeaderContainer> {

            override fun create(view: View) = MonthHeaderContainer(view)

            @SuppressLint("SetTextI18n")
            override fun bind(container: MonthHeaderContainer, month: CalendarMonth) {
                val monthName = month.yearMonth.month.name
                val paddedMonth = month.month.toString().padStart(2, '0')
                val yearMonth = "${month.year}-${paddedMonth}"
                val monthSummaries = daySummaries.filter { it.dateFormatted.take(7) == yearMonth }
                val monthSummariesCount = monthSummaries.size

                if (monthSummariesCount > 0) {
                    val monthDistance = monthSummaries.sumOf { it.sumDistance }
                    container.textView.text = getString(
                        R.string.calendar_month_format,
                        monthName,
                        monthDistance,
                        monthSummariesCount
                    )
                } else {
                    container.textView.text = monthName
                }
            }
        }
}
