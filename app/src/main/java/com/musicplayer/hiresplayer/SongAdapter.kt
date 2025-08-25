package com.musicplayer.hiresplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_song.view.*
import androidx.core.content.ContextCompat

class SongAdapter(
    private val songs: List<Song>,
    private val onSongClick: (Song, Int) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {
    
    private var currentPlayingIndex = -1
    
    fun updateCurrentSong(index: Int) {
        val oldIndex = currentPlayingIndex
        currentPlayingIndex = index
        notifyItemChanged(oldIndex)
        notifyItemChanged(currentPlayingIndex)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position], position == currentPlayingIndex)
    }
    
    override fun getItemCount() = songs.size
    
    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        fun bind(song: Song, isPlaying: Boolean) {
            itemView.apply {
                textSongTitle.text = song.title
                textSongArtist.text = song.artist
                textSongDuration.text = formatTime(song.duration)
                
                // Highlight currently playing song
                setBackgroundColor(
                    if (isPlaying) 
                        ContextCompat.getColor(context, R.color.colorAccent) 
                    else 
                        ContextCompat.getColor(context, android.R.color.transparent)
                )
                
                setOnClickListener {
                    onSongClick(song, adapterPosition)
                }
            }
        }
        
        private fun formatTime(timeMs: Long): String {
            val totalSeconds = timeMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }
    }
}
