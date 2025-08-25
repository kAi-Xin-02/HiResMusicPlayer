package com.musicplayer.hiresplayer

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    
    private var mediaPlayer: MediaPlayer? = null
    private var songs = ArrayList<Song>()
    private var currentSongIndex = 0
    private var isPlaying = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var songAdapter: SongAdapter
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupUI()
        checkPermissions()
    }
    
    private fun setupUI() {
        // Setup RecyclerView for songs
        songAdapter = SongAdapter(songs) { song, position ->
            playSong(position)
        }
        
        recyclerViewSongs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = songAdapter
        }
        
        // Setup controls
        buttonPlayPause.setOnClickListener { togglePlayPause() }
        buttonPrevious.setOnClickListener { previousSong() }
        buttonNext.setOnClickListener { nextSong() }
        
        // Setup seek bar
        seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Volume control
        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val volume = progress / 100f
                mediaPlayer?.setVolume(volume, volume)
                textVolume.text = "${progress}%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(
                this, 
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 
                PERMISSION_REQUEST_CODE
            )
        } else {
            loadSongs()
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongs()
            }
        }
    }
    
    private fun loadSongs() {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        )
        
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Audio.Media.TITLE + " ASC"
        )
        
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            
            songs.clear()
            
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val path = it.getString(pathColumn)
                val duration = it.getLong(durationColumn)
                val size = it.getLong(sizeColumn)
                
                // Only add high-quality files (optional filter)
                if (File(path).exists()) {
                    songs.add(Song(id, title, artist, path, duration, size))
                }
            }
        }
        
        songAdapter.notifyDataSetChanged()
        
        if (songs.isNotEmpty()) {
            updateSongInfo(0)
        }
    }
    
    private fun playSong(index: Int) {
        if (index < 0 || index >= songs.size) return
        
        currentSongIndex = index
        val song = songs[index]
        
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(song.path)
                prepareAsync()
                
                setOnPreparedListener {
                    start()
                    isPlaying = true
                    updatePlayPauseButton()
                    updateProgress()
                    
                    seekBarProgress.max = duration
                }
                
                setOnCompletionListener {
                    nextSong()
                }
                
                setOnErrorListener { _, what, extra ->
                    // Handle high-quality file playback errors
                    false
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        updateSongInfo(index)
        songAdapter.updateCurrentSong(index)
    }
    
    private fun togglePlayPause() {
        mediaPlayer?.let { mp ->
            if (isPlaying) {
                mp.pause()
                isPlaying = false
            } else {
                mp.start()
                isPlaying = true
                updateProgress()
            }
            updatePlayPauseButton()
        } ?: run {
            if (songs.isNotEmpty()) {
                playSong(currentSongIndex)
            }
        }
    }
    
    private fun previousSong() {
        val newIndex = if (currentSongIndex > 0) currentSongIndex - 1 else songs.size - 1
        playSong(newIndex)
    }
    
    private fun nextSong() {
        val newIndex = if (currentSongIndex < songs.size - 1) currentSongIndex + 1 else 0
        playSong(newIndex)
    }
    
    private fun updatePlayPauseButton() {
        buttonPlayPause.text = if (isPlaying) "⏸️" else "▶️"
    }
    
    private fun updateSongInfo(index: Int) {
        if (index < songs.size) {
            val song = songs[index]
            textSongTitle.text = song.title
            textArtist.text = song.artist
            
            // Display audio quality based on file size and duration
            val quality = getAudioQuality(song)
            textQuality.text = quality
            
            textDuration.text = formatTime(song.duration)
        }
    }
    
    private fun getAudioQuality(song: Song): String {
        val fileSizeKB = song.size / 1024
        val durationSeconds = song.duration / 1000
        val bitrateKbps = if (durationSeconds > 0) (fileSizeKB * 8) / durationSeconds else 0
        
        return when {
            bitrateKbps > 1000 -> "Hi-Res Lossless"
            bitrateKbps > 500 -> "Hi-Res Audio"
            bitrateKbps > 256 -> "High Quality"
            else -> "Standard Quality"
        }
    }
    
    private fun updateProgress() {
        mediaPlayer?.let { mp ->
            if (isPlaying) {
                seekBarProgress.progress = mp.currentPosition
                textCurrentTime.text = formatTime(mp.currentPosition.toLong())
                
                handler.postDelayed({ updateProgress() }, 100)
            }
        }
    }
    
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
    }
}
