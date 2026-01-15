package com.codelv.inventory.suppliers

import android.util.Log
import androidx.core.text.trimmedLength
import com.codelv.inventory.DataSupplier
import com.codelv.inventory.ImportResult
import com.codelv.inventory.Part
import com.codelv.inventory.cleanUrl
import org.jsoup.nodes.Document

class LCSC: DataSupplier(name="LCSC", requiresJs=false) {
    override fun searchUrl(q: String): String {
        return "https://www.lcsc.com/search?q=${q}"
    }
    override fun isProductPage(url: String) : Boolean {
        return url.contains("lcsc.com/product-detail/")
    }

    override suspend fun importPartData(part: Part, doc: Document, overwrite: Boolean) : ImportResult {
        val tag = "LCSC"
        var result = false

        if (part.pictureUrl.isBlank() || overwrite) {
            val node = doc.selectXpath("//meta[@name=\"og:image\"]").first()
            if (node != null && node.hasAttr("content")) {
                part.pictureUrl = cleanUrl(node.attr("content"))
                Log.d(tag, "Imported picture url")
                result = true
            } else {
                Log.d(tag, "No picture found")
            }
        }

        if (part.datasheetUrl.isBlank() || overwrite) {
            var node =
                doc.selectXpath("//td[contains(text(), \"Datasheet\")]/following-sibling::*/a")
                    .first()
            if (node != null && node.hasAttr("href")) {
                part.datasheetUrl = cleanUrl(node.attr("href"))
                Log.d(tag, "Imported datasheet url")
                result = true
            } else {
                Log.d(tag, "No datasheet found")
            }
        }

        if (part.manufacturer.isBlank() || overwrite) {
            var node = doc.selectXpath("//meta[@name=\"og:product:brand\"]").first()
            if (node != null && node.hasAttr("content")) {
                part.manufacturer = node.attr("content").trim()
                Log.d(tag, "Imported manufacturer")
                result = true
            } else {
                Log.d(tag, "No manufacturer found")
            }
        }

        if (part.sku.isBlank() || overwrite) {
            var node =
                doc.selectXpath("//td[contains(text(), \"LCSC Part #\")]/following-sibling::*/div/span")
                    .first()
            if (node != null && node.hasText()) {
                part.sku = node.text().trim()
                Log.d(tag, "Imported sku")
                result = true
            } else {
                Log.d(tag, "No sku found")
            }
        }

        if (part.description.isBlank() || overwrite) {
            var node = doc.selectXpath("//td[contains(text(), \"Description\")]/following-sibling::*")
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