
package com.example.spritesheets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import com.example.spritesheets.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import okhttp3.Request
import okhttp3.Response
import kotlinx.coroutines.*

class MainActivity : FragmentActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var exoplayer: ExoPlayer
    private  var thumbnailList: List<ThumbnailMetadata>? = null
    private var videoUrl: String? = "https://bbcontent.nayatel.com/content/tour_guide_video_clips/multi_linguistic.mp4"

    private val executorService: ExecutorService = Executors.newSingleThreadExecutor() // Executor for async tasks
    private val thumbnailCache = mutableMapOf<String, Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

       // thumbnailList = parseThumbnailMetadataFromJson(this)
        fetchJsonFromUrl("https://bbcontent.nayatel.com/content/tour_guide_video_clips/frames_metadataa.json")
        Log.d("Thumbnail", "Parsed metadata: $thumbnailList")

        exoplayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoplayer
        exoplayer.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
        exoplayer.prepare()
        exoplayer.play()

        setupScrubListener()
    }

    //Yeh Agr Ham Locally Rakhein URL sy na lein to is trh fr JSOn Parse hgi

//    private fun parseThumbnailMetadataFromJson(context: Context): List<ThumbnailMetadata> {
//        val jsonString = context.assets.open("frames_metadata.json").bufferedReader().use { it.readText() }
//        Log.d("Thumbnail", "Read JSON: $jsonString")
//        return Gson().fromJson(jsonString, object : TypeToken<List<ThumbnailMetadata>>() {}.type)
//    }

    @OptIn(UnstableApi::class)
    private fun setupScrubListener() {
        binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
            .addListener(object : TimeBar.OnScrubListener {
                private var lastScrubbedPosition: Long = -1
                override fun onScrubStart(timeBar: TimeBar, position: Long) {
                    exoplayer.pause()
                    Log.d("Thumbnail", "Scrub started at position: $position")
                    fetchAndDisplayThumbnail(position)
                    updateThumbnailPosition(position)

                }

                override fun onScrubMove(timeBar: TimeBar, position: Long) {
                    if (position != lastScrubbedPosition) {
                        lastScrubbedPosition = position
                        executorService.submit {
                            fetchAndDisplayThumbnail(position)
                        }
                    }
                    updateThumbnailPosition(position)
                }

                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                    Log.d("Thumbnail", "Scrub stopped at position: $position")
                    executorService.submit {
                        fetchAndDisplayThumbnail(position)
                    }
                    updateThumbnailPosition(position)
                 //   exoplayer.play()
                }
            })
    }
//Yeh Decider Function hai agr cache sy mil gya to if sy e return hjai ga
    //nahi to fetchThumbnailFromUrl() yeh call krna paray ga
    private fun fetchAndDisplayThumbnail(position: Long) {
        val nearestThumbnail = getNearestThumbnail(position)
        Log.d("Thumbnail", "fetchAndDisplayThumbnail at the nearrest: $nearestThumbnail")
        if (nearestThumbnail != null) {
            val cachedBitmap = thumbnailCache[nearestThumbnail.mappedThumbnailImage]
            if (cachedBitmap != null) {
                Log.d("Thumbnail", "Found cached thumbnail.")
                runOnUiThread {
                    binding.previewImage.setImageBitmap(cachedBitmap)
                    binding.previewImage.visibility = View.VISIBLE
                }
            } else {
                Log.d("Thumbnail", "Fetching FROM URI")
                fetchThumbnailFromUrl(nearestThumbnail.mappedThumbnailImage)
            }
        }
    }

    private fun getNearestThumbnail(position: Long): ThumbnailMetadata? {
        Log.d("RequiredPosition", "getNearestThumbnail: $position")
        return thumbnailList?.minByOrNull { Math.abs(it.timestamp - position) }
    //return thumbnailList.find { it.timestamp == position }
//        val closestThumbnail = thumbnailList.minByOrNull {
//            Math.abs(it.timestamp - position) // Find the closest timestamp
//        }
//        Log.d("Thumbnail", "Nearest thumbnail: $closestThumbnail")
//        return closestThumbnail
    }

    //Fetching the Updated Thumbnail
    private fun fetchThumbnailFromUrl(imageUrl: String) {
        executorService.submit {
            try {
                // Fetch the thumbnail from the network
                val bitmap = downloadImage(imageUrl)
                if (bitmap != null) {
                    // Cache the thumbnail locally
                    Log.d("Thumbnail", "downloadImage and  cached thumbnail.")
                    thumbnailCache[imageUrl] = bitmap
                    runOnUiThread {
                        binding.previewImage.setImageBitmap(bitmap)
                        binding.previewImage.visibility = View.VISIBLE

                    }
                } else {
                    Log.e("Thumbnail", "Failed to load image: $imageUrl")
                }
            } catch (e: Exception) {
                Log.e("Thumbnail", "Error fetching thumbnail: $e")
            }
        }
    }
//Agr Cache nh hwa to yeh function tb call hga
    private fun downloadImage(url: String): Bitmap? {
        return try {
            val inputStream = URL(url).openStream()
            Log.e("Thumbnail", "downloading image: $inputStream")
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e("Thumbnail", "Error downloading image: $url", e)
            null
        }
    }

    //yeh function hamei jo thumbnail Preview ki Position hai jo Seek k sath sath chal rhi usmy HelpOut krrhi
        private fun updateThumbnailPosition(position: Long) {
        val timeBar = binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
        val timeBarWidth = timeBar.width
        val imageWidth = binding.previewImage.width
        if (exoplayer.duration <= 0) {
            Log.e("SpriteSheet", "Invalid player duration: ${exoplayer.duration}")
            return
        }
        val newPos = (position.toFloat() / exoplayer.duration) * timeBarWidth - (imageWidth / 2)
        val clampedPos = newPos.coerceIn(0f, timeBarWidth - imageWidth.toFloat())
        Log.d("SpriteSheet", "Calculated thumbnail position: $clampedPos")
        binding.previewImage.x = clampedPos
        binding.previewImage.visibility = View.VISIBLE

    }
    override fun onDestroy() {
        super.onDestroy()
        // Clear the cache when the activity is destroyed
        //Issy Memory effectivelty use hgi
        thumbnailCache.clear()
        Log.d("Thumbnail", "Cache cleared")
    }

    private fun fetchJsonFromUrl(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()

            try {
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    Log.d("Thumbnail", "Fetched JSON: $jsonResponse")

                    // Pass the parsed list back to the main Q k Backgorund py srf fetching ho skhti hai
                    //Setup Main Thread py hga oherwise exception ayegi leading to crash
                    withContext(Dispatchers.Main) {
                        thumbnailList = Gson().fromJson(jsonResponse, object : TypeToken<List<ThumbnailMetadata>>() {}.type)
                    }
                } else {


                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
