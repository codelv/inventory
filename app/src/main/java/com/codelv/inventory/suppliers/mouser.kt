package com.codelv.inventory.suppliers

import android.util.Log
import androidx.core.text.trimmedLength
import com.codelv.inventory.DataSupplier
import com.codelv.inventory.ImportResult
import com.codelv.inventory.Part
import com.codelv.inventory.cleanUrl
import org.jsoup.nodes.Document

class Mouser: DataSupplier(name="Mouser", requiresJs=true) {

    override fun searchUrl(q: String): String {
        return "https://www.mouser.com/c/?q=${q}"
    }

    override fun isProductPage(url: String) : Boolean {
        return url.contains("mouser.com/ProductDetail/")
    }

    override fun requestHeaders(): Map<String, String> {
        return mapOf(
            "Referer" to "https://www.mouser.com",
            "Accept-Language" to "en-US,en"
        )
    }

    override suspend fun importPartData(part: Part, doc: Document, overwrite: Boolean) : ImportResult {
        val tag = "Mouser"
        var result = false

        if (doc.selectXpath("//body[@itemtype=\"http://schema.org/SearchResultsPage\"]")
                .first() != null
        ) {
            return ImportResult.MultipleResults
        }

        if (part.pictureUrl.isBlank() || overwrite) {
            val img =
                doc.selectXpath("//meta[@property=\"og:image\"]")
                    .first()
            if (img != null && img.hasAttr("content")) {
                part.pictureUrl = cleanUrl(img.attr("content"))
                Log.d(tag, "Imported picture url")
                result = true
            } else {
                Log.d(tag, "No picture found")
            }
        }

        if (part.datasheetUrl.isBlank() || overwrite) {
            val datasheet =
                doc.selectXpath("//a[@id=\"pdp-datasheet_0\"]").first()
            if (datasheet != null && datasheet.hasAttr("href")) {
                part.datasheetUrl = cleanUrl(datasheet.attr("href"))
                Log.d(tag, "Imported datasheet url")
                result = true
            } else {
                Log.d(tag, "No datasheet found")
            }
        }

        if (part.manufacturer.isBlank() || overwrite) {
            val mfg =
                doc.selectXpath("//a[@id=\"lnkManufacturerName\"]")
                    .first()
            if (mfg != null && mfg.hasText()) {
                part.manufacturer = mfg.text().trim()
                Log.d(tag, "Imported manufacturer")
                result = true
            } else {
                Log.d(tag, "No manufacturer found")
            }
        }

        if (part.sku.isBlank() || overwrite) {
            val sku =
                doc.selectXpath("//span[@id=\"spnMouserPartNumFormattedForProdInfo\"]")
                    .first()
            if (sku != null && sku.hasText()) {
                part.sku = sku.text().trim()
                Log.d(tag, "Imported supplier part number")
                result = true
            } else {
                Log.d(tag, "No supplier part number found")
            }
        }

        if (part.description.isBlank() || overwrite) {
            val span = doc.selectXpath("//span[@id=\"spnDescription\"]").first()
            if (span != null && span.hasText()) {
                part.description = span.text().trim()
                Log.d(tag, "Imported description")
                result = true
            }
        }
        return if (result) ImportResult.Success else ImportResult.NoData
    }
}