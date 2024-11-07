package dev.fireants.carrierconfig

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

// We don't use namespaces.
private val ns: String? = null

class XMLParser {
    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream): Roaminfo {
        inputStream.use { inputStream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()
            return readFeed(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFeed(parser: XmlPullParser): Roaminfo {
        var nonroam : Nonroam? = null
        var roam : Roam? = null

        parser.require(XmlPullParser.START_TAG, ns, "roaming_config")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // Starts by looking for the entry tag.
            if (parser.name == "non_roaming") {
                nonroam = readEntry(parser)
            } else if (parser.name == "roaming") {
                roam = readEntry2(parser)
            } else {
                skip(parser)
            }
        }
        if (nonroam == null) {
            nonroam = Nonroam(arrayListOf())
        }
        if (roam == null) {
            roam = Roam(arrayListOf())
        }
        return Roaminfo(nonroam, roam)
    }

    data class Roaminfo(val nonroam: Nonroam, val roam: Roam)
    data class Nonroam(val items: ArrayList<String>)
    data class Roam(val items: ArrayList<String>)

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readEntry(parser: XmlPullParser): Nonroam {
        parser.require(XmlPullParser.START_TAG, ns, "non_roaming")
        var items: ArrayList<String> = arrayListOf()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "item" -> items.add(readItem(parser))
                else -> skip(parser)
            }
        }
        return Nonroam(items)
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readEntry2(parser: XmlPullParser): Roam {
        parser.require(XmlPullParser.START_TAG, ns, "roaming")
        var items: ArrayList<String> = arrayListOf()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "item" -> items.add(readItem(parser))
                else -> skip(parser)
            }
        }
        return Roam(items)
    }

    // Processes title tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readItem(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "item")
        var title = ""
        if (parser.next() == XmlPullParser.TEXT) {
            title = parser.text
            parser.nextTag()
        }
        parser.require(XmlPullParser.END_TAG, ns, "item")
        return title
    }

    // Processes link tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readLink(parser: XmlPullParser): String {
        var link = ""
        parser.require(XmlPullParser.START_TAG, ns, "link")
        val tag = parser.name
        val relType = parser.getAttributeValue(null, "rel")
        if (tag == "link") {
            if (relType == "alternate") {
                link = parser.getAttributeValue(null, "href")
                parser.nextTag()
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, "link")
        return link
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

}