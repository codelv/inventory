package com.codelv.inventory.suppliers

import android.util.Log
import androidx.core.text.trimmedLength
import com.codelv.inventory.DataSupplier
import com.codelv.inventory.ImportResult
import com.codelv.inventory.Part
import com.codelv.inventory.cleanUrl
import org.jsoup.nodes.Document

class LCSC: DataSupplier(requiresJs=false) {
    override fun matchesName(name: String): Boolean {
        return name.uppercase() == "LCSC"
    }

    override fun searchUrl(q: String): String {
        return "https://www.lcsc.com/search?q=${q}"
    }
    override fun isProductPage(url: String, content: String) : Boolean {
        return url.contains("lcsc.com/product-detail/")
    }

    override suspend fun importPartData(part: Part, doc: Document, overwrite: Boolean) : ImportResult {
        val tag = "LCSC"

        var result = false
        if (doc.selectXpath("//div[@class=\"product-table\"]")
                .first() != null
        ) {
            // TODO: May show only 1 result
            return ImportResult.MultipleResults
        }

        if (part.pictureUrl.trimmedLength() == 0 || overwrite) {
            var img =
                doc.selectXpath("//div[@class=\"asset\"]//img")
                    .first()
            if (img == null) {


            }
            if (img != null && img.hasAttr("src")) {
                part.pictureUrl = cleanUrl(img.attr("src"))
                Log.d(tag, "Imported picture url")
                result = true
            } else {
                Log.d(tag, "No picture found")
            }
        }

        if (part.datasheetUrl.trimmedLength() == 0 || overwrite) {
            var datasheet =
                doc.selectXpath("//table[@class=\"info-table\"]//tr[td[contains(text(), \"Datasheet\")]]//a")
                    .first()
            if (datasheet == null) {
                datasheet =
                    doc.selectXpath("//div[contains(text(), \"Datasheet:\")]/following-sibling::*/a")
                        .first()
            }
            if (datasheet != null && datasheet.hasAttr("href")) {
                part.datasheetUrl = cleanUrl(datasheet.attr("href"))
                Log.d(tag, "Imported datasheet url")
                result = true
            } else {
                Log.d(tag, "No datasheet found")
            }
        }

        if (part.manufacturer.trimmedLength() == 0 || overwrite) {
            var mfg =
                doc.selectXpath("//table[@class=\"info-table\"]//tr[td[contains(text(), \"Manufacturer\")]]//a")
                    .first()
            if (mfg == null) {
                mfg =
                    doc.selectXpath("//div[contains(text(), \"Manufacturer:\")]/following-sibling::*/a")
                        .first()
            }
            if (mfg != null && mfg.hasText()) {
                part.manufacturer = mfg.text().trim()
                Log.d(tag, "Imported manufacturer")
                result = true
            } else {
                Log.d(tag, "No manufacturer found")
            }
        }

        if (part.description.trimmedLength() == 0 || overwrite) {
            var desc =
                doc.selectXpath("//table[@class=\"info-table\"]//tr[td[contains(text(), \"Description\")]]//td")
                    .last()
            if (desc == null) {
                desc =
                    doc.selectXpath("//div[contains(text(), \"Description:\")]/following-sibling::*")
                        .first()
            }
            if (desc != null && desc.hasText()) {
                part.description = desc.text().trim()
                Log.d(tag, "Imported description")
                result = true
            }
        }
        return if (result) ImportResult.Success else ImportResult.NoData
    }

}