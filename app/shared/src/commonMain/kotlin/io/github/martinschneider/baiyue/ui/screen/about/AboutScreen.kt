package io.github.martinschneider.baiyue.ui.screen.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FaqItem(val question: String, val answer: String)

private val faqItems = listOf(
    FaqItem(
        "What is the purpose of this project?",
        "There are many online resources about hiking in Taiwan, but English-language information (especially about the Xiaobaiyue) is still limited and often incomplete. This project aims to create an easy-to-use bucket list that also provides a starting point to plan a trip to each 百岳 Baiyue and 小百岳 Xiaobaiyue."
    ),
    FaqItem(
        "How to use this app?",
        "The map shows all peaks in Taiwan. 百岳 peaks are marked in blue, 小百岳 in green. Tap on a marker to see details, navigate to the peak, or mark it as visited. You can also attach your summit photo. Use the list view to browse all peaks and track your progress."
    ),
    FaqItem(
        "Where is the progress stored? Is my data secure?",
        "All information is stored locally on your device. Hiking progress and photos are never uploaded to any server. You can use the backup & restore functions to copy your data between devices or transfer it to/from the web app."
    ),
    FaqItem(
        "What is the history of the 百岳?",
        "The 1964 book \"100 Famous Japanese Mountains\" influenced Taiwanese hiking legend Wen-An Lin to compile a similar list of mountains in Taiwan. Together with other local mountaineers, he selected 100 peaks known at the time to be above 3000 m. The peaks were chosen by criteria like uniqueness, danger, height, beauty and prominence. The 百岳 list was released in 1971 and has since become a bucket list for many Taiwanese hikers."
    ),
    FaqItem(
        "What is the history of the 小百岳?",
        "The Sports Committee of Taiwan identified 100 entry-level hikes to promote national mountaineering. These peaks are known as the 小百岳, Taiwan's 100 \"little\" peaks and a first list was released in 2003."
    ),
    FaqItem(
        "Are all 百岳 over 3000 meters?",
        "They were supposed to be. However, 鹿山 Lushan has since been re-surveyed to be \"only\" 2981 meters high. It has been kept on the list, regardless."
    ),
    FaqItem(
        "Why are there more than 100 小百岳?",
        "In contrast to the Baiyue, the Xiaobaiyue list has been updated several times, and peaks have been replaced for various reasons (for example, difficulty of access). This app displays all versions of the list."
    ),
    FaqItem(
        "Where does the data come from?",
        "The information has been taken from the Chinese Wikipedia pages for the 百岳 and 小百岳 and the primary sources mentioned there. Elevation data is taken from OpenStreetMap (OSM)."
    ),
    FaqItem(
        "How are the English translations of the peaks chosen?",
        "The naming pattern uses the Hanyu Pinyin transliteration of the Chinese name while keeping 山 shan untranslated, for example, 七星山 is written as Qixingshan instead of Mt. Qixing, Mt. Cising, Mt. Chihsing or Seven Star Mountain. If there are multiple peaks (North, South, East, West etc.), that distinction is translated into English, such as Yushan East Peak."
    ),
    FaqItem(
        "How can I report an error or a problem?",
        "Please email xiaobaiyue@5164.at. I'm looking forward to hearing from you."
    ),
    FaqItem(
        "Have you climbed all the peaks?",
        "No, but I'm enjoying the journey."
    )
)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text("About", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        faqItems.forEach { item ->
            FaqAccordion(item)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FaqAccordion(item: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    item.question,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Text(
                    item.answer,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(
                        start = 16.dp, end = 16.dp, bottom = 16.dp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
