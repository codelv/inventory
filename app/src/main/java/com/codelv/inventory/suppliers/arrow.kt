package com.codelv.inventory.suppliers

import android.util.Log
import androidx.core.text.trimmedLength
import com.codelv.inventory.DataSupplier
import com.codelv.inventory.ImportResult
import com.codelv.inventory.Part
import com.codelv.inventory.cleanUrl
import org.jsoup.nodes.Document

class Arrow: DataSupplier(name="Arrow", requiresJs = true) {
    override fun searchUrl(q: String): String {
        return "https://www.arrow.com/en/products/search?q=${q}"
    }

    override fun isProductPage(url: String) : Boolean {
        return url.contains("arrow.com/") && !url.contains("/search")
                && url.contains("/products/")
    }

    override fun requestHeaders() : Map<String, String> {
        return mapOf(
            "Referer" to "https://www.arrow.com",
            "Accept-Language" to "en-US,en"
        )
    }
    override suspend fun importPartData(part: Part, doc: Document, overwrite: Boolean) : ImportResult {
        val tag = "Arrow"
        var result = false

        if (part.pictureUrl.isBlank() || overwrite) {
            val img =
                doc.selectXpath("//img[@class=\"Product-Summary-Image\"]")
                    .first()
            if (img != null && img.hasAttr("src")) {
                part.pictureUrl = cleanUrl(img.attr("src"))
                Log.d(tag, "Imported picture url")
                result = true
            } else {
                Log.d(tag, "No picture found")
            }
        }

        if (part.datasheetUrl.isBlank() || overwrite) {
            val a =
                doc.selectXpath("//a[contains(@class, \"DatasheetViewer-downloadButton\")]").first()
            if (a != null && a.hasAttr("href")) {
                part.datasheetUrl = cleanUrl(a.attr("href"))
                Log.d(tag, "Imported datasheet url")
                result = true
            } else {
                Log.d(tag, "No datasheet found")
            }
        }

        if (part.manufacturer.isBlank() || overwrite) {
            val node =
                doc.selectXpath("//*[@class=\"Product-SimplifiedSummary-SubHeading-Manufacturer\"]")
                    .first()
            if (node != null && node.hasText()) {
                part.manufacturer = node.text().trim()
                Log.d(tag, "Imported manufacturer")
                result = true
            } else {
                Log.d(tag, "No manufacturer found")
            }
        }

        if (part.description.isBlank() || overwrite) {
            val node =
                doc.selectXpath("//*[@class=\"Product-Summary-Description\"]")
                    .first()

            if (node != null && node.hasText()) {
                part.description = node.text().trim()
                Log.d(tag, "Imported description")
                result = true
            } else {
                Log.d(tag, "No description found")
            }
        }
        return if (result) ImportResult.Success else ImportResult.NoData
    }
}