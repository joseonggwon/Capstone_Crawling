package com.example.crawling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.crawling.ui.theme.CrawlingTheme
import kotlinx.coroutines.*
import org.jsoup.Jsoup

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CrawlingTheme {
                var crawledData by remember { mutableStateOf("Loading...") }

                LaunchedEffect(Unit) {
                    crawledData = crawl()
                }

                DisplayHtml(crawledData)
            }
        }
    }

    private suspend fun crawl(): String = withContext(Dispatchers.IO) {
        val searchSite = "https://prod.danawa.com/list/?cate="

        //tv = 1022811 (단위 W), 전자레인지 = 10338815 (단위 W), 냉장고 = 10251508 (단위 kWh(월)), 에어컨 = 1022644(단위 : kW)

        //보일러 = 10330122(소비전력이라는 단어가 없어서 따로 해야할 듯)
        //세탁기 = 10244107(드럼세탁기 코드, 통돌이는 10244730, 세탁기는 소비전력이 없고 등급만 적혀있어서 시뮬레이션 불가)

        val productCode = "10330122"
        val document = Jsoup.connect(searchSite + productCode).get()

        val onlyProduct = document.select("li.prod_item.prod_layer")
        val onlyProductString = onlyProduct.joinToString(" ") { it.text() }

        val productsName = onlyProduct.select("a[name='productName']").map { it.text() }
        val productPowers = if (productCode == "10330122") {
            emptyList<Double>()
        }
        else {
            extractPowers(onlyProductString)
        }
        val productPrice = extractPrices(onlyProductString)

        val productInfoList = List(productsName.size) { index ->
            ProductInfo(
                name = productsName.getOrNull(index) ?: "Unknown",
                power = productPowers.getOrNull(index) ?: 0,
                price = productPrice.getOrNull(index) ?: 0
            )
        }

        return@withContext productInfoList.joinToString(separator = "\n") {
            "Name: ${it.name}, Power: ${it.power}, Price: ${it.price}"
        }
    }

    fun extractPowers(text: String): List<Double> {
        val powers = mutableListOf<Double>()
        val regex = "\\d+(\\.\\d+)?(?=(W|kW|kWh\\(월\\)))".toRegex()

        val splitText = text.split("소비전력").drop(1)

        for (part in splitText) {
            val matchResult = regex.find(part)
            matchResult?.let {
                val power = it.value.toDouble()
                powers.add(power)
            }
        }

        return powers
    }


    fun extractPrices(text: String): List<Int> {
        val prices = mutableListOf<Int>()
        val regex = "\\d{1,3}(,\\d{3})*원".toRegex()

        val splitText = text.split("소비전력")

        for (part in splitText) {
            val matchResult = regex.find(part)
            matchResult?.let {
                val price = it.value.replace("[^\\d]".toRegex(), "").toInt()
                prices.add(price)
            }
        }

        return prices
    }

    data class ProductInfo(
        val name: String,
        val power: Number,
        val price: Int
    )
}

@Composable
fun DisplayHtml(htmlContent: String, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Text(
        text = htmlContent,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    )
}

@Preview(showBackground = true)
@Composable
fun DisplayHtmlPreview() {
    CrawlingTheme {
        DisplayHtml("HTML content goes here")
    }
}
