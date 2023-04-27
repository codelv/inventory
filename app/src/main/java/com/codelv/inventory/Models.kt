package com.codelv.inventory

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.core.text.trimmedLength
import androidx.lifecycle.ViewModel
import androidx.room.*
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

val USER_AGENTS = listOf(
    "Mozilla/5.0 (Linux; Android 10; Redmi Note 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; vivo 1906) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 10; Redmi Note 9S) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; SM-A507FN) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 12; 220333QAG) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; SM-A528B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; arm_64; Android 11; POCO M2 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 YaBrowser/22.11.7.42.00 SA/3 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 10; TECNO KC8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 8.1.0; SM-G610F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.3 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Linux; Android 11; SAMSUNG SM-A5070) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/19.0 Chrome/102.0.5005.125 Mobile Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 15_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Linux; Android 12; BV4900Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; RMX1851) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 7.1.1; SM-C7108) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 10; Redmi Note 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) GSA/258.1.520699392 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Linux; Android 11; Infinix X6812 Build/RP1A.200720.011; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/111.0.5563.116 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 12; M2102J20SG) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 12; RMX2151) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.40 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 12; SM-A217F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 9; Infinix X625C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; SM-A136U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 12; Redmi Note 9 Pro Max) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.4 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Linux; Android 12; M2007J20CG) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; RMX3471) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 14_8_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Linux; Android 12; V2109) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/112.0.5615.46 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Linux; Android 11; M2102J20SI) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 15_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.2 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Linux; Android 12; M2103K19C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.88 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; M2102J20SG) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 10; M2010J19CI) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; Redmi Note 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; SM-A207F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 10; STK-L21) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 10; Redmi Note 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 9; Moto Z3 Play) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Linux; arm_64; Android 10; DNN-LX9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 YaBrowser/23.1.5.90.00 SA/3 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; SM-G781B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; SM-A307GN) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 12; M2101K6G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; SM-A305F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; Redmi Note 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 9; 5202) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 10; Infinix X657C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; SM-A715F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/112.0.5615.46 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Linux; Android 11; Infinix X689C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 12; DCO-LX9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 9; SO-01K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 12; SM-N986N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; M2104K10I) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; M2003J15SC) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.94 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; SM-A525F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; SM-A305GT) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; RMX2061) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; CPH2413) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; RMX3363) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 12; Redmi Note 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; SM-A536U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 12; Infinix X6815B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 12; SM-A127F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 14_7_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Linux; Android 8.1.0; vivo 1814) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; Nokia 7.2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 15_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.6,2 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Linux; Android 13; SM-A042F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; M2101K6P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Android 10; Mobile; rv:102.0) Gecko/102.0 Firefox/102.0",
    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (iPhone13,2; U; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/602.1.50 (KHTML, like Gecko) Version/10.0 Mobile/15E148 Safari/602.1",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.3 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 EdgiOS/110.1587.63 Mobile/15E148 Safari/605.1.15",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/111.0 Mobile/15E148 Safari/605.1.15",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/111.0.5563.72 Mobile/15E148 Safari/604.1"
)
var userAgent = USER_AGENTS.random()

suspend fun fetch(url: String, retries: Int = 3): Document? {
    var doc: Document? = null;
    Log.d("FETCH", "Fetching ${url}")
    withContext(Dispatchers.IO) {
        for (i in 0..max(1, retries))
            try {
                var req = Jsoup.connect(url).userAgent(userAgent).followRedirects(true)
                if (url.contains("digikey")) {
                    req = req.referrer("https://www.digikey.com")
                }
                doc = req.get()
                Log.d("FETCH", "OK!")
                break;
            } catch (e: java.net.SocketTimeoutException) {
                delay(1000)
                userAgent = USER_AGENTS.random()
                Log.d("FETCH", "ERROR: ${e}, retry with new UA..")
            } catch (e: java.lang.Exception) {
                Log.d("FETCH", "ERROR: ${e}")
                break;
            }
    }
    return doc;
}

fun cleanUrl(url: String): String {
    if (!url.startsWith("http")) {
        return "https://${url.trimStart('/')}"
    }
    return url
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }
}

enum class ImportResult {
    Success,
    Error,
    MultipleResults,
    NoData,
}

@Entity(tableName = "parts")
data class Part(
    @PrimaryKey(autoGenerate = true) var id: Int,
    var name: String = "",
    var mpn: String = "",
    var sku: String = "", // Supplier part number
    var manufacturer: String = "",
    var description: String = "",
    var supplier: String = "",
    var order_number: String = "",
    var datasheetUrl: String = "",
    var pictureUrl: String = "",
    var location: String = "",
    var unit_price: Double = 0.0,
    var total_amount: Double = 0.0,
    var num_ordered: Int = 0,
    var num_in_stock: Int = 0,
    val created: Date = Date(),
    var updated: Date = Date(),
) {
    fun isValid(): Boolean {
        return mpn.length > 0
    }

    fun digikeyUrl(): String {
        val k = URLEncoder.encode(if (sku.trimmedLength() > 2) sku else mpn, "utf-8")
        return "https://www.digikey.com/en/products/result?keywords=${k}"
    }

    // Import image, datasheet, and description
    suspend fun importFromDigikey(overwrite: Boolean = false): ImportResult {
        val url = digikeyUrl();
        if (url.isNotBlank()) {
            try {
                var result: Boolean = false;
                var doc = fetch(url);
                if (doc != null) {
                    if (doc.selectXpath("//div[@data-testid=\"category-page\"]")
                        .first() != null) {
                        return ImportResult.MultipleResults
                    }

                    if (pictureUrl.trimmedLength() == 0 || overwrite) {
                        val img =
                            doc.selectXpath("//*[@data-testid=\"carousel-main-image\"]//img")
                                .first()
                        if (img != null && img.hasAttr("src")) {
                            this.pictureUrl = cleanUrl(img.attr("src"))
                            Log.d("DIGIKEY", "Imported picture url")
                            result = true;
                        } else {
                            Log.d("DIGIKEY", "No picture found")
                        }
                    }

                    if (datasheetUrl.trimmedLength() == 0 || overwrite) {
                        val datasheet =
                            doc.selectXpath("//a[@data-testid=\"datasheet-download\"]").first()
                        if (datasheet != null && datasheet.hasAttr("href")) {
                            this.datasheetUrl = cleanUrl(datasheet.attr("href"))
                            Log.d("DIGIKEY", "Imported datasheet url")
                            result = true;
                        } else {
                            Log.d("DIGIKEY", "No datasheet found")
                        }
                    }

                    if (manufacturer.trimmedLength() == 0 || overwrite) {
                        val mfg =
                            doc.selectXpath("//*[@data-testid=\"overview-manufacturer\"]//a")
                                .first()
                        if (mfg != null && mfg.hasText()) {
                            this.manufacturer = mfg.text().trim()
                            Log.d("DIGIKEY", "Imported manufacturer")
                            result = true;
                        } else {
                            Log.d("DIGIKEY", "No manufacturer found")
                        }
                    }

                    if (description.trimmedLength() == 0 || overwrite) {
                        for (div in doc.selectXpath("//*[@data-testid=\"detailed-description\"]/*/div")) {
                            if (div.hasText() && !div.text().startsWith("Detailed")) {
                                this.description = div.text()
                                Log.d("DIGIKEY", "Imported description")
                                result = true;
                                break;
                            }
                        }
                    }
                    return if (result) ImportResult.Success else ImportResult.NoData
                }
            } catch (e: java.lang.Exception) {
                Log.e("Part", e.toString())
            }
        }
        return ImportResult.Error
    }
}

@Entity(tableName = "scans")
data class Scan(
    @PrimaryKey(autoGenerate = true) var id: Int,
    var value: String = "",
    val created: Date = Date(),
) {
    @delegate:Ignore
    val valueBytes: String by lazy {
        var bytes = "";
        for (c in value) {
            when (c.code) {
                in 48..57 -> bytes += "${c}," // 0-9
                in 97..122 -> bytes += "${c}," // a-z
                in 65..90 -> bytes += "${c}," // A-Z
                else -> bytes += "${c} (${c.code}),"
            }
        }
        bytes
    }

    @delegate:Ignore
    val part: Part? by lazy {

        var result: Part? = null;
        if ((value.startsWith("[)>\u001E06") || value.startsWith("0[)>\u001E06")) && value.length > 10) {
            result = parseTrackingQrcode();
        } else if (value.startsWith("{") && value.length > 10 && value.endsWith("}")) {
            result = parseJsonQrcode();
        }
        result
    }

    fun isValid(): Boolean {
        return part != null
    }


    // Digikey format
    // [[)>06, P3191-E2JFCT-ND, 1PE2JF, K, 1K80853959, 10K96724994, 11K1, 4LCN, Q1000, 11ZPICK, 12Z13912711, 13Z828015, 20Z000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000]
    // TI format
    // [[)>06, P, 1PSN65LVDS33PW, 6P, 2PB, Q30, V0033317, 1T1409142ZFD, 4WTKY, D2245+5, 31T2917413TW2, 20LTID, 21LDEU, 22LTAI, 23LTWN, EG4, 3Z1/260C/UNLIM;//;121722, L1285, 7K, N00]
    fun parseTrackingQrcode(): Part? {
        var entries = value.split(29.toChar())
        // Log.d("Scan", "Entries ${entries}")
        var part = Part(id = 0);
        var qtyIndex = 0;

        for ((i, p) in entries.withIndex()) {
            if (p.startsWith("1P") && p.length > 2) {
                part.mpn = p.substring(2) // MPN
            } else if (p.startsWith("Q") && p.length > 1) {
                try {
                    val qty = Integer.parseUnsignedInt(p.substring(1));
                    qtyIndex = i;
                    part.num_in_stock = qty;
                    part.num_ordered = qty;
                } catch (e: java.lang.NumberFormatException) {
                    // Pass
                }
            } else if (p.startsWith("1K") && p.length > 2) {
                part.order_number = p.substring(2);
            } else if (p.startsWith("P") && p.length > 1) {
                part.sku = p.substring(1);
            }
        }
        if (part.isValid()) {
            if (part.order_number.length > 0 && qtyIndex == 8) {
                part.supplier = "Digikey"
            } else if (value.startsWith("0")) {
                part.supplier = "Nuvoton"
            } else {
                part.supplier = "TI"
            }
            return part;
        }
        return null
    }

    fun parseJsonQrcode(): Part? {
        var entries = value.substring(1, value.length - 1).split(",");
        // Log.d("Scan", "Entries ${entries}")
        var part = Part(id = 0);
        for ((i, p) in entries.withIndex()) {
            val entry = p.split(":");
            if (entry.size == 2) {
                when (entry[0]) {
                    "pm" -> part.mpn = entry[1]
                    "qty" -> {
                        try {
                            val qty = Integer.parseUnsignedInt(entry[1])
                            part.num_in_stock = qty
                            part.num_ordered = qty
                        } catch (e: java.lang.NumberFormatException) {
                            // Pass
                        }
                    }
                    else -> {}
                }
            }
        }

        if (part.isValid()) {
            part.supplier = "LCSC";
            return part;
        }
        return null;
    }
}

//@Dao
//interface Manager<T> {
//    @Insert
//    suspend fun insertAll(vararg items: T)
//
//    @Insert
//    suspend fun insert(item: T)
//
//    @Update
//    suspend fun updateAll(vararg item: T)
//
//    @Update
//    suspend fun update(vararg item: T)
//
//    @Delete
//    suspend fun delete(item: T)
//
//}

// Kotlins type inference seems to suck an cannot
// just use the type at "compile" time so I must repeat and rename everything
@Dao
interface PartManager {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg items: Part): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Part): Long

    @Update
    suspend fun updateAll(vararg item: Part)

    @Update
    suspend fun update(vararg item: Part)

    @Delete
    suspend fun delete(item: Part)

    @Query("SELECT COUNT(id) FROM parts")
    suspend fun totalCount(): Int

    @Query("SELECT * FROM parts ORDER BY -created")
    suspend fun all(): List<Part>

    @Query("SELECT EXISTS(SELECT * FROM parts WHERE mpn = :mpn)")
    suspend fun withMpnExists(mpn: String): Boolean

    @Query("SELECT EXISTS(SELECT * FROM parts WHERE id = :id)")
    suspend fun withIdExists(id: Int): Boolean
}

@Dao
interface ScanManager {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg items: Scan): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Scan): Long

    @Update
    suspend fun updateAll(vararg item: Scan)

    @Update
    suspend fun update(vararg item: Scan)

    @Delete
    suspend fun delete(item: Scan)

    @Query("SELECT * FROM scans ORDER BY -created")
    suspend fun all(): List<Scan>

    @Query("SELECT COUNT(id) FROM scans")
    suspend fun totalCount(): Int

    @Query("SELECT EXISTS(SELECT * FROM scans WHERE id = :id)")
    suspend fun withIdExists(id: Int): Boolean

    @Query("SELECT EXISTS(SELECT * FROM scans WHERE value = :value)")
    suspend fun withValueExists(value: String): Boolean
}

@Database(
    version = 2,
    entities = [Part::class, Scan::class],
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun parts(): PartManager
    abstract fun scans(): ScanManager

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun instance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "inventory.db"
                )
                    .build()
                Companion.instance = db

                db
            }

        }

    }

}

class AppViewModel(val database: AppDatabase) : ViewModel() {
    var parts: MutableList<Part> = mutableStateListOf();
    var scans: MutableList<Scan> = mutableStateListOf();
    var scanOptions: ScanOptions = ScanOptions();

    init {
        scanOptions.setOrientationLocked(false).setBeepEnabled(false)
    }

    suspend fun load() {
        parts.addAll(database.parts().all())
        scans.addAll(database.scans().all())
    }

    suspend fun addScan(scan: Scan): Boolean {
        if (!database.scans().withValueExists(scan.value)) {
            val id = database.scans().insert(scan)
            scan.id = id.toInt();
            scans.add(0, scan)
            Log.d("DB", "Added scan ${scan}")
            return true
        } else {
            Log.w("DB", "Scan with barcode already exists: ${scan.value}")
            return false
        }
    }

    suspend fun removeScan(scan: Scan): Boolean {
        if (scan in scans) {
            scans.remove(scan)
        }
        if (database.scans().withIdExists(scan.id)) {
            database.scans().delete(scan)
            Log.w("DB", "Removed ${scan}")
            return true
        }
        Log.w("DB", "Cannot remove ${scan}, it does not exist in the db")
        return false
    }

    suspend fun addPart(part: Part): Boolean {
        if (!database.parts().withMpnExists(part.mpn)) {
            val id = database.parts().insert(part)
            part.id = id.toInt();
            parts.add(0, part)
            Log.d("DB", "Added part ${part}")
            return true
        } else {
            Log.w("DB", "Part with mpn ${part.mpn} already exists!")
            return false
        }
    }

    suspend fun removePart(part: Part): Boolean {
        if (part in parts) {
            parts.remove(part)
        }
        if (database.parts().withIdExists(part.id)) {
            Log.d("DB", "Removed ${part}")
            database.parts().delete(part)
            return true
        }
        Log.w("DB", "Cannot remove ${part}, it does not exist in the db")
        return false
    }

    suspend fun savePart(part: Part) {
        part.updated = Date()
        database.parts().update(part)
        Log.d("DB", "Saved part ${part}")
    }

}