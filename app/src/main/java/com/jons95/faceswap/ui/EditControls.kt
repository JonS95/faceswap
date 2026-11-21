package com.jons95.faceswap.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jons95.faceswap.util.FaceAdjustments

@Composable
fun EditControls(
    adjustments: FaceAdjustments,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdjustmentsChanged: (FaceAdjustments) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(onClick = onToggle) {
                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (expanded) "Hide Adjustments" else "Adjust Face")
            }
            if (expanded && !adjustments.isDefault) {
                TextButton(onClick = onReset) {
                    Icon(Icons.Outlined.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AdjustSlider(
                        label = "Brightness",
                        icon = Icons.Outlined.BrightnessMedium,
                        value = adjustments.brightness,
                        valueRange = -0.5f..0.5f,
                        onValueChange = { onAdjustmentsChanged(adjustments.copy(brightness = it)) }
                    )
                    AdjustSlider(
                        label = "Contrast",
                        icon = Icons.Outlined.Contrast,
                        value = adjustments.contrast,
                        valueRange = 0.5f..1.5f,
                        onValueChange = { onAdjustmentsChanged(adjustments.copy(contrast = it)) }
                    )
                    AdjustSlider(
                        label = "Saturation",
                        icon = Icons.Outlined.Palette,
                        value = adjustments.saturation,
                        valueRange = 0f..2f,
                        onValueChange = { onAdjustmentsChanged(adjustments.copy(saturation = it)) }
                    )
                    AdjustSlider(
                        label = "Warmth",
                        icon = Icons.Outlined.Thermostat,
                        value = adjustments.warmth,
                        valueRange = -1f..1f,
                        onValueChange = { onAdjustmentsChanged(adjustments.copy(warmth = it)) }
                    )
                    AdjustSlider(
                        label = "Blend",
                        icon = Icons.Outlined.Opacity,
                        value = adjustments.blendOpacity,
                        valueRange = 0f..1f,
                        onValueChange = { onAdjustmentsChanged(adjustments.copy(blendOpacity = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    icon: ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp).padding(start = 8.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f)
        )
    }
}
