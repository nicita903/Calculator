package com.example.calculator.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Small vector-style line icons, independent of installed font glyphs. */
@Composable
fun TerminalIcon(index: Int, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val unit = size.width / 24f
        fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(color,
            Offset(x * unit, y * unit), Offset(x2 * unit, y2 * unit), 1.6f * unit)
        when (index) {
            -1 -> { line(3f,6f,21f,6f); line(3f,12f,21f,12f); line(3f,18f,21f,18f) }
            0, 1 -> {
                drawRect(color,Offset(4*unit,2*unit), androidx.compose.ui.geometry.Size(16*unit,20*unit),style=Stroke(1.5f*unit))
                line(7f,7f,17f,7f)
                for(x in listOf(8f,16f)) for(y in listOf(12f,17f)) line(x-1,y,x+1,y)
            }
            2 -> { line(18f,3f,6f,3f);line(6f,3f,13f,12f);line(13f,12f,6f,21f);line(6f,21f,18f,21f) }
            3 -> {
                line(3f,20f,22f,20f);line(4f,2f,4f,21f)
                drawPath(Path().apply { moveTo(5*unit,16*unit); cubicTo(10*unit,-4*unit,15*unit,28*unit,22*unit,5*unit) },color,style=Stroke(1.6f*unit))
            }
            4 -> { drawCircle(color,9*unit,Offset(12*unit,12*unit),style=Stroke(1.6f*unit));line(12f,6f,12f,12f);line(12f,12f,17f,15f) }
            5 -> {
                for(y in listOf(6f,12f,18f)) {line(3f,y,21f,y); drawCircle(color,2*unit,Offset((if(y==12f) 15 else 8)*unit,y*unit))}
            }
            else -> { drawCircle(color,9*unit,Offset(12*unit,12*unit),style=Stroke(1.6f*unit));line(12f,10f,12f,18f);drawCircle(color,unit,Offset(12*unit,6*unit)) }
        }
    }
}
