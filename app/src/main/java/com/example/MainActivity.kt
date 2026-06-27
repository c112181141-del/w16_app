package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ChatMessage
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

// Data class representing beach coordinates and details
data class BeachInfo(
    val name: String,
    val location: String,
    val transit: String,
    val latitude: Double,
    val longitude: Double
)

val PRESET_BEACHES = listOf(
    BeachInfo(
        name = "基隆 外木山海灘",
        location = "基隆市中山區協和街旁岸際（近外木山濱海風景區）",
        transit = "搭乘台鐵至基隆站後轉搭公車305至「外木山站」；或開車導航至外木山海濱步道旁停車場。",
        latitude = 25.1636,
        longitude = 121.7242
    ),
    BeachInfo(
        name = "新北 萬里海灘",
        location = "新北市萬里區獅子公園至下寮沙灘岸際",
        transit = "搭乘捷運至淡水站轉搭淡水客運862、863至「萬里沙灘站」；或開車沿台二線往金山方向導航萬里。",
        latitude = 25.1764,
        longitude = 121.6895
    ),
    BeachInfo(
        name = "新北 石門白沙灣",
        location = "新北市石門區德茂里下員坑（北海岸國家風景區內）",
        transit = "捷運淡水站搭乘淡水客運862、863至「白沙灣站」；開車經台二線約23公里處即可到達。",
        latitude = 25.2842,
        longitude = 121.5074
    ),
    BeachInfo(
        name = "高雄 旗津海水浴場",
        location = "高雄市旗津區廟前路1號對面沙灘",
        transit = "搭乘高雄捷運至西子灣站，步行至鼓山輪渡站搭乘渡輪至旗津，出站後步行沿廟前路走到底。",
        latitude = 22.6135,
        longitude = 120.2687
    ),
    BeachInfo(
        name = "台中 高美濕地",
        location = "台中市清水區大甲溪出海口南側（高美野生動物保護區）",
        transit = "搭乘台鐵至清水站轉搭公車178、179至「高美濕地站」；開車經西濱快速道路（台61）下清水交流道。",
        latitude = 24.3117,
        longitude = 120.5489
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(application)
    )

    // Trigger initial welcome message if empty
    LaunchedEffect(Unit) {
        viewModel.checkAndAddWelcomeMessage()
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("💬 智慧 AI 諮詢", "🛡️ 離線安全指引")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🌊 淨灘安全助手",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearHistory() },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "清空聊天紀錄",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OceanDeepBlue
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(BackgroundLight)
        ) {
            // Tab Header with custom design
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = OceanDeepBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = OceanDeepBlue
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
                                )
                            )
                        },
                        modifier = Modifier.testTag("tab_$index")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (selectedTab == 0) {
                    ChatTab(viewModel)
                } else {
                    SafetyGuideTab()
                }
            }
        }
    }
}

@Composable
fun ChatTab(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var inputText by remember { mutableStateOf("") }

    // Scroll to bottom when messages list size changes or loading completes
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestions = listOf(
        "準備出發：我想到外木山淨灘" to "準備出發：我想去基隆外木山淨灘，需要注意什麼？",
        "危險處置：沙灘上有針筒！" to "在沙灘上撿到針筒、碎玻璃等醫療尖銳廢棄物要怎麼處理？",
        "導航路线：萬里海灘交通" to "我想問萬里海灘要怎麼去？交通方式與定位？",
        "生態保護：發現擱淺海龜" to "如果發現沙灘上有漁網纏繞的巨石，或是看到受傷擱淺的鯨豚/海龜，該打什麼電話或如何通報？",
        "垃圾分類：保麗龍哪一類？" to "保麗龍、寶特瓶等海洋垃圾屬於一般垃圾還是回收？無痕海洋是什麼？"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat messages area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }

            if (isLoading) {
                item {
                    TypingIndicator()
                }
            }
        }

        // Suggestions Horizontal Row
        if (messages.size <= 2 && !isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "💡 志工常用問題快速提問：",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = OceanTeal
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions) { (label, promptText) ->
                        SuggestionChip(
                            label = label,
                            onClick = {
                                viewModel.sendMessage(promptText)
                            }
                        )
                    }
                }
            }
        }

        // Text input row with edge to edge navigation protection
        Surface(
            tonalElevation = 2.dp,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("請輸入您的問題（如：準備出發外木山）...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !isLoading) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                                keyboardController?.hide()
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                            keyboardController?.hide()
                        }
                    },
                    containerColor = OceanDeepBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "發送訊息"
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MintIce,
        contentColor = OceanTeal,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.heightIn(min = 36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = OceanTeal
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.sender == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        // Sender Header Label with Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
        ) {
            Icon(
                imageVector = if (isUser) Icons.Default.Person else Icons.Default.Face,
                contentDescription = null,
                tint = if (isUser) OceanTeal else SeafoamGreen,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isUser) "志工夥伴" else "安全守護助理",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) OceanTeal else OceanDeepBlue
                )
            )
        }

        // Bubble Content Card
        Surface(
            color = if (isUser) SurfaceBubbleUser else SurfaceBubbleAI,
            tonalElevation = if (isUser) 1.dp else 3.dp,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 16.dp
            ),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .testTag("message_bubble_${message.sender}")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (isUser) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextDark,
                            lineHeight = 22.sp
                        )
                    )
                } else {
                    MarkdownFormatter(text = message.text)
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = Color.White,
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp),
            modifier = Modifier.widthIn(max = 240.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = OceanTeal
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "守護小助手正在思考安全回覆...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// Custom parser to split lines and format them properly according to markdown constraints
@Composable
fun MarkdownFormatter(text: String) {
    val lines = text.split("\n")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                continue
            }

            when {
                // Warning Notification alert: begins with ⚠️
                trimmed.startsWith("⚠️") -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = AlertAmber.copy(alpha = 0.15f),
                            contentColor = AlertOrange
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "安全警告",
                                tint = AlertOrange,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = parseMarkdownToAnnotatedString(trimmed, highlightColor = AlertOrange),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }
                }

                // Blockquotes: begins with >
                trimmed.startsWith(">") -> {
                    val quoteText = trimmed.removePrefix(">").trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .heightIn(min = 20.dp)
                                .background(color = OceanTeal, shape = RoundedCornerShape(2.dp))
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseMarkdownToAnnotatedString(quoteText, highlightColor = OceanTeal),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = OceanTeal,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }

                // Bullet Lists: begins with * or -
                trimmed.startsWith("*") || trimmed.startsWith("-") -> {
                    val listContent = trimmed.substring(1).trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = OceanTeal,
                                fontWeight = FontWeight.Black
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseMarkdownToAnnotatedString(listContent, highlightColor = OceanDeepBlue),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextDark,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }

                // Regular lines (normal paragraph)
                else -> {
                    Text(
                        text = parseMarkdownToAnnotatedString(trimmed, highlightColor = OceanTeal),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextDark,
                            lineHeight = 22.sp
                        )
                    )
                }
            }
        }
    }
}

// Sub-parser for inline **bold text**
fun parseMarkdownToAnnotatedString(text: String, highlightColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = Regex("\\*\\*(.*?)\\*\\*")
        val matches = regex.findAll(text)
        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > cursor) {
                append(text.substring(cursor, start))
            }
            pushStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = highlightColor
                )
            )
            append(match.groupValues[1])
            pop()
            cursor = end
        }
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}

@Composable
fun SafetyGuideTab() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Offline state variables
    var selectedBeachIndex by remember { mutableStateOf(0) }
    val selectedBeach = PRESET_BEACHES[selectedBeachIndex]

    // Pre-departure checklist state
    var checkTides by remember { mutableStateOf(false) }
    var checkShoes by remember { mutableStateOf(false) }
    var checkGloves by remember { mutableStateOf(false) }
    var checkWater by remember { mutableStateOf(false) }
    var checkMap by remember { mutableStateOf(false) }
    var checkPhone by remember { mutableStateOf(false) }

    val allChecked = checkTides && checkShoes && checkGloves && checkWater && checkMap && checkPhone

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Section A: Pre-departure Checklist
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SeafoamGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "📝 安全出發準備檢查表",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OceanDeepBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "為了保障您在海灘上的生命與財產安全，出發前請逐項確認完成：",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ChecklistItem(
                        text = "確認海象與當日潮汐表 (嚴禁漲潮/發布大浪警報時淨灘)",
                        checked = checkTides,
                        onCheckedChange = { checkTides = it }
                    )
                    ChecklistItem(
                        text = "穿著防滑包鞋或安全鞋 (嚴禁穿拖鞋、涼鞋防割傷)",
                        checked = checkShoes,
                        onCheckedChange = { checkShoes = it }
                    )
                    ChecklistItem(
                        text = "配戴耐磨/防刺手套 (拾取垃圾避免被針頭、玻璃刺傷)",
                        checked = checkGloves,
                        onCheckedChange = { checkGloves = it }
                    )
                    ChecklistItem(
                        text = "備足飲用水並擦防曬 (注意水分補充與防中暑)",
                        checked = checkWater,
                        onCheckedChange = { checkWater = it }
                    )
                    ChecklistItem(
                        text = "下載離線地圖 (沿岸可能訊號不佳，預防迷失最後一哩路)",
                        checked = checkMap,
                        onCheckedChange = { checkMap = it }
                    )
                    ChecklistItem(
                        text = "攜帶充飽電之行動電話與行動電源 (隨時保持通聯備用)",
                        checked = checkPhone,
                        onCheckedChange = { checkPhone = it }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedVisibility(
                        visible = allChecked,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            color = MintIce,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = SeafoamGreen
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "✅ 檢查完成！小助手預祝您安全、平安順利！",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = OceanTeal,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section B: Offline Location & SOS Reporter
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = OceanTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🛰️ 偏遠岸際 GPS 位置通報器",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OceanDeepBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "選擇目的地：",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Beach Selection Row
                    Box(modifier = Modifier.fillMaxWidth()) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedBeach.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = OceanDeepBlue
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "展開下拉選單",
                                    tint = OceanTeal
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            PRESET_BEACHES.forEachIndexed { index, beach ->
                                DropdownMenuItem(
                                    text = { Text(beach.name, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedBeachIndex = index
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Location Details Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "📍 岸際位置：",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = selectedBeach.location,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "🚗 交通指引：",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = selectedBeach.transit,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "🛰️ 衛星 GPS 經緯度座標：",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Surface(
                                color = MintIce,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${selectedBeach.latitude}, ${selectedBeach.longitude}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = OceanTeal
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(
                                                AnnotatedString("${selectedBeach.latitude}, ${selectedBeach.longitude}")
                                            )
                                            Toast.makeText(context, "經緯度座標已複製！", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "複製經緯度",
                                            tint = OceanTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone Dial and Signal warning
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = AlertAmber.copy(alpha = 0.12f),
                            contentColor = AlertOrange
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚠️ 海邊或偏遠岸際通常手機訊號較弱，出發前請務必先下載離線地圖，並注意最後一公里路況。",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Coast Guard Emergency SOS Dial Button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:118")
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "撥打海巡署「118」通報 (鯨豚海龜擱淺或緊急事件)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section C: Hazardous Waste Manual
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AlertOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "☣️ 危險廢棄物速查處置手冊",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OceanDeepBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HazardGuideItem(
                        title = "🔴 醫療與尖銳廢棄物 (針筒、碎玻璃)",
                        icon = Icons.Default.Warning,
                        iconColor = DangerRed,
                        description = "*   **切勿徒手撿拾**：尖銳針頭極易刺穿皮膚，恐有感染風險。\n*   **使用工具**：一律使用不鏽鋼長夾、手套拾取。\n*   **硬殼密封**：拾取後必須放入專用的硬殼容器（如堅固的厚寶特瓶或大開口瓶），並旋緊瓶蓋妥善防護。"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    HazardGuideItem(
                        title = "🟡 有毒與不明化學品 (農藥瓶、油桶)",
                        icon = Icons.Default.Info,
                        iconColor = AlertAmber,
                        description = "*   **嚴禁觸摸與聞嗅**：農藥瓶、化學機油桶可能殘留劇毒或強酸鹼。\n*   **嚴禁打開容器**：切勿打開或使氣體逸散，防吸入中毒。\n*   **記錄通報**：保持安全距離，拍照並記錄 GPS 經緯度，迅速通知現場主辦單位或專責環保局通報處理。"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    HazardGuideItem(
                        title = "🔵 大型與動物屍體 (廢漁網、鯨豚)",
                        icon = Icons.Default.Info,
                        iconColor = OceanTeal,
                        description = "*   **切勿自行搬動**：遭巨石或重物纏繞的廢棄漁網極沉重，易拉傷或絆倒。\n*   **保育與擱淺通報**：若發現受傷擱淺之鯨豚、海龜或海洋保育類動物屍體，**請立即撥打海巡署專線「118」通報**，切勿破壞現場。"
                    )
                }
            }
        }
    }
}

@Composable
fun ChecklistItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = OceanTeal,
                uncheckedColor = Color.Gray
            ),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
                color = if (checked) Color.Gray else TextDark
            )
        )
    }
}

@Composable
fun HazardGuideItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    description: String
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundLight),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = OceanDeepBlue
                        )
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收合" else "展開",
                    tint = OceanTeal
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    MarkdownFormatter(text = description)
                }
            }
        }
    }
}
