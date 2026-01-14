package com.codelv.inventory.suppliers

import android.util.Log
import androidx.core.text.trimmedLength
import com.codelv.inventory.DataSupplier
import com.codelv.inventory.ImportResult
import com.codelv.inventory.Part
import com.codelv.inventory.cleanUrl
import org.jsoup.nodes.Document

class Digikey: DataSupplier(requiresJs=true) {
    override fun matchesName(name: String): Boolean {
        return name.lowercase() == "digikey"
    }

    override fun searchUrl(q: String): String {
        return "https://www.digikey.com/en/products/result?keywords=${q}"
    }

    override fun isProductPage(url: String, content: String) : Boolean {
        return url.contains("digikey.com/") && url.contains("/products/detail/")
    }

    override fun requestHeaders() : Map<String, String> {
        return mapOf(
            "Referer" to "https://www.digikey.com",
            "Accept-Language" to "en-US,en"
        )
    }

    override suspend fun importPartData(part: Part, doc: Document, overwrite: Boolean) : ImportResult {
        val tag = "Digikey"
        var result = false

        // TODO: Detect "Were a fan of robots"
        if (doc.selectXpath("//div[@data-testid=\"category-page\"]")
                .first() != null
        ) {
            return ImportResult.MultipleResults
        }

        if (part.pictureUrl.trimmedLength() == 0 || overwrite) {
            val img =
                doc.selectXpath("//*[@data-testid=\"carousel-main-image\"]//img")
                    .first()
            if (img != null && img.hasAttr("src")) {
                part.pictureUrl = cleanUrl(img.attr("src"))
                Log.d(tag, "Imported picture url")
                result = true
            } else {
                Log.d(tag, "No picture found")
            }
        }

        if (part.datasheetUrl.trimmedLength() == 0 || overwrite) {
            val datasheet =
                doc.selectXpath("//a[@data-testid=\"datasheet-download\"]").first()
            if (datasheet != null && datasheet.hasAttr("href")) {
                part.datasheetUrl = cleanUrl(datasheet.attr("href"))
                Log.d(tag, "Imported datasheet url")
                result = true
            } else {
                Log.d(tag, "No datasheet found")
            }
        }

        if (part.manufacturer.trimmedLength() == 0 || overwrite) {
            val mfg =
                doc.selectXpath("//*[@data-testid=\"overview-manufacturer\"]//a")
                    .first()
            if (mfg != null && mfg.hasText()) {
                part.manufacturer = mfg.text().trim()
                Log.d(tag, "Imported manufacturer")
                result = true
            } else {
                Log.d(tag, "No manufacturer found")
            }
        }

        if (part.description.trimmedLength() == 0 || overwrite) {
            for (div in doc.selectXpath("//*[@data-testid=\"detailed-description\"]/*/div")) {
                if (div.hasText() && !div.text().startsWith("Detailed")) {
                    part.description = div.text()
                    Log.d(tag, "Imported description")
                    result = true
                    break
                }
            }
        }
        return if (result) ImportResult.Success else ImportResult.NoData
    }
}