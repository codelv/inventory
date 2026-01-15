package com.codelv.inventory.suppliers

import android.util.Log
import androidx.core.text.trimmedLength
import com.codelv.inventory.DataSupplier
import com.codelv.inventory.ImportResult
import com.codelv.inventory.Part
import com.codelv.inventory.cleanUrl
import org.jsoup.nodes.Document

// RS search does not display results without enabling local storage
class RS : DataSupplier(name = "RS", requiresJs = true, requireStorage = true) {

    override fun searchUrl(q: String): String {
        return "https://us.rs-online.com/catalogsearch/result/?q=${q}"
    }

    override fun isProductPage(url: String): Boolean {
        return url.contains("rs-online.com/product")
    }

    override fun requestHeaders(): Map<String, String> {
        return mapOf(
            "Referer" to "https://us.rs-online.com",
            "Accept-Language" to "en-US,en"
        )
    }

    override suspend fun importPartData(
        part: Part,
        doc: Document,
        overwrite: Boolean
    ): ImportResult {
        val tag = "RS"
        var result = false

        if (part.pictureUrl.isBlank() || overwrite) {
            val img =
                doc.selectXpath("//div[@class=\"product media\"]//img")
                    .first()
            val meta =
                doc.selectXpath("//meta[@name=\"twitter:image\"]")
                    .first()
            if (img != null && img.hasAttr("src")) {
                part.pictureUrl = cleanUrl(img.attr("src"))
                Log.d(tag, "Imported picture url")
                result = true
            } else if (meta != null && meta.hasAttr("content")) {
                part.pictureUrl = cleanUrl(meta.attr("content"))
                Log.d(tag, "Imported picture url")
                result = true
            } else {
                Log.d(tag, "No picture found ${img}")
            }
        }

        if (part.datasheetUrl.isBlank() || overwrite) {
            val datasheet =
                doc.selectXpath("//div[@class=\"downloads-item\"]//a").first()
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
                doc.selectXpath("//td[@data-th=\"Brand\"]")
                    .first()
            if (mfg != null && mfg.hasText()) {
                part.manufacturer = mfg.text().trim()
                Log.d(tag, "Imported manufacturer")
                result = true
            } else {
                Log.d(tag, "No manufacturer found")
            }
        }

        if (part.description.isBlank() || overwrite) {
            for (meta in doc.selectXpath("//meta[@name=\"description\"]")) {
                if (meta.hasAttr("content")) {
                    part.description = meta.attr("content").trim()
                    Log.d(tag, "Imported description")
                    result = true
                    break
                }
            }
        }
        return if (result) ImportResult.Success else ImportResult.NoData
    }
}


class RSUK : DataSupplier(name = "RS-UK", requiresJs = false) {

    override fun searchUrl(q: String): String {
        return "https://uk.rs-online.com/web/c/?searchTerm=${q}"
    }

    override fun isProductPage(url: String): Boolean {
        return url.contains("rs-online.com/web/p/")
    }

    override fun requestHeaders(): Map<String, String> {
        return mapOf(
            "Referer" to "https://uk.rs-online.com",
            "Accept-Language" to "en-GB,en"
        )
    }

    override suspend fun importPartData(
        part: Part,
        doc: Document,
        overwrite: Boolean
    ): ImportResult {
        val tag = "RS-UK"
        var result = false

        if (part.pictureUrl.isBlank() || overwrite) {
            val img =
                doc.selectXpath("//div[@data-testid=\"gallery-image-placeholder\"]//img")
                    .first()
            if (img != null && img.hasAttr("src")) {
                part.pictureUrl = cleanUrl(img.attr("src"))
                Log.d(tag, "Imported picture url")
                result = true
            } else {
                Log.d(tag, "No picture found ${img}")
            }
        }

        if (part.datasheetUrl.isBlank() || overwrite) {
            val a =
                doc.selectXpath("//ul[@data-testid=\"technical-documents\"]//a[contains(@text(), \"Datasheet\")]")
                    .first()
            if (a != null && a.hasAttr("href")) {
                part.datasheetUrl = cleanUrl(a.attr("href"))
                Log.d(tag, "Imported datasheet url")
                result = true
            } else {
                Log.d(tag, "No datasheet found")
            }
        }

        if (part.manufacturer.isBlank() || overwrite) {
            var node = doc.selectXpath("//*[@data-testid=\"brand-desktop\"]").first()
            if (node == null) {
                node = doc.selectXpath("//*[@data-testid=\"brand-mobile\"]").first()
            }
            if (node != null && node.hasText()) {
                part.manufacturer = node.text().trim()
                Log.d(tag, "Imported manufacturer")
                result = true
            } else {
                Log.d(tag, "No manufacturer found")
            }
        }

        if (part.description.isBlank() || overwrite) {
            val node = doc.selectXpath("//*[@data-testid=\"long-description\"]").first()
            if (node != null && node.hasText()) {
                part.description = node.text().trim()
                Log.d(tag, "Imported description")
                result = true
            } else {
                Log.d(tag, "No description found")
            }
        }

        if (part.sku.isBlank() || overwrite) {
            var node = doc.selectXpath("//*[@data-testid=\"stock-number-desktop\"]").first()
            if (node == null) {
                node = doc.selectXpath("//*[@data-testid=\"stock-number-mobile\"]").first()
            }
            if (node != null && node.hasText()) {
                part.sku = node.text().trim()
                Log.d(tag, "Imported sku")
                result = true
            } else {
                Log.d(tag, "No sku found")
            }
        }

        return if (result) ImportResult.Success else ImportResult.NoData
    }
}