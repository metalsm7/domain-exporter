package com.mparang.exporter

import io.javalin.Javalin
import org.apache.commons.net.whois.WhoisClient
import org.yaml.snakeyaml.Yaml
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.Instant

class DomainExporter {
    private val domains: List<String>

    init {
        val yaml = Yaml()
        val inputStream = this::class.java.classLoader.getResourceAsStream("application.yaml")
            ?: throw IllegalStateException("application.yaml not found in resources")
        val config: Map<String, Any> = yaml.load(inputStream)
        val domainsRaw = config["domains"]?.toString()
            ?: throw IllegalStateException("'domains' not found in application.yaml")
        val domainsStr = resolveEnvVars(domainsRaw)
        domains = domainsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun resolveEnvVars(value: String): String {
        return Regex("""\$\{(\w+)(?::([^}]*))?\}""").replace(value) { match ->
            val envName = match.groupValues[1]
            val defaultValue = match.groupValues[2]
            System.getenv(envName) ?: defaultValue.ifEmpty { match.value }
        }
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val exporter = DomainExporter()
            val app = Javalin
                .create{ config ->
                    config.routes.get("/metrics") { ctx ->
                        ctx.contentType("text/plain; version=0.0.4")
                        ctx.result(exporter.buildMetrics())
                    }
                    config.startup.showJavalinBanner = false
                }
                .start("0.0.0.0", 7000)
        }
    }

    fun queryWhois(domain: String): String {
        val whoisClient = WhoisClient()
        return try {
            whoisClient.connect(WhoisClient.DEFAULT_HOST)
            whoisClient.query(domain)
        } finally {
            whoisClient.disconnect()
        }
    }

    fun parseExpiryEpoch(whoisResult: String): Long? {
        val pattern = Regex("""(?i)Expir\w*\s*Date\s*:\s*(.+)""")
        val match = pattern.find(whoisResult) ?: return null
        val dateStr = match.groupValues[1].trim()
        return try {
            ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_ZONED_DATE_TIME).toEpochSecond()
        } catch (e: Exception) {
            try {
                Instant.parse(dateStr).epochSecond
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun buildMetrics(): String {
        val sb = StringBuilder()
        sb.appendLine("# HELP domain_expiry_timestamp_seconds Domain expiry date as unix timestamp")
        sb.appendLine("# TYPE domain_expiry_timestamp_seconds gauge")
        sb.appendLine("# HELP domain_whois_query_success Whether the whois query succeeded")
        sb.appendLine("# TYPE domain_whois_query_success gauge")

        for (domain in domains) {
            try {
                val whoisResult = queryWhois(domain)
                val expiryEpoch = parseExpiryEpoch(whoisResult)
                if (expiryEpoch != null) {
                    sb.appendLine("domain_expiry_timestamp_seconds{domain=\"$domain\"} $expiryEpoch")
                }
                sb.appendLine("domain_whois_query_success{domain=\"$domain\"} 1")
            } catch (e: Exception) {
                sb.appendLine("domain_whois_query_success{domain=\"$domain\"} 0")
            }
        }

        // # HELP domain_expiry_timestamp_seconds Domain expiry date as unix timestamp
        // # TYPE domain_expiry_timestamp_seconds gauge
        // # HELP domain_whois_query_success Whether the whois query succeeded
        // # TYPE domain_whois_query_success gauge
        // domain_expiry_timestamp_seconds{domain="test.com"} 1836009100
        // domain_whois_query_success{domain="test.com"} 1
        // domain_expiry_timestamp_seconds{domain="test2.com"} 1825892847
        // domain_whois_query_success{domain="test2.com"} 1


        return sb.toString()
    }
}
