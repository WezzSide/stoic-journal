package com.stoicjournal.core.common.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    delayMillis: Long = 20,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified
) {
    var textToDisplay by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        textToDisplay = ""
        text.forEach { char ->
            textToDisplay += char
            delay(delayMillis)
        }
    }

    Text(
        text = textToDisplay,
        modifier = modifier,
        style = style,
        color = color
    )
}
