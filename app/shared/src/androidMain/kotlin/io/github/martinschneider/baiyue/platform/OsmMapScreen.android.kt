package io.github.martinschneider.baiyue.platform

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.martinschneider.baiyue.data.model.HikeDescription
import io.github.martinschneider.baiyue.data.model.Mountain
import io.github.martinschneider.baiyue.data.model.MountainType
import io.github.martinschneider.baiyue.data.model.Region
import io.github.martinschneider.baiyue.ui.screen.map.MapViewModel
import io.github.martinschneider.baiyue.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow

@Composable
internal fun OsmMapContent(
    viewModel: MapViewModel,
    mountains: List<Mountain>,
    climbed: Map<Long, Boolean>,
    photos: Set<Long>,
    onMarkerClick: (Long) -> Unit,
    showBaiyue: Boolean,
    showXiaobaiyue: Boolean,
    tracks: List<HikeDescription> = emptyList(),
    showTracks: Boolean = false,
    onTrackClick: (HikeDescription) -> Unit = {},
) {
    val context = LocalContext.current
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val pendingZoomTarget by viewModel.pendingZoomTarget.collectAsState()
    val pendingBoundsZoom by viewModel.pendingBoundsZoom.collectAsState()

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose {
            mapViewRef.value?.onDetach()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val prefs = viewModel.mapPreferences
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(
                        org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                    )
                    controller.setCenter(
                        GeoPoint(prefs.mapLat, prefs.mapLng)
                    )
                    controller.setZoom(prefs.mapZoom)
                    mapViewRef.value = this
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            val center = mapCenter
                            prefs.mapLat = center.latitude
                            prefs.mapLng = center.longitude
                            invalidate()
                            return false
                        }
                        override fun onZoom(event: ZoomEvent?): Boolean {
                            prefs.mapZoom = zoomLevelDouble
                            val show = zoomLevelDouble >= 10.0
                            overlays.filterIsInstance<Polyline>().forEach { it.isVisible = show }
                            invalidate()
                            return false
                        }
                    })
                }
            },
            update = { mapView ->
                // Handle pending zoom target from list navigation
                pendingZoomTarget?.let { target ->
                    viewModel.clearPendingZoomTarget()
                    mapView.post {
                        val mapHeight = mapView.height
                        val targetZoom = 15.0
                        if (mapHeight > 0) {
                            val metersPerPixel = 156543.03 * Math.cos(Math.toRadians(target.lat)) / Math.pow(2.0, targetZoom)
                            val offsetPixels = mapHeight * 0.25
                            val offsetDegrees = (offsetPixels * metersPerPixel) / 111320.0
                            val offsetPoint = GeoPoint(target.lat - offsetDegrees, target.lng)
                            mapView.controller.animateTo(offsetPoint, targetZoom, 2000L)
                        } else {
                            mapView.controller.animateTo(GeoPoint(target.lat, target.lng), targetZoom, 2000L)
                        }
                    }
                }

                pendingBoundsZoom?.let { (sw, ne) ->
                    viewModel.clearPendingBoundsZoom()
                    val boundingBox = org.osmdroid.util.BoundingBox(
                        ne.lat, ne.lng, sw.lat, sw.lng
                    )
                    mapView.post {
                        mapView.zoomToBoundingBox(boundingBox, true, 40)
                        if (mapView.zoomLevelDouble < 10.0) {
                            mapView.controller.setZoom(10.0)
                        }
                        mapView.postDelayed({ mapView.invalidate() }, 500)
                    }
                }

                mapView.overlays.removeAll { it is Marker }
                mapView.overlays.removeAll { it is Polyline }

                // Add polylines before markers so markers render on top
                if (showTracks) {
                    tracks.forEach { hike ->
                        if (hike.track.size >= 2) {
                            val smoothed = smoothTrackPoints(hike.track)
                            // Outline for contrast
                            val outline = Polyline(mapView).apply {
                                outlinePaint.color = android.graphics.Color.parseColor("#80FFFFFF")
                                outlinePaint.strokeWidth = 6f * context.resources.displayMetrics.density
                                outlinePaint.isAntiAlias = true
                                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                                setPoints(smoothed)
                                isVisible = mapView.zoomLevelDouble >= 10.0
                                setOnClickListener { _, _, _ -> false }
                            }
                            mapView.overlays.add(outline)
                            // Main track line
                            val polyline = Polyline(mapView).apply {
                                outlinePaint.color = android.graphics.Color.parseColor("#E0FF7043")
                                outlinePaint.strokeWidth = 4f * context.resources.displayMetrics.density
                                outlinePaint.isAntiAlias = true
                                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                                setPoints(smoothed)
                                isVisible = mapView.zoomLevelDouble >= 10.0
                                setOnClickListener { _, _, _ ->
                                    onTrackClick(hike)
                                    true
                                }
                            }
                            mapView.overlays.add(polyline)
                        }
                    }
                }

                mountains.forEach { mountain ->
                    val isClimbed = climbed[mountain.osmId] == true
                    val hasPhoto = mountain.osmId in photos
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(mountain.lat, mountain.lng)
                        title = "${mountain.chinese} ${mountain.english}"
                        snippet = "${mountain.elevationInt}m"
                        icon = createMarkerDrawable(context, mountain.type, isClimbed, hasPhoto)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ ->
                            onMarkerClick(mountain.osmId)
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            }
        )

        MapOverlay(
            viewModel = viewModel,
            showBaiyue = showBaiyue,
            showXiaobaiyue = showXiaobaiyue,
            showTracks = showTracks,
        )
    }
}

@Composable
internal fun PeakMapPreview(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose {
            mapViewRef.value?.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(false)
                setBuiltInZoomControls(false)
                zoomController.setVisibility(
                    org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                )
                controller.setCenter(GeoPoint(lat, lng))
                controller.setZoom(14.0)
                setOnTouchListener { _, _ -> true }
                mapViewRef.value = this
            }
        },
        update = { mapView ->
            mapView.controller.setCenter(GeoPoint(lat, lng))
        }
    )
}

@Composable
internal fun TrackMapPreview(
    track: List<List<Double>>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose {
            mapViewRef.value?.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(false)
                setBuiltInZoomControls(false)
                zoomController.setVisibility(
                    org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                )
                // Consume touch events so parent scroll isn't disrupted
                setOnTouchListener { _, _ -> true }
                mapViewRef.value = this
            }
        },
        update = { mapView ->
            mapView.overlays.removeAll { it is Polyline }

            if (track.size >= 2) {
                val geoPoints = track.map { pt -> GeoPoint(pt[0], pt[1]) }
                val polyline = Polyline(mapView).apply {
                    outlinePaint.color = android.graphics.Color.parseColor("#FF7043")
                    outlinePaint.strokeWidth = 4f * context.resources.displayMetrics.density
                    outlinePaint.isAntiAlias = true
                    setPoints(geoPoints)
                    isGeodesic = true
                    setOnClickListener { _, _, _ -> false }
                }
                mapView.overlays.add(polyline)

                val minLat = track.minOf { it[0] }
                val maxLat = track.maxOf { it[0] }
                val minLng = track.minOf { it[1] }
                val maxLng = track.maxOf { it[1] }
                val boundingBox = org.osmdroid.util.BoundingBox(
                    maxLat, maxLng, minLat, minLng
                )
                mapView.post {
                    mapView.zoomToBoundingBox(boundingBox, false, 40)
                }
            }
            mapView.invalidate()
        }
    )
}

/**
 * Smooth track points using Catmull-Rom spline interpolation.
 * Inserts interpolated points between each pair for a smoother line.
 */
private fun smoothTrackPoints(track: List<List<Double>>): List<GeoPoint> {
    if (track.size < 3) return track.map { GeoPoint(it[0], it[1]) }

    val result = mutableListOf<GeoPoint>()
    val n = track.size
    val segments = 4 // interpolated points per segment

    for (i in 0 until n - 1) {
        val p0 = track[(i - 1).coerceAtLeast(0)]
        val p1 = track[i]
        val p2 = track[(i + 1).coerceAtMost(n - 1)]
        val p3 = track[(i + 2).coerceAtMost(n - 1)]

        for (s in 0 until segments) {
            val t = s.toFloat() / segments
            val t2 = t * t
            val t3 = t2 * t
            val lat = 0.5f * (
                (2 * p1[0]) +
                (-p0[0] + p2[0]) * t +
                (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2 +
                (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3
            )
            val lng = 0.5f * (
                (2 * p1[1]) +
                (-p0[1] + p2[1]) * t +
                (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2 +
                (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3
            )
            result.add(GeoPoint(lat, lng))
        }
    }
    // Add the last point
    result.add(GeoPoint(track.last()[0], track.last()[1]))
    return result
}

private fun createMarkerDrawable(
    context: android.content.Context,
    type: MountainType,
    isClimbed: Boolean,
    hasPhoto: Boolean
): android.graphics.drawable.Drawable {
    val color = when {
        type == MountainType.BAIYUE && isClimbed -> android.graphics.Color.parseColor("#2A81CB")
        type == MountainType.BAIYUE -> android.graphics.Color.parseColor("#003B6F")
        isClimbed -> android.graphics.Color.parseColor("#728224")
        else -> android.graphics.Color.parseColor("#436436")
    }

    val density = context.resources.displayMetrics.density
    val width = (30 * density).toInt()
    val height = (42 * density).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = width / 2f
    val circleRadius = width / 2f - 2 * density
    val circleY = cx // center of the circle part
    val pinTipY = height - 2 * density

    // Draw teardrop pin shape: circle + triangle tip
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2 * density
    }

    // Teardrop path: arc for the top circle, lines to the bottom point
    val path = Path().apply {
        // The angle where the tangent lines from the tip touch the circle
        val tangentAngle = Math.toDegrees(
            Math.asin((circleRadius / (pinTipY - circleY)).toDouble())
        ).toFloat()
        // Arc from bottom-right to bottom-left (going over the top)
        addArc(
            cx - circleRadius, circleY - circleRadius,
            cx + circleRadius, circleY + circleRadius,
            90f - tangentAngle,
            -(360f - 2 * tangentAngle)
        )
        // Line to pin tip and back
        lineTo(cx, pinTipY)
        close()
    }

    canvas.drawPath(path, fillPaint)
    canvas.drawPath(path, strokePaint)

    // Draw icon inside the circle
    val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    val iconSize = circleRadius * 1.1f

    when {
        isClimbed && hasPhoto -> drawCameraIcon(canvas, cx, circleY, iconSize, iconPaint)
        isClimbed -> drawCheckIcon(canvas, cx, circleY, iconSize, iconPaint)
        else -> drawMountainIcon(canvas, cx, circleY, iconSize, iconPaint)
    }

    return BitmapDrawable(context.resources, bitmap)
}

private fun drawCheckIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
    val strokePaint = Paint(paint).apply {
        style = Paint.Style.STROKE
        strokeWidth = size * 0.18f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val half = size * 0.4f
    val path = Path().apply {
        moveTo(cx - half * 0.7f, cy + half * 0.05f)
        lineTo(cx - half * 0.1f, cy + half * 0.55f)
        lineTo(cx + half * 0.8f, cy - half * 0.55f)
    }
    canvas.drawPath(path, strokePaint)
}

private fun drawMountainIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
    val half = size * 0.45f
    val path = Path().apply {
        // Main peak
        moveTo(cx, cy - half * 0.7f)
        lineTo(cx + half, cy + half * 0.5f)
        lineTo(cx - half, cy + half * 0.5f)
        close()
    }
    canvas.drawPath(path, paint)
}

private fun drawCameraIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
    val strokePaint = Paint(paint).apply {
        style = Paint.Style.STROKE
        strokeWidth = size * 0.12f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val w = size * 0.45f
    val h = size * 0.32f
    // Camera body
    canvas.drawRoundRect(
        cx - w, cy - h * 0.5f, cx + w, cy + h * 0.9f,
        size * 0.06f, size * 0.06f,
        strokePaint
    )
    // Lens circle
    canvas.drawCircle(cx, cy + h * 0.2f, h * 0.35f, strokePaint)
    // Viewfinder bump
    val path = Path().apply {
        moveTo(cx - w * 0.35f, cy - h * 0.5f)
        lineTo(cx - w * 0.2f, cy - h * 0.95f)
        lineTo(cx + w * 0.2f, cy - h * 0.95f)
        lineTo(cx + w * 0.35f, cy - h * 0.5f)
    }
    canvas.drawPath(path, strokePaint)
}
