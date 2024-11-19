//////Loading thumbnail working fine locally
//package com.example.spritesheets
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.net.Uri
//import android.os.Bundle
//import android.os.AsyncTask
//import android.util.Log
//import android.view.View
//import androidx.fragment.app.FragmentActivity
//import androidx.media3.common.MediaItem
//import androidx.media3.common.util.UnstableApi
//import androidx.media3.exoplayer.ExoPlayer
//import androidx.media3.ui.DefaultTimeBar
//import androidx.media3.ui.TimeBar
//import com.example.spritesheets.databinding.ActivityMainBinding
//import com.google.gson.Gson
//import com.google.gson.reflect.TypeToken
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//
//class MainActivity : FragmentActivity() {
//    private lateinit var binding: ActivityMainBinding
//    private lateinit var exoplayer: ExoPlayer
//    private lateinit var metadataList: List<SpriteSheetMetadata>
//    private var videoUrl: String? = "https://bbcontent.nayatel.com/content/tour_guide_video_clips/Subtitles_Audio.mp4"
//
//    private val executorService: ExecutorService = Executors.newSingleThreadExecutor() // Executor for async tasks
//    private val spriteSheetCache = mutableMapOf<String, Bitmap>() // Cache for sprite sheets
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Load and parse the sprite sheet metadata
//        metadataList = parseMetadataFromJson(this)
//        Log.d("SpriteSheet", "Parsed metadata: $metadataList")
//
//        // Initialize the ExoPlayer and set the media item
//        exoplayer = ExoPlayer.Builder(this).build()
//        binding.playerView.player = exoplayer
//        exoplayer.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
//        exoplayer.prepare()
//        exoplayer.play()
//
//        // Set up the scrub listener
//        setupScrubListener()
//    }
//
//    @OptIn(UnstableApi::class)
//    private fun setupScrubListener() {
//        binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
//            .addListener(object : TimeBar.OnScrubListener {
//                private var lastScrubbedPosition: Long = -1
//
//                override fun onScrubStart(timeBar: TimeBar, position: Long) {
//                    exoplayer.pause()
//                    Log.d("SpriteSheet", "Scrub started at position: $position")
//                    // Move frame extraction to background
//                    fetchAndDisplayFrame(position)
//                }
//
//                override fun onScrubMove(timeBar: TimeBar, position: Long) {
//                    // Only update if position has changed significantly
//                    if (position != lastScrubbedPosition) {
//                        lastScrubbedPosition = position
//                        // Asynchronous background task
//                        executorService.submit {
//                            fetchAndDisplayFrame(position)
//                        }
//                    }
//                }
//
//                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
//                    Log.d("SpriteSheet", "Scrub stopped at position: $position")
//                    // Fetch latest frame after scrub stops
//                    executorService.submit {
//                        fetchAndDisplayFrame(position)
//                    }
//                }
//            })
//    }
//
//    private fun parseMetadataFromJson(context: Context): List<SpriteSheetMetadata> {
//        val jsonString = context.assets.open("sprites_metadata_short.json").bufferedReader().use { it.readText() }
//        Log.d("SpriteSheet", "Read JSON: $jsonString")
//        return Gson().fromJson(jsonString, object : TypeToken<List<SpriteSheetMetadata>>() {}.type)
//    }
//
//    private fun fetchAndDisplayFrame(position: Long) {
//        val frame = extractFrameAtPosition(position / 1000, metadataList, this)
//        if (frame != null) {
//            Log.d("SpriteSheet", "Frame extracted successfully")
//            runOnUiThread {
//                binding.previewImage.setImageBitmap(frame)
//                updateThumbnailPosition(position)
//            }
//        } else {
//            Log.e("SpriteSheet", "Failed to extract frame at position: $position")
//            runOnUiThread {
//                binding.previewImage.visibility = View.GONE
//            }
//        }
//    }
//
//    private fun extractFrameAtPosition(
//        position: Long,
//        metadataList: List<SpriteSheetMetadata>,
//        context: Context
//    ): Bitmap? {
//        val metadata = metadataList.find { position in it.start_time..it.end_time } ?: return null.also {
//            Log.e("SpriteSheet", "No matching metadata found for position: $position")
//        }
//
//        // Load the sprite sheet from cache or disk if necessary
//        val spriteSheet = spriteSheetCache[metadata.sprite_sheet_name]
//            ?: loadSpriteSheet(context, metadata.sprite_sheet_name)
//
//        spriteSheet ?: return null
//
//        // Calculate the position of the frame in the grid
//        val timeInSheet = (position - metadata.start_time).toInt()
//        val frameIndex = timeInSheet / metadata.frame_interval
//        val x = (frameIndex % metadata.frames_per_row) * metadata.frame_width
//        val y = (frameIndex / metadata.frames_per_row) * metadata.frame_height
//
//        if (x < 0 || y < 0 || x + metadata.frame_width > spriteSheet.width || y + metadata.frame_height > spriteSheet.height) {
//            Log.e("SpriteSheet", "Invalid frame extraction coordinates: x=$x, y=$y")
//            return null
//        }
//
//        // Extract the bitmap for the frame
//        return Bitmap.createBitmap(spriteSheet, x, y, metadata.frame_width, metadata.frame_height)
//    }
//
//    private fun loadSpriteSheet(context: Context, spriteSheetName: String): Bitmap? {
//        // Check if the sprite sheet is already in cache
//        val spriteSheetResId = context.resources.getIdentifier(
//            spriteSheetName.substringBefore("."),
//            "drawable",
//            context.packageName
//        )
//
//        if (spriteSheetResId == 0) {
//            Log.e("SpriteSheet", "Sprite sheet resource not found for name: $spriteSheetName")
//            return null
//        }
//
//        // Load the sprite sheet bitmap with scaling to reduce memory usage
//        val spriteSheet = decodeBitmapFromResource(context, spriteSheetResId)
//        spriteSheet?.let {
//            spriteSheetCache[spriteSheetName] = it // Cache the bitmap
//        }
//
//        return spriteSheet
//    }
//
//    private fun decodeBitmapFromResource(context: Context, resId: Int): Bitmap? {
//        val options = BitmapFactory.Options()
//        options.inSampleSize = 2 // Scale down the image to reduce memory consumption
//        return BitmapFactory.decodeResource(context.resources, resId, options)
//    }
//
//    @OptIn(UnstableApi::class)
//    private fun updateThumbnailPosition(position: Long) {
//        val timeBar = binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
//        val timeBarWidth = timeBar.width
//        val imageWidth = binding.previewImage.width
//
//        if (exoplayer.duration <= 0) {
//            Log.e("SpriteSheet", "Invalid player duration: ${exoplayer.duration}")
//            return
//        }
//
//        val newPos = (position.toFloat() / exoplayer.duration) * timeBarWidth - (imageWidth / 2)
//        val clampedPos = newPos.coerceIn(0f, timeBarWidth - imageWidth.toFloat())
//
//        Log.d("SpriteSheet", "Calculated thumbnail position: $clampedPos")
//        binding.previewImage.x = clampedPos
//        binding.previewImage.visibility = View.VISIBLE
//    }
//}


//
//package com.example.spritesheets
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.net.Uri
//import android.os.Bundle
//import android.util.Log
//import android.view.View
//import androidx.fragment.app.FragmentActivity
//import androidx.media3.common.MediaItem
//import androidx.media3.exoplayer.ExoPlayer
//import androidx.media3.ui.DefaultTimeBar
//import androidx.media3.ui.TimeBar
//import com.bumptech.glide.Glide
//import com.bumptech.glide.load.engine.DiskCacheStrategy
//import com.bumptech.glide.request.target.SimpleTarget
//import com.bumptech.glide.request.transition.Transition
//import com.example.spritesheets.databinding.ActivityMainBinding
//import com.google.gson.Gson
//import com.google.gson.reflect.TypeToken
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//
//class MainActivity : FragmentActivity() {
//    private lateinit var binding: ActivityMainBinding
//    private lateinit var exoplayer: ExoPlayer
//    private lateinit var metadataList: List<SpriteSheetMetadata>
//    //private var videoUrl: String? = "https://bbcontent.nayatel.com/content/tour_guide_video_clips/Subtitles_Audio.mp4"
//    private var videoUrl: String? = "https://bbcontent.nayatel.com/content/tour_guide_video_clips/multi_linguistic.mp4"
//
//    private val executorService: ExecutorService = Executors.newSingleThreadExecutor() // Executor for async tasks
//    private val spriteSheetCache = mutableMapOf<String, Bitmap>() // Cache for sprite sheets
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Load and parse the sprite sheet metadata
//        metadataList = parseMetadataFromJson(this)
//        Log.d("SpriteSheet", "Parsed metadata: $metadataList")
//
//        // Initialize the ExoPlayer and set the media item
//        exoplayer = ExoPlayer.Builder(this).build()
//        binding.playerView.player = exoplayer
//        exoplayer.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
//        exoplayer.prepare()
//        exoplayer.play()
//
//        // Set up the scrub listener
//        setupScrubListener()
//    }
//
//    private fun setupScrubListener() {
//        binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
//            .addListener(object : TimeBar.OnScrubListener {
//                private var lastScrubbedPosition: Long = -1
//
//                override fun onScrubStart(timeBar: TimeBar, position: Long) {
//                    exoplayer.pause()
//                    Log.d("SpriteSheet", "Scrub started at position: $position")
//                    // Move frame extraction to background
//                    fetchAndDisplayFrame(position)
//                }
//
//                override fun onScrubMove(timeBar: TimeBar, position: Long) {
//                    // Only update if position has changed significantly
//                    if (position != lastScrubbedPosition) {
//                        lastScrubbedPosition = position
//                        // Asynchronous background task
//                        executorService.submit {
//                            fetchAndDisplayFrame(position)
//                        }
//                    }
//                }
//
//                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
//                    Log.d("SpriteSheet", "Scrub stopped at position: $position")
//                    // Fetch latest frame after scrub stops
//                    executorService.submit {
//                        fetchAndDisplayFrame(position)
//                    }
//                }
//            })
//    }
//
//    private fun parseMetadataFromJson(context: Context): List<SpriteSheetMetadata> {
//        val jsonString = context.assets.open("sprites_metadata.json").bufferedReader().use { it.readText() }
//        Log.d("SpriteSheet", "Read JSON: $jsonString")
//        return Gson().fromJson(jsonString, object : TypeToken<List<SpriteSheetMetadata>>() {}.type)
//    }
//
//    private fun fetchAndDisplayFrame(position: Long) {
//        val frame = extractFrameAtPosition(position / 1000, metadataList, this)
//        if (frame != null) {
//            Log.d("SpriteSheet", "Frame extracted successfully")
//            runOnUiThread {
//                // Set the extracted frame bitmap into the ImageView
//                binding.previewImage.setImageBitmap(frame)
//                updateThumbnailPosition(position)
//            }
//        } else {
//            Log.e("SpriteSheet", "Failed to extract frame at position: $position")
//            runOnUiThread {
//                binding.previewImage.visibility = View.GONE
//            }
//        }
//    }
//
//    private fun extractFrameAtPosition(
//        position: Long,
//        metadataList: List<SpriteSheetMetadata>,
//        context: Context
//    ): Bitmap? {
//        val metadata = metadataList.find { position in it.start_time..it.end_time } ?: return null.also {
//            Log.e("SpriteSheet", "No matching metadata found for position: $position")
//        }
//
//        // Load the sprite sheet from cache or download it
//        val spriteSheet = spriteSheetCache[metadata.sprite_link]
//            ?: loadSpriteSheet(context, metadata.sprite_link)
//
//        spriteSheet ?: return null
//
//        // Calculate the position of the frame in the grid
//        val timeInSheet = (position - metadata.start_time).toInt()
//        val frameIndex = timeInSheet / metadata.frame_interval
//        val x = (frameIndex % metadata.frames_per_row) * metadata.frame_width
//        val y = (frameIndex / metadata.frames_per_row) * metadata.frame_height
//
//        if (x < 0 || y < 0 || x + metadata.frame_width > spriteSheet.width || y + metadata.frame_height > spriteSheet.height) {
//            Log.e("SpriteSheet", "Invalid frame extraction coordinates: x=$x, y=$y")
//            return null
//        }
//
//        // Extract the bitmap for the frame
//        return Bitmap.createBitmap(spriteSheet, x, y, metadata.frame_width, metadata.frame_height)
//    }
//
//    private fun loadSpriteSheet(context: Context, spriteSheetUrl: String): Bitmap? {
//        // If already cached, return cached sprite sheet
//        spriteSheetCache[spriteSheetUrl]?.let { return it }
//
//        // Load sprite sheet from network using Glide
//        Glide.with(context)
//            .asBitmap()
//            .load(spriteSheetUrl)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
//            .into(object : SimpleTarget<Bitmap>() {
//                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                    // Cache the sprite sheet bitmap once it is ready
//                    spriteSheetCache[spriteSheetUrl] = resource
//                    // After caching, you can call frame extraction again
//                    Log.d("SpriteSheet", "Sprite sheet loaded and cached.")
//                }
//            })
//
//        // Return null temporarily; actual sprite sheet will be cached asynchronously
//        return null
//    }
//
//    private fun updateThumbnailPosition(position: Long) {
//        val timeBar = binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
//        val timeBarWidth = timeBar.width
//        val imageWidth = binding.previewImage.width
//
//        if (exoplayer.duration <= 0) {
//            Log.e("SpriteSheet", "Invalid player duration: ${exoplayer.duration}")
//            return
//        }
//
//        val newPos = (position.toFloat() / exoplayer.duration) * timeBarWidth - (imageWidth / 2)
//        val clampedPos = newPos.coerceIn(0f, timeBarWidth - imageWidth.toFloat())
//
//        Log.d("SpriteSheet", "Calculated thumbnail position: $clampedPos")
//        binding.previewImage.x = clampedPos
//        binding.previewImage.visibility = View.VISIBLE
//    }
//}
//package com.example.spritesheets
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.net.Uri
//import android.os.Bundle
//import android.util.Log
//import android.view.View
//import androidx.fragment.app.FragmentActivity
//import androidx.media3.common.MediaItem
//import androidx.media3.exoplayer.ExoPlayer
//import androidx.media3.ui.DefaultTimeBar
//import androidx.media3.ui.TimeBar
//import com.bumptech.glide.Glide
//import com.bumptech.glide.load.engine.DiskCacheStrategy
//import com.bumptech.glide.request.target.SimpleTarget
//import com.bumptech.glide.request.transition.Transition
//import com.example.spritesheets.databinding.ActivityMainBinding
//import com.google.gson.Gson
//import com.google.gson.reflect.TypeToken
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//
//class MainActivity : FragmentActivity() {
//    private lateinit var binding: ActivityMainBinding
//    private lateinit var exoplayer: ExoPlayer
//    private lateinit var metadataList: List<SpriteSheetMetadata>
//    private var videoUrl: String? = "https://bbcontent.nayatel.com/content/tour_guide_video_clips/multi_linguistic.mp4"
//    private val executorService: ExecutorService = Executors.newFixedThreadPool(4) // Optimize thread pool size
//    private val spriteSheetCache = mutableMapOf<String, Bitmap?>() // Cache for sprite sheets
//    private var lastScrubTime: Long = 0
//    private val MIN_SCRUB_INTERVAL = 50L  // Minimum interval between scrubs (in milliseconds)
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Load and parse the sprite sheet metadata
//        metadataList = parseMetadataFromJson(this)
//        Log.d("SpriteSheet", "Parsed metadata: $metadataList")
//
//        // Initialize the ExoPlayer and set the media item
//        exoplayer = ExoPlayer.Builder(this).build()
//        binding.playerView.player = exoplayer
//        exoplayer.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
//        exoplayer.prepare()
//        exoplayer.play()
//
//        // Set up the scrub listener
//        setupScrubListener()
//    }
//
//    private fun setupScrubListener() {
//        binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
//            .addListener(object : TimeBar.OnScrubListener {
//                private var lastScrubbedPosition: Long = -1
//
//                override fun onScrubStart(timeBar: TimeBar, position: Long) {
//                    exoplayer.pause()
//                    Log.d("SpriteSheet", "Scrub started at position: $position")
//                    // Move frame extraction to background
//                    fetchAndDisplayFrame(position)
//                }
//
//                override fun onScrubMove(timeBar: TimeBar, position: Long) {
//                    val currentTime = System.currentTimeMillis()
//                    if (currentTime - lastScrubTime > MIN_SCRUB_INTERVAL) {
//                        lastScrubTime = currentTime
//                        // Asynchronous background task
//                        executorService.submit {
//                            fetchAndDisplayFrame(position)
//                        }
//                    }
//                }
//
//                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
//                    Log.d("SpriteSheet", "Scrub stopped at position: $position")
//                    // Fetch latest frame after scrub stops
//                    executorService.submit {
//                        fetchAndDisplayFrame(position)
//                    }
//                }
//            })
//    }
//
//    private fun parseMetadataFromJson(context: Context): List<SpriteSheetMetadata> {
//        val jsonString = context.assets.open("sprites_metadata.json").bufferedReader().use { it.readText() }
//        Log.d("SpriteSheet", "Read JSON: $jsonString")
//        return Gson().fromJson(jsonString, object : TypeToken<List<SpriteSheetMetadata>>() {}.type)
//    }
//
//    private fun fetchAndDisplayFrame(position: Long) {
//        val frame = extractFrameAtPosition(position / 1000, metadataList, this)
//        if (frame != null) {
//            Log.d("SpriteSheet", "Frame extracted successfully")
//            runOnUiThread {
//                // Set the extracted frame bitmap into the ImageView
//                binding.previewImage.setImageBitmap(frame)
//                updateThumbnailPosition(position)
//            }
//        } else {
//            Log.e("SpriteSheet", "Failed to extract frame at position: $position")
//            runOnUiThread {
//                binding.previewImage.visibility = View.GONE
//            }
//        }
//    }
//
//    private fun extractFrameAtPosition(
//        position: Long,
//        metadataList: List<SpriteSheetMetadata>,
//        context: Context
//    ): Bitmap? {
//        val metadata = metadataList.find { position in it.start_time..it.end_time } ?: return null.also {
//            Log.e("SpriteSheet", "No matching metadata found for position: $position")
//        }
//
//        // Load the sprite sheet from cache or download it
//        val spriteSheet = spriteSheetCache[metadata.sprite_link]
//            ?: loadSpriteSheet(context, metadata.sprite_link)
//
//        spriteSheet ?: return null
//
//        // Calculate the position of the frame in the grid
//        val timeInSheet = (position - metadata.start_time).toInt()
//        val frameIndex = timeInSheet / metadata.frame_interval
//        val x = (frameIndex % metadata.frames_per_row) * metadata.frame_width
//        val y = (frameIndex / metadata.frames_per_row) * metadata.frame_height
//
//        if (x < 0 || y < 0 || x + metadata.frame_width > spriteSheet.width || y + metadata.frame_height > spriteSheet.height) {
//            Log.e("SpriteSheet", "Invalid frame extraction coordinates: x=$x, y=$y")
//            return null
//        }
//
//        // Extract the bitmap for the frame
//        return Bitmap.createBitmap(spriteSheet, x, y, metadata.frame_width, metadata.frame_height)
//    }
//
//    private fun loadSpriteSheet(context: Context, spriteSheetUrl: String): Bitmap? {
//        // If already cached, return cached sprite sheet
//        spriteSheetCache[spriteSheetUrl]?.let { return it }
//
//        // Load sprite sheet from network using Glide (ensure caching)
//        Glide.with(context)
//            .asBitmap()
//            .load(spriteSheetUrl)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
//            .into(object : SimpleTarget<Bitmap>() {
//                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                    // Cache the sprite sheet bitmap once it is ready
//                    spriteSheetCache[spriteSheetUrl] = resource
//                    Log.d("SpriteSheet", "Sprite sheet loaded and cached.")
//                }
//            })
//
//        // Return null temporarily; sprite sheet will be loaded asynchronously
//        return null
//    }
//
//    private fun updateThumbnailPosition(position: Long) {
//        val timeBar = binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
//        val timeBarWidth = timeBar.width
//        val imageWidth = binding.previewImage.width
//
//        if (timeBarWidth <= 0 || exoplayer.duration <= 0) {
//            return
//        }
//
//        // Calculate new thumbnail position
//        val newPos = (position.toFloat() / exoplayer.duration) * timeBarWidth - (imageWidth / 2)
//        val clampedPos = newPos.coerceIn(0f, timeBarWidth - imageWidth.toFloat())
//
//        // Only update if the position changed
//        if (clampedPos != binding.previewImage.x) {
//            binding.previewImage.x = clampedPos
//            binding.previewImage.visibility = View.VISIBLE
//        }
//    }
//
//    private fun decodeBitmapFromResource(context: Context, resId: Int): Bitmap? {
//        val options = BitmapFactory.Options().apply {
//            inJustDecodeBounds = true
//            BitmapFactory.decodeResource(context.resources, resId, this)
//            val scale = calculateInSampleSize(this, 200, 200)
//            inSampleSize = scale
//            inJustDecodeBounds = false
//        }
//        return BitmapFactory.decodeResource(context.resources, resId, options)
//    }
//
//    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
//        val (height: Int, width: Int) = options.run { outHeight to outWidth }
//        var inSampleSize = 1
//
//        if (height > reqHeight || width > reqWidth) {
//            val halfHeight = height / 2
//            val halfWidth = width / 2
//
//            // Calculate the largest inSampleSize value that is a power of 2 and keeps both height and width larger than the requested
//            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
//                inSampleSize *= 2
//            }
//        }
//        return inSampleSize
//    }
//}

//////////////////////////////////////////////////////////////////////////////////

//package com.example.spritesheets
//
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.net.Uri
//import android.os.Bundle
//import android.util.Log
//import android.view.View
//import androidx.fragment.app.FragmentActivity
//import androidx.collection.LruCache
//import androidx.media3.common.MediaItem
//import androidx.media3.exoplayer.ExoPlayer
//import androidx.media3.ui.DefaultTimeBar
//import androidx.media3.ui.TimeBar
//import com.example.spritesheets.databinding.ActivityMainBinding
//import com.google.gson.Gson
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.io.InputStream
//import java.net.URL
//
//class MainActivity : FragmentActivity() {
//    private lateinit var binding: ActivityMainBinding
//    private lateinit var exoplayer: ExoPlayer
//    private lateinit var spriteSheets: List<SpriteSheetMetadata>  // List to store sprite sheet data
//    private var videoUrl: String? = "https://bbcontent.nayatel.com/content/tour_guide_video_clips/multi_linguistic.mp4"
//
//    private val spriteCache = LruCache<String, Bitmap>(100) // Cache for sprites
//    private var lastScrubbedPosition: Long = -1
//
//    // Preloading buffer size (number of frames to preload ahead of time)
//    private val preloadBufferSize = 49
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Initialize ExoPlayer
//        exoplayer = ExoPlayer.Builder(this).build()
//        binding.playerView.player = exoplayer
//        exoplayer.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))  // Your video URL
//        exoplayer.prepare()
//        exoplayer.play()
//
//        // Load sprite sheet data from the assets folder
//        loadSpriteSheetDataFromAssets()
//
//        // Set up the scrub listener
//        setupScrubListener()
//    }
//
//    // Function to load sprite sheet data from the assets folder
//    private fun loadSpriteSheetDataFromAssets() {
//        // Open the JSON file from assets folder
//        val jsonString = loadJsonFromAssets("sprites_metadata.json")
//        spriteSheets = parseSpriteSheetData(jsonString)
//        Log.d("SpriteSheetValues", "Loaded sprite sheet data: ${spriteSheets.size} sheets")
//    }
//
//    // Function to load the JSON file from the assets folder
//    private fun loadJsonFromAssets(fileName: String): String {
//        val assetManager = assets
//        val inputStream: InputStream = assetManager.open(fileName)
//        return inputStream.bufferedReader().use { it.readText() }
//    }
//
//    // Function to parse JSON into a list of SpriteSheet objects
//    private fun parseSpriteSheetData(jsonString: String): List<SpriteSheetMetadata> {
//        val gson = Gson()
//        val data = gson.fromJson(jsonString, Array<SpriteSheetMetadata>::class.java).toList()
//        Log.d("SpriteSheetValues", "Parsed sprite sheet metadata: $data")
//        return data
//    }
//
//    // Set up scrub listener to track scrub position
//    private fun setupScrubListener() {
//        binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
//            .addListener(object : TimeBar.OnScrubListener {
//                override fun onScrubStart(timeBar: TimeBar, position: Long) {
//                    exoplayer.pause()
//                    lastScrubbedPosition = position
//                    Log.d("SpriteSheetValues", "Scrub started at position: $position")
//                }
//
//                override fun onScrubMove(timeBar: TimeBar, position: Long) {
//                    // Preload upcoming frames based on the scrub position
//                    updateThumbnailPosition(position)
//                }
//
//                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
//                    lastScrubbedPosition = position
//                    Log.d("SpriteSheetValues", "Scrub stopped at position: $position")
//                }
//            })
//    }
//
//    // Function to update the thumbnail based on the scrub position
//    private fun updateThumbnailPosition(position: Long) {
//        if (position == lastScrubbedPosition) return // Skip if no actual scrub
//
//        // Find relevant sprite sheet based on current scrub position
//        val spriteSheet = getRelevantSpriteSheet((position / 1000).toInt())
//        if (spriteSheet == null) {
//            Log.d("SpriteSheetValues", "No relevant sprite sheet found for position: $position")
//            return
//        }
//
//        // Preload future frames
//        preloadFrames(spriteSheet, position)
//
//        // Calculate the frame index based on the scrub position
//        val frameIndex = getFrameIndex(position.toInt(), spriteSheet)
//        Log.d("SpriteSheetValues", "Calculated frame index: $frameIndex for position: $position")
//
//        // Load and display the relevant sprite frame
//        loadAndDisplayFrame(spriteSheet, frameIndex)
//
//        // Update the UI with the thumbnail position
//        val timeBar = binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
//        val timeBarWidth = timeBar.width
//        val imageWidth = binding.previewImage.width
//
//        if (timeBarWidth <= 0 || exoplayer.duration <= 0) {
//            Log.d("SpriteSheetValues", "Time bar width or video duration is zero, skipping thumbnail update.")
//            return
//        }
//
//        // Calculate new thumbnail position
//        val newPos = (position.toFloat() / exoplayer.duration) * timeBarWidth - (imageWidth / 2)
//        val clampedPos = newPos.coerceIn(0f, timeBarWidth - imageWidth.toFloat())
//
//        // Only update if the position changed
//        if (clampedPos != binding.previewImage.x) {
//            binding.previewImage.x = clampedPos
//            binding.previewImage.visibility = View.VISIBLE
//            Log.d("SpriteSheetValues", "Thumbnail position updated to: $clampedPos")
//        }
//    }
//
//    // Preload a set number of future frames
//    private fun preloadFrames(spriteSheet: SpriteSheetMetadata, position: Long) {
//        val startFrame = getFrameIndex(position.toInt(), spriteSheet)
//        val framesToPreload = (startFrame until (startFrame + preloadBufferSize)).toList()
//
//        // Preload frames ahead of time
//        framesToPreload.forEach { frameIndex ->
//            CoroutineScope(Dispatchers.IO).launch {
//                if (getSpriteFromCache(spriteSheet.sprite_link, frameIndex) == null) {
//                    val spriteBitmap = fetchSpriteFrame(spriteSheet.sprite_link, frameIndex)
//                    addSpriteToCache(spriteSheet.sprite_link, frameIndex, spriteBitmap)
//                }
//            }
//        }
//    }
//
//    // Function to get the relevant sprite sheet based on the current time (position)
//    private fun getRelevantSpriteSheet(currentTime: Int): SpriteSheetMetadata? {
//        for (spriteSheet in spriteSheets) {
//            if (currentTime in spriteSheet.start_time..spriteSheet.end_time) {
//                Log.d("SpriteSheetValues", "Found relevant sprite sheet for position $currentTime: $spriteSheet")
//                return spriteSheet
//            }
//        }
//        Log.d("SpriteSheetValues", "No sprite sheet found for position: $currentTime")
//        return null
//    }
//
//    private fun getFrameIndex(currentTime: Int, spriteSheet: SpriteSheetMetadata): Int {
//        val frameOffset = (currentTime - spriteSheet.start_time) / spriteSheet.frame_interval
//        val frameIndex = frameOffset % spriteSheet.total_frames
//        Log.d("SpriteSheetValues", "Frame index calculated: $frameIndex for current time: $currentTime")
//        return frameIndex
//    }
//
//    private fun loadAndDisplayFrame(spriteSheet: SpriteSheetMetadata, frameIndex: Int) {
//        // First check if the sprite is already cached
//        val cachedSprite = getSpriteFromCache(spriteSheet.sprite_link, frameIndex)
//        if (cachedSprite != null) {
//            Log.d("SpriteSheetValues", "Sprite frame $frameIndex loaded from cache.")
//            binding.previewImage.setImageBitmap(cachedSprite)
//            return
//        }
//
//        // If not cached, load it from the source
//        Log.d("SpriteSheetValues", "Sprite frame $frameIndex not found in cache. Fetching from source.")
//        CoroutineScope(Dispatchers.IO).launch {
//            val spriteUrl = spriteSheet.sprite_link
//            val spriteBitmap = fetchSpriteFrame(spriteUrl, frameIndex)
//            addSpriteToCache(spriteUrl, frameIndex, spriteBitmap)
//            withContext(Dispatchers.Main) {
//                binding.previewImage.setImageBitmap(spriteBitmap)
//                Log.d("SpriteSheetValues", "Sprite frame $frameIndex loaded and displayed.")
//            }
//        }
//    }
//
//    // Function to fetch sprite frame from the sprite sheet (this can be an optimized implementation)
//    private fun fetchSpriteFrame(spriteUrl: String, frameIndex: Int): Bitmap {
//        try {
//            // Fetch the sprite sheet from the URL
//            val connection = URL(spriteUrl).openConnection()
//            connection.connectTimeout = 5000  // Set a timeout in case the network request hangs
//            connection.readTimeout = 5000
//
//            // Open the stream and decode the image
//            val inputStream = connection.getInputStream()
//            val spriteSheetBitmap = BitmapFactory.decodeStream(inputStream)
//
//            // Check if the Bitmap is null
//            if (spriteSheetBitmap == null) {
//                Log.e("SpriteSheetValues", "Failed to decode sprite sheet from URL: $spriteUrl")
//                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Return a blank image
//            }
//
//            Log.d("SpriteSheetValues", "Fetched sprite sheet from URL: $spriteUrl")
//
//            // Calculate the row and column of the frame in the sprite sheet grid
//            val frameWidth = 1280  // Use the frame width from the sprite sheet data
//            val frameHeight = 720  // Use the frame height from the sprite sheet data
//            val framesPerRow = 6   // The number of frames per row (based on the JSON)
//
//            // Calculate the row and column for the frame based on the frame index
//            val row = frameIndex / framesPerRow
//            val col = frameIndex % framesPerRow
//
//            // Define the rectangle for cropping the frame
//            val x = col * frameWidth
//            val y = row * frameHeight
//            val width = frameWidth
//            val height = frameHeight
//
//            // Ensure the sprite sheet is large enough to contain the frame
//            if (x + width <= spriteSheetBitmap.width && y + height <= spriteSheetBitmap.height) {
//                Log.d("SpriteSheetValues", "Sprite frame cropped at (x=$x, y=$y, width=$width, height=$height)")
//                return Bitmap.createBitmap(spriteSheetBitmap, x, y, width, height)
//            } else {
//                Log.e("SpriteSheetValues", "Frame index $frameIndex is out of bounds for the sprite sheet.")
//                return spriteSheetBitmap // Return the entire sprite sheet in case of error
//            }
//        } catch (e: Exception) {
//            Log.e("SpriteSheetValues", "Failed to fetch sprite frame from $spriteUrl: ${e.message}")
//            e.printStackTrace()
//            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Return a blank bitmap if there is an error
//        }
//    }
//
//    // Cache management - get sprite from cache
//    private fun getSpriteFromCache(spriteLink: String, frameIndex: Int): Bitmap? {
//        return spriteCache.get("$spriteLink-$frameIndex")
//    }
//
//    // Cache management - add sprite to cache
//    private fun addSpriteToCache(spriteLink: String, frameIndex: Int, sprite: Bitmap) {
//        spriteCache.put("$spriteLink-$frameIndex", sprite)
//        Log.d("SpriteSheetValues", "Sprite frame $frameIndex added to cache.")
//    }
//}
////-------------------------<<<<<<-------------->>>>>-------------
//fetching preload
//package com.example.spritesheets
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.net.Uri
//import android.os.Bundle
//import android.util.Log
//import android.view.View
//import androidx.fragment.app.FragmentActivity
//import androidx.media3.common.MediaItem
//import androidx.media3.exoplayer.ExoPlayer
//import androidx.media3.ui.DefaultTimeBar
//import androidx.media3.ui.TimeBar
//import com.example.spritesheets.databinding.ActivityMainBinding
//import com.google.gson.Gson
//import com.google.gson.reflect.TypeToken
//import java.io.InputStream
//import java.net.HttpURLConnection
//import java.net.URL
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//
//class MainActivity : FragmentActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private lateinit var exoplayer: ExoPlayer
//    private lateinit var metadataList: List<SpriteSheetMetadata>
//    private var videoUrl: String? = "https://bbcontent.nayatel.com/content/tour_guide_video_clips/multi_linguistic.mp4"
//
//    private val executorService: ExecutorService = Executors.newSingleThreadExecutor() // Executor for async tasks
//    private val spriteSheetCache = mutableMapOf<String, Bitmap>() // Cache for sprite sheets
//    private var lastScrubbedPosition: Long = -1 // Track the last scrubbed position to determine when to load the next sprite sheet
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Load and parse the sprite sheet metadata
//        metadataList = parseMetadataFromJson(this)
//        Log.d("SpriteSheet", "Parsed metadata: $metadataList")
//
//        // Preload the first sprite sheet into cache
//        preloadFirstSpriteSheet()
//
//        // Initialize the ExoPlayer and set the media item
//        exoplayer = ExoPlayer.Builder(this).build()
//        binding.playerView.player = exoplayer
//        exoplayer.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
//        exoplayer.prepare()
//        exoplayer.play()
//
//        // Set up the scrub listener
//        setupScrubListener()
//    }
//
//    private fun preloadFirstSpriteSheet() {
//        // Preload the first sprite sheet and cache it
//        val firstSpriteSheetMetadata = metadataList.firstOrNull()
//        if (firstSpriteSheetMetadata != null) {
//            val firstSpriteSheetUrl = firstSpriteSheetMetadata.sprite_link
//            if (!spriteSheetCache.containsKey(firstSpriteSheetUrl)) {
//                executorService.submit {
//                    loadSpriteSheetFromUrl(firstSpriteSheetUrl)
//                }
//            }
//        }
//    }
//    private fun parseMetadataFromJson(context: Context): List<SpriteSheetMetadata> {
//                val jsonString = context.assets.open("sprites_metadata.json").bufferedReader().use { it.readText() }
//        Log.d("SpriteSheet", "Read JSON: $jsonString")
//        return Gson().fromJson(jsonString, object : TypeToken<List<SpriteSheetMetadata>>() {}.type)
//    }
//
//    private fun setupScrubListener() {
//        binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
//            .addListener(object : TimeBar.OnScrubListener {
//                override fun onScrubStart(timeBar: TimeBar, position: Long) {
//                    exoplayer.pause()
//                    Log.d("SpriteSheet", "Scrub started at position: $position")
//                    // Fetch and display the first frame immediately
//                    fetchAndDisplayFrame(position)
//                }
//
//                override fun onScrubMove(timeBar: TimeBar, position: Long) {
//                    // Asynchronous background task for frame fetching
//                    if (position != lastScrubbedPosition) {
//                        lastScrubbedPosition = position
//                        executorService.submit {
//                            fetchAndDisplayFrame(position)
//                        }
//
//                        // Check if we need to fetch the next sprite sheet
//                        val currentMetadata = getMetadataForPosition(position)
//                        if (currentMetadata != null) {
//                            val nextMetadata = getNextSpriteSheetMetadata(currentMetadata)
//                            if (nextMetadata != null && position > (currentMetadata.end_time + 1) * 1000) {
//                                fetchAndCacheSpriteSheet(nextMetadata)
//                            }
//                        }
//                    }
//                }
//
//                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
//                    Log.d("SpriteSheet", "Scrub stopped at position: $position")
//                    // Fetch the last frame after scrub stops
//                    executorService.submit {
//                        fetchAndDisplayFrame(position)
//                    }
//                }
//            })
//    }
//
//    private fun getMetadataForPosition(position: Long): SpriteSheetMetadata? {
//        // Find the metadata corresponding to the current position
//        return metadataList.find { position in it.start_time..it.end_time }
//    }
//
//    private fun getNextSpriteSheetMetadata(currentMetadata: SpriteSheetMetadata): SpriteSheetMetadata? {
//        // Get the metadata for the next sprite sheet, which is the next one in the list
//        val currentIndex = metadataList.indexOf(currentMetadata)
//        return if (currentIndex != -1 && currentIndex < metadataList.size - 1) {
//            metadataList[currentIndex + 1]
//        } else {
//            null
//        }
//    }
//
//    private fun fetchAndDisplayFrame(position: Long) {
//        val frame = extractFrameAtPosition(position / 1000, metadataList, this)
//        if (frame != null) {
//            Log.d("SpriteSheet", "Frame extracted successfully")
//            runOnUiThread {
//                binding.previewImage.setImageBitmap(frame)
//                updateThumbnailPosition(position)
//            }
//        } else {
//            Log.e("SpriteSheet", "Failed to extract frame at position: $position")
//            runOnUiThread {
//                binding.previewImage.visibility = View.GONE
//            }
//        }
//    }
//
//    private fun extractFrameAtPosition(position: Long, metadataList: List<SpriteSheetMetadata>, context: Context): Bitmap? {
//        val metadata = metadataList.find { position in it.start_time..it.end_time } ?: return null.also {
//            Log.e("SpriteSheet", "No matching metadata found for position: $position")
//        }
//
//        // Load the sprite sheet from cache or disk if necessary
//        val spriteSheet = spriteSheetCache[metadata.sprite_link]
//            ?: loadSpriteSheetFromUrl(metadata.sprite_link)
//
//        spriteSheet ?: return null
//
//        // Calculate the position of the frame in the grid
//        val timeInSheet = (position - metadata.start_time).toInt()
//        val frameIndex = timeInSheet / metadata.frame_interval
//        val x = (frameIndex % metadata.frames_per_row) * metadata.frame_width
//        val y = (frameIndex / metadata.frames_per_row) * metadata.frame_height
//
//        if (x < 0 || y < 0 || x + metadata.frame_width > spriteSheet.width || y + metadata.frame_height > spriteSheet.height) {
//            Log.e("SpriteSheet", "Invalid frame extraction coordinates: x=$x, y=$y")
//            return null
//        }
//
//        // Extract the bitmap for the frame
//        return Bitmap.createBitmap(spriteSheet, x, y, metadata.frame_width, metadata.frame_height)
//    }
//
//    private fun fetchAndCacheSpriteSheet(metadata: SpriteSheetMetadata) {
//        val spriteSheetUrl = metadata.sprite_link
//        if (!spriteSheetCache.containsKey(spriteSheetUrl)) {
//            executorService.submit {
//                loadSpriteSheetFromUrl(spriteSheetUrl)
//            }
//        }
//    }
//
//    private fun loadSpriteSheetFromUrl(spriteSheetUrl: String): Bitmap? {
//        var bitmap: Bitmap? = null
//
//        // Run the download on a background thread to avoid blocking the UI thread
//        val future = executorService.submit<Bitmap?> {
//            try {
//                Log.d("SpriteSheet", "Downloading sprite sheet from URL: $spriteSheetUrl")
//                val url = URL(spriteSheetUrl)
//                val connection = url.openConnection() as HttpURLConnection
//                connection.apply {
//                    connectTimeout = 10000
//                    readTimeout = 10000
//                    requestMethod = "GET"
//                }
//
//                val inputStream: InputStream = connection.inputStream
//                val downloadedBitmap = BitmapFactory.decodeStream(inputStream)
//                inputStream.close()
//
//                if (downloadedBitmap != null) {
//                    // Cache the downloaded sprite sheet
//                    spriteSheetCache[spriteSheetUrl] = downloadedBitmap
//                }
//
//                downloadedBitmap
//            } catch (e: Exception) {
//                Log.e("SpriteSheet", "Failed to load sprite sheet from URL: $spriteSheetUrl", e)
//                null
//            }
//        }
//
//        try {
//            // Wait for the background task to complete and return the bitmap
//            bitmap = future.get()
//        } catch (e: Exception) {
//            Log.e("SpriteSheet", "Error retrieving sprite sheet from URL: $spriteSheetUrl", e)
//        }
//
//        return bitmap
//    }
//
//    private fun updateThumbnailPosition(position: Long) {
//        val timeBar = binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
//        val timeBarWidth = timeBar.width
//        val imageWidth = binding.previewImage.width
//
//        if (exoplayer.duration <= 0) {
//            Log.e("SpriteSheet", "Invalid player duration: ${exoplayer.duration}")
//            return
//        }
//
//        val newPos = (position.toFloat() / exoplayer.duration) * timeBarWidth - (imageWidth / 2)
//        val clampedPos = newPos.coerceIn(0f, timeBarWidth - imageWidth.toFloat())
//
//        Log.d("SpriteSheet", "Calculated thumbnail position: $clampedPos")
//        binding.previewImage.x = clampedPos
//        binding.previewImage.visibility = View.VISIBLE
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        // Cleanup and release resources
//        spriteSheetCache.clear()
//        exoplayer.release()
//    }
//}

//USING FRAMES FETCHING
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
import kotlin.math.log

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

    private fun parseThumbnailMetadataFromJson(context: Context): List<ThumbnailMetadata> {
        val jsonString = context.assets.open("frames_metadata.json").bufferedReader().use { it.readText() }
        Log.d("Thumbnail", "Read JSON: $jsonString")
        return Gson().fromJson(jsonString, object : TypeToken<List<ThumbnailMetadata>>() {}.type)
    }

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

                    // Pass the parsed list back to the main thread using callback
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
