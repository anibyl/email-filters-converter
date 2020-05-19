import com.xenomachina.argparser.ArgParser
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory


/**
 * @author Usievaład Kimajeŭ
 * @created 2020-05-08
 */
fun main(args: Array<String>) {
    ArgParser(args).parseInto(::Args).run {
        gmailToThunderbird(this)
    }
}

class Args(parser: ArgParser) {
    val source: File by parser.positional("SOURCE", help = "global path to Gmail mailFilters.xml") { File(this) }
    val account: String by parser.positional("ACCOUNT", help = "account like imap://name@domain")
    val noDots: Boolean by parser.flagging("-d", "--no-dots", help = "removes final dots from folder names")
    val ignoredFolderPrefix: String by parser.storing("-f", "--no-folders", help = "ignores folders that starts with the argument")
}

fun gmailToThunderbird(args: Args) {
    val xlmFile: File = args.source

    val xmlDoc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xlmFile)

    xmlDoc.documentElement.normalize()

    val feed: Element = xmlDoc.getElementsByTagName("feed").item(0) as Element

    val entries: NodeList = feed.getElementsByTagName("entry")

    val filters: ArrayList<ThunderbirdFilter> = arrayListOf()

    for (i in 0 until entries.length) {
        val entry: Element = entries.item(i) as Element

        val properties: NodeList = entry.getElementsByTagName("apps:property")

        var from: String? = null
        var label: String? = null

        for (j in 0 until properties.length) {
            val property: Element = properties.item(j) as Element

            if (property.getAttribute("name") == "from") {
                from = property.getAttribute("value")
            }

            if (property.getAttribute("name") == "label") {
                label = property.getAttribute("value")
            }
        }

        if (from == null || label == null) continue

        if (args.ignoredFolderPrefix.isNotEmpty() && label.startsWith(args.ignoredFolderPrefix)) continue

        if (args.noDots && label.endsWith(".")) {
            label = label.substringBeforeLast(".")
        }

        filters.add(
            ThunderbirdFilter(
                from.capitalize(),
                "${args.account}/${label}",
                "AND (from,ends with,${from})"
            )
        )
    }

    for (filter in filters) {
        println("name=\"${filter.name}\"")
        println("enabled=\"${filter.enabled}\"")
        println("type=\"${filter.type}\"")
        println("action=\"${filter.action}\"")
        println("actionValue=\"${filter.actionValue}\"")
        println("condition=\"${filter.condition}\"")
    }

    println()
}
