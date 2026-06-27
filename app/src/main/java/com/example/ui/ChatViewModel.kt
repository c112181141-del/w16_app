package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.ChatDatabase
import com.example.data.ChatMessage
import com.example.data.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository
    val messages: StateFlow<List<ChatMessage>>

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        val chatDao = ChatDatabase.getDatabase(application).chatDao()
        repository = ChatRepository(chatDao)
        messages = repository.allMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    private val SYSTEM_PROMPT = """
你是一款專為淨灘志工設計的智慧聊天小助手（Net-Beach Clean Assistant）。你的目標是提供實用的導航指引、專業的淨灘知識問答（Q&A），並將「維護志工安全」視為最高指導原則。

# 語氣與風格
- 語氣：溫慢、熱情、鼓勵（感謝志工對海洋的付出），同時兼具專業與清晰。
- 安全警示：當涉及生命安全、危險物品或氣候風險時，語氣必須轉為堅定、明確、帶有警示性，絕不模稜兩可。

# 核心功能與指令

## 1. 最高安全提示原則 (Safety-First Guardrails)
無論使用者詢問什麼內容，只要提及「準備出發」、「人在海灘上」、「詢問路線」或特定危險關鍵字，回覆中必須包含對應的安全提醒：
- 【海象與潮汐】：主動提醒「出發前務必查詢中央氣象署當日潮汐表」，嚴禁在漲潮時段或發布瘋狗浪/大浪警報時進行淨灘。
- 【個人裝備】：強力宣導必須穿著「防滑包鞋或安全鞋」（嚴禁拖鞋、涼鞋），並配戴「防刺/耐磨手套」，注意防曬與定時補充水分。
- 【地形風險】：提醒注意高低落差、滑溜青苔、碎石暗礁以及海灘上的漂流木。

## 2. 危險廢棄物處理指南 (Hazardous Waste Protocols)
當使用者詢問某種特定垃圾能不能撿，或拍到不明物品照片時，必須嚴格遵守以下分類指導：
- 【醫療與尖銳廢棄物】（如：針筒、針頭、碎玻璃）：提醒切勿徒手撿拾，必須使用夾子，並放入專用的硬殼容器（如堅固的寶特瓶）內。
- 【有毒與不明化學品】（如：農藥瓶、不明機油桶、有刺鼻味之容器）：嚴禁觸摸、打開或聞嗅！應保持安全距離，並記錄位置通報主辦單位。
- 【大型或動物屍體】（如：漁網纏繞巨石、鯨豚/海龜屍體）：提醒請勿自行搬動以免受傷。鯨豚海龜請指導其撥打海巡署專線「118」通報。

## 3. 導航與位置協助 (Navigation & Location Support)
當使用者詢問某個海灘的交通方式或怎麼去時：
- 提供該海灘的大致地理位置、建議的交通方式（大眾運輸或開車導航地標）。
- 固定提醒：「海邊或偏遠岸際通常手機訊號較弱，出發前請務必先下載離線地圖，並注意最後一公里路況。」
- 鼓勵使用者主動回報或記錄經緯度。

## 4. 淨灘知識問答 (Beach Cleanup Q&A)
- 解答常見垃圾分類（區分：一般垃圾、資源回收、海洋廢棄物特殊去處）。
- 宣導「無痕海洋（Leave No Trace）」理念，提醒不只撿垃圾，也要避免破壞當地的生態（如不踩踏海濱植物、不驚擾潮間帶生物）。

# 格式與限制
- 【排版規範】：大量使用 Markdown 的條列式（*）、粗體（**）、以及區塊引言（>），確保志工在戶外強光下看手機螢幕時，能一眼抓到重點。
- 【結尾強制要求】：任何涉及戶外行動或出發的對話，回覆最後必須獨立一行加上：
  ⚠️ 安全小叮嚀：[根據對話內容，給出一句 20 字內的安全警告]
""".trimIndent()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // 1. Insert user message in database
            val userMsg = ChatMessage(sender = "user", text = text)
            repository.insertMessage(userMsg)

            // 2. Set loading state
            _isLoading.value = true
            _errorMessage.value = null

            // 3. Make the API call in background
            try {
                val responseText = withContext(Dispatchers.IO) {
                    val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                        throw Exception("請先在 AI Studio 的 Secrets 面板中設定您的 GEMINI_API_KEY 以啟用智慧小助手。")
                    }

                    // Build conversation history to feed Gemini (up to last 10 messages for token context efficiency)
                    val historyList = messages.value.takeLast(10).map { msg ->
                        Content(parts = listOf(Part(text = msg.text)))
                    }

                    val request = GenerateContentRequest(
                        contents = historyList,
                        generationConfig = GenerationConfig(temperature = 0.3f), // Lower temperature for high safety and consistency
                        systemInstruction = Content(parts = listOf(Part(text = SYSTEM_PROMPT)))
                    )

                    val response = RetrofitClient.service.generateContent(apiKey, request)
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "小助手目前無法產生回應，請稍後再試。"
                }

                // 4. Insert assistant message
                val assistantMsg = ChatMessage(sender = "assistant", text = responseText)
                repository.insertMessage(assistantMsg)

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "發生未知錯誤"
                val assistantMsg = ChatMessage(
                    sender = "assistant",
                    text = "⚠️ **通訊錯誤**\n\n無法連接至智慧聊天服務，請確認您的網路連線或 API Key 設定。\n\n錯誤詳情：${e.localizedMessage ?: "無"}"
                )
                repository.insertMessage(assistantMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            // Optional: Insert initial greeting from Assistant
            val welcomeMsg = ChatMessage(
                sender = "assistant",
                text = "🌸 **感謝您對海洋環境的付出！**\n\n我是您的**淨灘志工智慧助手**。無論您想：\n* 規劃「準備出發」淨灘的安全事項\n* 詢問特定海灘的「導航與路線」\n* 查詢「危險廢棄物」處置步驟\n* 學習「垃圾分類與無痕海洋」知識\n\n我都會以**志工安全**為第一原則為您解答。您準備要去哪裡淨灘呢？"
            )
            repository.insertMessage(welcomeMsg)
        }
    }

    fun checkAndAddWelcomeMessage() {
        viewModelScope.launch {
            if (messages.value.isEmpty()) {
                val welcomeMsg = ChatMessage(
                    sender = "assistant",
                    text = "🌸 **感謝您對海洋環境的付出！**\n\n我是您的**淨灘志工智慧助手**。無論您想：\n* 規劃「準備出發」淨灘的安全事項\n* 詢問特定海灘的「導航與路線」\n* 查詢「危險廢棄物」處置步驟\n* 學習「垃圾分類與無痕海洋」知識\n\n我都會以**志工安全**為第一原則為您解答。您準備要去哪裡淨灘呢？"
                )
                repository.insertMessage(welcomeMsg)
            }
        }
    }
}

class ChatViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
