/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.aicamera

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Draw the detected object info in preview.  */
class ObjectGraphic constructor(
  overlay: GraphicOverlay,
  private val detectedObject: ObjectDetectionHelper.ObjectPrediction
) : GraphicOverlay.Graphic(overlay) {

  private val numColors = COLORS.size

  private val boxPaints = Array(numColors) { Paint() }
  private val textPaints = Array(numColors) { Paint() }
  private val labelPaints = Array(numColors) { Paint() }

  init {
    for (i in 0 until numColors) {
      textPaints[i] = Paint()
      textPaints[i].color = COLORS[i][0]
      textPaints[i].textSize = TEXT_SIZE
      boxPaints[i] = Paint()
      boxPaints[i].color = COLORS[i][1]
      boxPaints[i].style = Paint.Style.STROKE
      boxPaints[i].strokeWidth = STROKE_WIDTH
      labelPaints[i] = Paint()
      labelPaints[i].color = COLORS[i][1]
      labelPaints[i].style = Paint.Style.FILL
    }
  }

  override fun draw(canvas: Canvas) {
    // Decide color based on object tracking ID
    val colorID =
      if (detectedObject == null) 0
      else Random.nextInt(1, NUM_COLORS-1)
    var textWidth = 0
    val lineHeight = TEXT_SIZE + STROKE_WIDTH
    var yLabelOffset: Float = 0f

    // Calculate width and height of label box
    textWidth =
      textPaints[colorID].measureText(detectedObject.label + " " + detectedObject.score.toString()).toInt()
    yLabelOffset -= 2 * lineHeight

    // Draws the bounding box.
    val rect = RectF(detectedObject.location)
    val x0 = translateX(rect.left) - 200
    val x1 = translateX(rect.right) - 200
//    val x0 = rect.left
//    val x1 = rect.right
    rect.left = min(x0, x1)
    rect.right = max(x0, x1)
    rect.top = translateY(rect.top) + 200
    rect.bottom = translateY(rect.bottom) + 200
//    rect.top = rect.top
//    rect.bottom = rect.bottom
    canvas.drawRect(rect, boxPaints[colorID])

    // Draws other object info.
    canvas.drawRect(
      rect.left - STROKE_WIDTH,
      rect.top + yLabelOffset,
      rect.left + textWidth + 2 * STROKE_WIDTH,
      rect.top,
      labelPaints[colorID]
    )
    yLabelOffset += TEXT_SIZE
    canvas.drawText(
      detectedObject.label + " " + detectedObject.score.toString(),
      rect.left,
      rect.top + yLabelOffset,
      textPaints[colorID]
    )
  }

  companion object {
    private const val TEXT_SIZE = 54.0f
    private const val STROKE_WIDTH = 4.0f
    private const val NUM_COLORS = 10
    private val COLORS =
      arrayOf(
        intArrayOf(Color.BLACK, Color.WHITE),
        intArrayOf(Color.WHITE, Color.MAGENTA),
        intArrayOf(Color.BLACK, Color.LTGRAY),
        intArrayOf(Color.WHITE, Color.RED),
        intArrayOf(Color.WHITE, Color.BLUE),
        intArrayOf(Color.WHITE, Color.DKGRAY),
        intArrayOf(Color.BLACK, Color.CYAN),
        intArrayOf(Color.BLACK, Color.YELLOW),
        intArrayOf(Color.WHITE, Color.BLACK),
        intArrayOf(Color.BLACK, Color.GREEN)
      )
//    private const val LABEL_FORMAT = "%.2f%% confidence (index: %d)"
    private const val LABEL_FORMAT = "%.2f%%"
  }
}
