package com.rsps1008.qrcode.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rsps1008.qrcode.R


/**
 * A fragment representing a list of Items.
 */
class ScanResultFragment : Fragment() {

    private var columnCount = 1
    private val viewModel: ScanResultViewModel by viewModels()
    private var showFavoritesOnly = false
    private lateinit var historyAdapter: ScanResultRecyclerViewAdapter
    val isShowingFavorites: Boolean
        get() = showFavoritesOnly

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showFavoritesOnly = arguments?.getBoolean(ARG_SHOW_FAVORITES, false) == true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                viewModel.resultData.observe(viewLifecycleOwner) {
                    historyAdapter = ScanResultRecyclerViewAdapter(
                        it.toMutableList(),
                        onDelete = { result -> viewModel.deleteResult(requireContext(), result) },
                        onToggleFavorite = { result -> viewModel.toggleFavorite(requireContext(), result) }
                    )
                    this.adapter = historyAdapter
                }
                ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                    0,
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                ) {
                    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(198, 40, 40)
                    }
                    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        textSize = 14f * resources.displayMetrics.scaledDensity
                    }

                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ) = false

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        (viewHolder as? ScanResultRecyclerViewAdapter.ViewHolder)?.remove()
                    }

                    override fun onChildDraw(
                        canvas: Canvas,
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        dX: Float,
                        dY: Float,
                        actionState: Int,
                        isCurrentlyActive: Boolean
                    ) {
                        val itemView = viewHolder.itemView
                        if (dX != 0f) {
                            val left = if (dX > 0) itemView.left.toFloat() else itemView.right + dX
                            val right = if (dX > 0) itemView.left + dX else itemView.right.toFloat()
                            canvas.drawRect(left, itemView.top.toFloat(), right, itemView.bottom.toFloat(), backgroundPaint)
                            val label = getString(R.string.delete_history_item)
                            val labelWidth = labelPaint.measureText(label)
                            val labelX = if (dX > 0) {
                                itemView.left + 24f
                            } else {
                                itemView.right - labelWidth - 24f
                            }
                            val labelY = itemView.top + itemView.height / 2f - (labelPaint.ascent() + labelPaint.descent()) / 2f
                            canvas.drawText(label, labelX, labelY, labelPaint)
                        }
                        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    }
                }).attachToRecyclerView(this)
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setShowFavoritesOnly(
            view.context.applicationContext,
            showFavoritesOnly
        )
    }

    companion object {
        private const val ARG_SHOW_FAVORITES = "show_favorites"

        fun newInstance(showFavoritesOnly: Boolean = false) = ScanResultFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_SHOW_FAVORITES, showFavoritesOnly)
            }
        }
    }
}
