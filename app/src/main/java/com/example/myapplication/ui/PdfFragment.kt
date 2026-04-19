package com.example.myapplication.ui

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.RecentPdfRepository
import com.example.myapplication.databinding.FragmentPdfBinding
import com.example.myapplication.ui.home.RecentPdfViewModel
import com.example.myapplication.ui.home.RecentPdfViewModelFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class PdfFragment : Fragment() {

    private var _binding: FragmentPdfBinding? = null
    private val binding get() = _binding!!

    private var pdfUriString: String? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var currentPageIndex = 0

    private var whiteboardVisible = true
    private var currentWhitePageIndex = 0
    private var whitePageCount = 1

    private lateinit var recentPdfViewModel: RecentPdfViewModel
    private var recentSaved = false

    private val prefs by lazy {
        requireContext().getSharedPreferences("pdf_edits", 0)
    }

    private var textModeEnabled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPdfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecentPdfViewModel()

        pdfUriString = arguments?.getString("pdfUri")
        if (pdfUriString.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "PDF path missing", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse(pdfUriString)
        takeReadPermissionIfPossible(uri)
        savePdfToRecent(uri)
        openPdf(uri)
        setupUi()
    }

    private fun setupRecentPdfViewModel() {
        val dao = AppDatabase.getInstance(requireContext()).recentPdfDao()
        val repository = RecentPdfRepository(dao)
        val factory = RecentPdfViewModelFactory(repository)
        recentPdfViewModel = ViewModelProvider(this, factory)[RecentPdfViewModel::class.java]
    }

    private fun setupUi() {
        fun enableTextMode() {
            textModeEnabled = true
            binding.drawingView.setDrawingEnabled(false)

            binding.pdfTapLayer.visibility = View.VISIBLE
            binding.whiteTapLayer.visibility = View.VISIBLE

            binding.pdfTapLayer.isClickable = true
            binding.whiteTapLayer.isClickable = true
        }

        fun disableTextMode() {
            textModeEnabled = false

            binding.pdfTapLayer.visibility = View.GONE
            binding.whiteTapLayer.visibility = View.GONE

            binding.pdfTapLayer.isClickable = false
            binding.whiteTapLayer.isClickable = false
        }

        binding.btnPrevious.setOnClickListener {
            if (currentPageIndex > 0) {
                saveCurrentPdfPageEdits()
                saveCurrentWhitePageEdits()
                showPage(currentPageIndex - 1)
            }
        }

        binding.btnNext.setOnClickListener {
            val renderer = pdfRenderer ?: return@setOnClickListener
            if (currentPageIndex < renderer.pageCount - 1) {
                saveCurrentPdfPageEdits()
                saveCurrentWhitePageEdits()
                showPage(currentPageIndex + 1)
            }
        }

        binding.btnPrevWhite.setOnClickListener {
            if (currentWhitePageIndex > 0) {
                saveCurrentWhitePageEdits()
                currentWhitePageIndex--
                restoreWhitePageEdits()
                updateWhiteUi()
            }
        }

        binding.btnNextWhite.setOnClickListener {
            if (currentWhitePageIndex < whitePageCount - 1) {
                saveCurrentWhitePageEdits()
                currentWhitePageIndex++
                restoreWhitePageEdits()
                updateWhiteUi()
            }
        }

        binding.btnAddWhitePage.setOnClickListener {
            val pdfKey = pdfUriString ?: return@setOnClickListener
            saveCurrentWhitePageEdits()
            whitePageCount++
            prefs.edit()
                .putInt(currentPdfPageWhiteCountKey(pdfKey, currentPageIndex), whitePageCount)
                .apply()
            currentWhitePageIndex = whitePageCount - 1
            clearWhitePageViews()
            updateWhiteUi()
        }

        binding.btnToggleWhiteboard.setOnClickListener {
            whiteboardVisible = !whiteboardVisible
            binding.whitePane.visibility = if (whiteboardVisible) View.VISIBLE else View.GONE
            updateWhiteUi()
        }

        binding.btnSave.setOnClickListener {
            saveCurrentPdfPageEdits()
            saveCurrentWhitePageEdits()
            exportEditedPdfToDevice()
        }

        binding.btnText.setOnClickListener {
            enableTextMode()
            Toast.makeText(requireContext(), "Tap on PDF or white page", Toast.LENGTH_SHORT).show()
        }

        binding.pdfTapLayer.setOnTouchListener { _, event ->
            if (textModeEnabled && event.action == MotionEvent.ACTION_UP) {
                addTextBox(
                    parent = binding.pdfTextOverlay,
                    hint = "Text",
                    x = event.x,
                    y = event.y
                )
                saveCurrentPdfPageEdits()
                disableTextMode()
                true
            } else {
                false
            }
        }

        binding.whiteTapLayer.setOnTouchListener { _, event ->
            if (textModeEnabled && event.action == MotionEvent.ACTION_UP) {
                addTextBox(
                    parent = binding.whiteTextOverlay,
                    hint = "Text",
                    x = event.x,
                    y = event.y
                )
                saveCurrentWhitePageEdits()
                disableTextMode()
                true
            } else {
                false
            }
        }

        binding.btnPen.setOnClickListener {
            disableTextMode()
            binding.drawingView.setDrawingEnabled(true)
            binding.drawingView.setEraserMode(false)
        }

        binding.btnEraser.setOnClickListener {
            disableTextMode()
            binding.drawingView.setDrawingEnabled(true)
            binding.drawingView.setEraserMode(true)
        }

        binding.sizeSeekBar.max = 60
        binding.sizeSeekBar.progress = 12
        binding.drawingView.setStrokeWidth(12f)

        binding.sizeSeekBar.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: android.widget.SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                binding.drawingView.setStrokeWidth(progress.coerceAtLeast(3).toFloat())
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        disableTextMode()
    }

    private fun updateWhiteUi() {
        binding.tvWhitePageNumber.text = "W ${currentWhitePageIndex + 1} / $whitePageCount"
        binding.btnToggleWhiteboard.text = if (whiteboardVisible) "Hide W" else "Show W"
        binding.whitePane.visibility = if (whiteboardVisible) View.VISIBLE else View.GONE

        binding.btnPrevWhite.isEnabled = currentWhitePageIndex > 0
        binding.btnNextWhite.isEnabled = currentWhitePageIndex < whitePageCount - 1

        binding.btnPrevWhite.alpha = if (binding.btnPrevWhite.isEnabled) 1f else 0.35f
        binding.btnNextWhite.alpha = if (binding.btnNextWhite.isEnabled) 1f else 0.35f
    }

    private fun openPdf(uri: Uri) {
        try {
            fileDescriptor = requireContext().contentResolver.openFileDescriptor(uri, "r")
            if (fileDescriptor == null) {
                Toast.makeText(requireContext(), "Unable to open PDF", Toast.LENGTH_SHORT).show()
                return
            }

            pdfRenderer = PdfRenderer(fileDescriptor!!)
            showPage(0)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showPage(index: Int) {
        val renderer = pdfRenderer ?: return

        currentPage?.close()
        currentPage = renderer.openPage(index)
        currentPageIndex = index

        val page = currentPage ?: return
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        binding.pdfImageView.setImageBitmap(bitmap)
        binding.tvPageNumber.text = "Page ${index + 1} / ${renderer.pageCount}"

        binding.pdfTextOverlay.removeAllViews()
        restorePdfPageEdits()

        val pdfKey = pdfUriString ?: return
        whitePageCount = prefs.getInt(
            currentPdfPageWhiteCountKey(pdfKey, currentPageIndex),
            1
        ).coerceAtLeast(1)
        currentWhitePageIndex = 0

        restoreWhitePageEdits()
        updateWhiteUi()
    }

    private fun savePdfToRecent(uri: Uri) {
        if (recentSaved) return
        val name = getFileName(uri)
        recentPdfViewModel.addRecentPdf(uri.toString(), name)
        recentSaved = true
    }

    private fun getFileName(uri: Uri): String {
        var result = "PDF File"

        if (uri.scheme == "content") {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index != -1) {
                    result = cursor.getString(index)
                }
            }
        } else {
            val path = uri.path
            if (!path.isNullOrEmpty()) {
                result = path.substringAfterLast('/')
            }
        }

        return result
    }

    private fun takeReadPermissionIfPossible(uri: Uri) {
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
    }

    private fun addTextBox(
        parent: FrameLayout,
        hint: String,
        initialText: String = "",
        x: Float? = null,
        y: Float? = null,
        width: Int? = null,
        height: Int? = null
    ) {
        val boxWidth = width ?: dpToPx(170)
        val boxHeight = height ?: dpToPx(70)

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(10).toFloat()
            setColor(Color.argb(32, 255, 255, 255))
            setStroke(dpToPx(1), Color.argb(110, 103, 80, 164))
        }

        val editText = EditText(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(boxWidth, boxHeight)
            this.hint = hint
            setText(initialText)
            background = bg
            setTextColor(Color.BLACK)
            setHintTextColor(Color.argb(140, 80, 80, 80))
            gravity = Gravity.TOP or Gravity.START
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            elevation = 20f
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            minLines = 1
            maxLines = 6
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            isSingleLine = false

            setOnFocusChangeListener { _, _ ->
                if (parent === binding.pdfTextOverlay) {
                    saveCurrentPdfPageEdits()
                } else {
                    saveCurrentWhitePageEdits()
                }
            }

            setOnLongClickListener {
                parent.removeView(this)
                if (parent === binding.pdfTextOverlay) {
                    saveCurrentPdfPageEdits()
                } else {
                    saveCurrentWhitePageEdits()
                }
                true
            }
        }

        parent.addView(editText)

        parent.doOnLayout {
            val safeWidth = if ((width ?: 0) > 20) width!! else dpToPx(170)
            val safeHeight = if ((height ?: 0) > 20) height!! else dpToPx(70)

            editText.layoutParams = FrameLayout.LayoutParams(safeWidth, safeHeight)

            val targetX = (x ?: (parent.width / 2f - safeWidth / 2f))
            val targetY = (y ?: (parent.height / 4f - safeHeight / 2f))

            val maxX = (parent.width - safeWidth).coerceAtLeast(0)
            val maxY = (parent.height - safeHeight).coerceAtLeast(0)

            editText.x = targetX.coerceIn(0f, maxX.toFloat())
            editText.y = targetY.coerceIn(0f, maxY.toFloat())
            editText.bringToFront()
        }

        makeTextBoxDraggable(editText, parent)
        editText.requestFocus()
    }

    private fun makeTextBoxDraggable(target: EditText, parent: ViewGroup) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0f
        var startY = 0f
        var dragging = false
        val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop

        target.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = v.x
                    startY = v.y
                    dragging = false
                    false
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY

                    if (!dragging && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                        dragging = true
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    }

                    if (dragging) {
                        val maxX = (parent.width - v.width).toFloat().coerceAtLeast(0f)
                        val maxY = (parent.height - v.height).toFloat().coerceAtLeast(0f)
                        v.x = (startX + dx).coerceIn(0f, maxX)
                        v.y = (startY + dy).coerceIn(0f, maxY)
                        true
                    } else {
                        false
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                    if (dragging) {
                        if (parent === binding.pdfTextOverlay) {
                            saveCurrentPdfPageEdits()
                        } else {
                            saveCurrentWhitePageEdits()
                        }
                        true
                    } else {
                        false
                    }
                }

                else -> false
            }
        }
    }

    private fun saveCurrentPdfPageEdits() {
        val pdfKey = pdfUriString ?: return
        saveTextOverlay(pdfTextKey(pdfKey, currentPageIndex), binding.pdfTextOverlay)
    }

    private fun restorePdfPageEdits() {
        val pdfKey = pdfUriString ?: return
        restoreTextOverlay(pdfTextKey(pdfKey, currentPageIndex), binding.pdfTextOverlay)
    }

    private fun saveCurrentWhitePageEdits() {
        val pdfKey = pdfUriString ?: return
        saveTextOverlay(
            whiteTextKey(pdfKey, currentPageIndex, currentWhitePageIndex),
            binding.whiteTextOverlay
        )
        saveDrawingBitmap(
            whiteDrawingFileName(pdfKey, currentPageIndex, currentWhitePageIndex),
            binding.drawingView
        )
    }

    private fun restoreWhitePageEdits() {
        clearWhitePageViews()
        val pdfKey = pdfUriString ?: return

        restoreTextOverlay(
            whiteTextKey(pdfKey, currentPageIndex, currentWhitePageIndex),
            binding.whiteTextOverlay
        )

        val whiteBitmap = loadBitmapFromFile(
            whiteDrawingFileName(pdfKey, currentPageIndex, currentWhitePageIndex)
        )

        binding.drawingView.clearCanvas()
        if (whiteBitmap != null) {
            binding.drawingView.loadBitmap(whiteBitmap)
        }
    }

    private fun clearWhitePageViews() {
        binding.whiteTextOverlay.removeAllViews()
        binding.drawingView.clearCanvas()
    }

    private fun saveTextOverlay(key: String, parent: FrameLayout) {
        val arr = JSONArray()

        for (i in 0 until parent.childCount) {
            val editText = parent.getChildAt(i) as? EditText ?: continue

            val w = if (editText.width > 20) editText.width else dpToPx(170)
            val h = if (editText.height > 20) editText.height else dpToPx(70)

            val obj = JSONObject().apply {
                put("text", editText.text.toString())
                put("hint", editText.hint?.toString() ?: "")
                put("x", editText.x.toDouble())
                put("y", editText.y.toDouble())
                put("width", w)
                put("height", h)
                put("parentWidth", parent.width.coerceAtLeast(1))
                put("parentHeight", parent.height.coerceAtLeast(1))
            }
            arr.put(obj)
        }

        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun restoreTextOverlay(key: String, parent: FrameLayout) {
        val raw = prefs.getString(key, null) ?: return
        val arr = JSONArray(raw)

        parent.doOnLayout {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)

                val savedWidth = obj.optInt("width", dpToPx(170)).coerceAtLeast(dpToPx(120))
                val savedHeight = obj.optInt("height", dpToPx(70)).coerceAtLeast(dpToPx(48))

                addTextBox(
                    parent = parent,
                    hint = obj.optString("hint", ""),
                    initialText = obj.optString("text", ""),
                    x = obj.optDouble("x", 0.0).toFloat(),
                    y = obj.optDouble("y", 0.0).toFloat(),
                    width = savedWidth,
                    height = savedHeight
                )
            }
        }
    }

    private fun saveDrawingBitmap(fileName: String, sourceView: View) {
        if (sourceView.width <= 0 || sourceView.height <= 0) return

        val bitmap = Bitmap.createBitmap(
            sourceView.width,
            sourceView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        sourceView.draw(canvas)

        if (isBitmapBlank(bitmap)) {
            deleteBitmapFile(fileName)
            return
        }

        val file = File(requireContext().filesDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun loadBitmapFromFile(fileName: String): Bitmap? {
        val file = File(requireContext().filesDir, fileName)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    private fun deleteBitmapFile(fileName: String) {
        val file = File(requireContext().filesDir, fileName)
        if (file.exists()) file.delete()
    }

    private fun exportEditedPdfToDevice() {
        val renderer = pdfRenderer ?: return
        val pdfKey = pdfUriString ?: return
        val fileName = buildExportFileName()
        val document = PdfDocument()

        try {
            saveCurrentPdfPageEdits()
            saveCurrentWhitePageEdits()

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val srcBitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(srcBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val pageInfo = PdfDocument.PageInfo.Builder(
                    srcBitmap.width,
                    srcBitmap.height,
                    document.pages.size + 1
                ).create()

                val docPage = document.startPage(pageInfo)
                val canvas = docPage.canvas
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(srcBitmap, 0f, 0f, null)

                drawSavedTextsOnCanvas(
                    canvas = canvas,
                    textKey = pdfTextKey(pdfKey, i),
                    targetWidth = srcBitmap.width,
                    targetHeight = srcBitmap.height
                )

                document.finishPage(docPage)
                page.close()
            }

            for (pdfPageIndex in 0 until renderer.pageCount) {
                val count = prefs.getInt(
                    currentPdfPageWhiteCountKey(pdfKey, pdfPageIndex),
                    1
                ).coerceAtLeast(1)

                for (whiteIndex in 0 until count) {
                    val width = 1200
                    val height = 1600

                    val pageInfo = PdfDocument.PageInfo.Builder(
                        width,
                        height,
                        document.pages.size + 1
                    ).create()

                    val docPage = document.startPage(pageInfo)
                    val canvas = docPage.canvas
                    canvas.drawColor(Color.WHITE)

                    val whiteBitmap = loadBitmapFromFile(
                        whiteDrawingFileName(pdfKey, pdfPageIndex, whiteIndex)
                    )

                    if (whiteBitmap != null) {
                        val dst = RectF(0f, 0f, width.toFloat(), height.toFloat())
                        canvas.drawBitmap(whiteBitmap, null, dst, null)
                    }

                    drawSavedTextsOnCanvas(
                        canvas = canvas,
                        textKey = whiteTextKey(pdfKey, pdfPageIndex, whiteIndex),
                        targetWidth = width,
                        targetHeight = height
                    )

                    document.finishPage(docPage)
                }
            }

            val ok = writePdfToDownloads(fileName, document)

            if (ok) {
                Toast.makeText(requireContext(), "Saved to device: $fileName", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_LONG).show()
            }

            showPage(currentPageIndex)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            document.close()
        }
    }

    private fun drawSavedTextsOnCanvas(
        canvas: Canvas,
        textKey: String,
        targetWidth: Int,
        targetHeight: Int
    ) {
        val raw = prefs.getString(textKey, null) ?: return
        val arr = JSONArray(raw)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 40f
        }

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val text = obj.optString("text", "")
            if (text.isBlank()) continue

            val parentWidth = obj.optInt("parentWidth", targetWidth).coerceAtLeast(1)
            val parentHeight = obj.optInt("parentHeight", targetHeight).coerceAtLeast(1)
            val scaleX = targetWidth.toFloat() / parentWidth.toFloat()
            val scaleY = targetHeight.toFloat() / parentHeight.toFloat()

            val x = obj.optDouble("x", 0.0).toFloat() * scaleX
            var y = obj.optDouble("y", 0.0).toFloat() * scaleY + paint.textSize

            val lines = text.split("\n")
            for (line in lines) {
                canvas.drawText(line, x, y, paint)
                y += paint.textSize * 1.25f
            }
        }
    }

    private fun writePdfToDownloads(fileName: String, document: PdfDocument): Boolean {
        val resolver = requireContext().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MyApplication")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values) ?: return false

        return try {
            resolver.openOutputStream(uri)?.use { output ->
                document.writeTo(output)
            } ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            true
        } catch (_: Exception) {
            false
        }
    }

    private fun buildExportFileName(): String {
        val name = getFileName(Uri.parse(pdfUriString))
        val base = if (name.endsWith(".pdf", true)) name.removeSuffix(".pdf") else name
        return "${base}_edited.pdf"
    }

    private fun isBitmapBlank(bitmap: Bitmap): Boolean {
        val widthStep = (bitmap.width / 20).coerceAtLeast(1)
        val heightStep = (bitmap.height / 20).coerceAtLeast(1)

        for (x in 0 until bitmap.width step widthStep) {
            for (y in 0 until bitmap.height step heightStep) {
                val pixel = bitmap.getPixel(x, y)
                if (pixel != Color.TRANSPARENT && pixel != Color.WHITE) {
                    return false
                }
            }
        }
        return true
    }

    private fun pdfTextKey(pdfKey: String, pageIndex: Int): String {
        return "${safePdfKey(pdfKey)}_pdf_text_$pageIndex"
    }

    private fun currentPdfPageWhiteCountKey(pdfKey: String, pdfPageIndex: Int): String {
        return "${safePdfKey(pdfKey)}_pdfpage_${pdfPageIndex}_white_count"
    }

    private fun whiteTextKey(pdfKey: String, pdfPageIndex: Int, whiteIndex: Int): String {
        return "${safePdfKey(pdfKey)}_pdfpage_${pdfPageIndex}_white_text_$whiteIndex"
    }

    private fun whiteDrawingFileName(pdfKey: String, pdfPageIndex: Int, whiteIndex: Int): String {
        return "white_draw_${safePdfKey(pdfKey)}_${pdfPageIndex}_$whiteIndex.png"
    }

    private fun safePdfKey(value: String): String {
        return Base64.encodeToString(value.toByteArray(), Base64.NO_WRAP)
            .replace("/", "_")
            .replace("+", "-")
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onPause() {
        saveCurrentPdfPageEdits()
        saveCurrentWhitePageEdits()
        super.onPause()
    }

    override fun onDestroyView() {
        saveCurrentPdfPageEdits()
        saveCurrentWhitePageEdits()

        try {
            currentPage?.close()
        } catch (_: Exception) {
        }

        try {
            pdfRenderer?.close()
        } catch (_: Exception) {
        }

        try {
            fileDescriptor?.close()
        } catch (_: Exception) {
        }

        _binding = null
        super.onDestroyView()
    }
}