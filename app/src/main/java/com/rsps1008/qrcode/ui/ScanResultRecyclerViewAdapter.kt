package com.rsps1008.qrcode.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.rsps1008.qrcode.R
import com.rsps1008.qrcode.databinding.FragmentItemBinding
import com.rsps1008.qrcode.ui.database.ScanResult
import com.rsps1008.qrcode.ui.database.TYPE

import java.text.SimpleDateFormat
import java.util.*

class ScanResultRecyclerViewAdapter(
    private val results: MutableList<ScanResult>,
    private val onDelete: (ScanResult) -> Unit,
    private val onToggleFavorite: (ScanResult) -> Unit,
    private val onRequestEditTitle: (ScanResult) -> Unit
) : RecyclerView.Adapter<ScanResultRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            FragmentItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = results[position]
        val imgId = when(item.type) {
            TYPE.SMS -> R.drawable.baseline_sms
            TYPE.TEXT -> R.drawable.baseline_abc
            TYPE.REDIRECT -> R.drawable.baseline_insert_link
            TYPE.WIFI -> R.drawable.baseline_wifi
            TYPE.PHONE -> R.drawable.baseline_phone
            TYPE.EMAIL -> R.drawable.baseline_email
        }
        holder.typeView.setImageResource(imgId)
        holder.favoriteView.setImageResource(
            if (item.isFavorite) R.drawable.ic_star else R.drawable.ic_star_border
        )
        holder.favoriteView.contentDescription = holder.itemView.context.getString(
            if (item.isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites
        )
        holder.editTitleView.visibility = if (item.type == TYPE.REDIRECT) View.VISIBLE else View.GONE
        holder.timeStampView.text =
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.TAIWAN).format(item.timestamp)
        if (item.title.isNullOrBlank()) {
            holder.titleView.visibility = View.GONE
            holder.contentView.text = item.content
        } else {
            holder.titleView.visibility = View.VISIBLE
            holder.titleView.text = item.title
            holder.contentView.text = item.content
        }
    }

    override fun getItemCount(): Int = results.size

    inner class ViewHolder(binding: FragmentItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnLongClickListener {
        val typeView: ImageView = binding.type
        val titleView: TextView = binding.title
        val editTitleView: ImageView = binding.editTitle
        val favoriteView: ImageView = binding.favorite
        val timeStampView: TextView = binding.timestamp
        val contentView: TextView = binding.content

        init {
            favoriteView.setOnClickListener {
                onToggleFavorite(results[bindingAdapterPosition])
            }
            editTitleView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRequestEditTitle(results[position])
                }
            }
            // 新增點擊事件判斷是否為網址
            itemView.setOnClickListener {
                val content = contentView.text.toString()
                if (android.util.Patterns.WEB_URL.matcher(content).matches()) {
                    val url = if (!content.startsWith("http://") && !content.startsWith("https://"))
                        "http://$content" else content
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    itemView.context.startActivity(intent)
                }
            }
            // 原本的長按複製功能
            itemView.setOnLongClickListener(this)
        }

        fun remove() {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val result = results.removeAt(position)
                notifyItemRemoved(position)
                onDelete(result)
            }
        }

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }

        override fun onLongClick(p0: View?): Boolean {
            val content = contentView.text.toString()
            val clipboardManager =
                p0?.context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("simple text", content)
            clipboardManager.setPrimaryClip(clip)
            Toast.makeText(
                p0.context,
                String.format(p0.context.getString(com.rsps1008.qrcode.R.string.copy_already), content),
                Toast.LENGTH_SHORT
            ).show()
            return true
        }
    }


}
