package com.mparang.exporter

import io.javalin.Javalin
import org.apache.commons.net.whois.WhoisClient
import org.yaml.snakeyaml.Yaml
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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

        //
//         val whoisStr = """AC Phone Number             : 82-070-7734-2274
// Registered Date             : 2020. 04. 23.
// Last Updated Date           : 2025. 02. 05.
// Expiration Date             : 2027. 04. 23.
// Publishes                   : Y
// Authorized Agency           : Megazone(http://HOSTING.KR)"""
//         println("res: ${parseExpiryEpoch(whoisStr)}")

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
            Javalin
                .create{ config ->
                    config.routes.get("/metrics") { ctx ->
                        println("metrics requested")
                        ctx.contentType("text/plain; version=0.0.4")
                        ctx.result(exporter.buildMetrics())
                    }
                    config.startup.showJavalinBanner = false
                }
                .start("0.0.0.0", 7000)
        }
    }

    fun queryWhois(domain: String, whoisHost: String = "whois.iana.org"): String {
        val whoisClient = WhoisClient()
        return try {
            whoisClient.connect(whoisHost)
            whoisClient.query(domain)
        } finally {
            whoisClient.disconnect()
        }
//         return when (testIdx) {
//             1 -> """% IANA WHOIS server
// % for more information on IANA, visit http://www.iana.org
// % This query returned 1 object
//
// refer:        whois.verisign-grs.com
//
// domain:       COM
//
// organisation: VeriSign Global Registry Services
// address:      12061 Bluemont Way
// address:      Reston VA 20190
// address:      United States of America (the)
//
// contact:      administrative
// name:         Registry Customer Service
// organisation: VeriSign Global Registry Services
// address:      12061 Bluemont Way
// address:      Reston VA 20190
// address:      United States of America (the)
// phone:        +1 703 925-6999
// fax-no:       +1 703 948 3978
// e-mail:       info@verisign-grs.com
//
// contact:      technical
// name:         Registry Customer Service
// organisation: VeriSign Global Registry Services
// address:      12061 Bluemont Way
// address:      Reston VA 20190
// address:      United States of America (the)
// phone:        +1 703 925-6999
// fax-no:       +1 703 948 3978
// e-mail:       info@verisign-grs.com
//
// nserver:      A.GTLD-SERVERS.NET 192.5.6.30 2001:503:a83e:0:0:0:2:30
// nserver:      B.GTLD-SERVERS.NET 192.33.14.30 2001:503:231d:0:0:0:2:30
// nserver:      C.GTLD-SERVERS.NET 192.26.92.30 2001:503:83eb:0:0:0:0:30
// nserver:      D.GTLD-SERVERS.NET 192.31.80.30 2001:500:856e:0:0:0:0:30
// nserver:      E.GTLD-SERVERS.NET 192.12.94.30 2001:502:1ca1:0:0:0:0:30
// nserver:      F.GTLD-SERVERS.NET 192.35.51.30 2001:503:d414:0:0:0:0:30
// nserver:      G.GTLD-SERVERS.NET 192.42.93.30 2001:503:eea3:0:0:0:0:30
// nserver:      H.GTLD-SERVERS.NET 192.54.112.30 2001:502:8cc:0:0:0:0:30
// nserver:      I.GTLD-SERVERS.NET 192.43.172.30 2001:503:39c1:0:0:0:0:30
// nserver:      J.GTLD-SERVERS.NET 192.48.79.30 2001:502:7094:0:0:0:0:30
// nserver:      K.GTLD-SERVERS.NET 192.52.178.30 2001:503:d2d:0:0:0:0:30
// nserver:      L.GTLD-SERVERS.NET 192.41.162.30 2001:500:d937:0:0:0:0:30
// nserver:      M.GTLD-SERVERS.NET 192.55.83.30 2001:501:b1f9:0:0:0:0:30
// ds-rdata:     19718 13 2 8acbb0cd28f41250a80a491389424d341522d946b0da0c0291f2d3d771d7805a
//
// whois:        whois.verisign-grs.com
//
// status:       ACTIVE
// remarks:      Registration information: http://www.verisigninc.com
//
// created:      1985-01-01
// changed:      2026-03-10
// source:       IANA
//
// """
//             else -> """kpoprnx.com
//    Domain Name: KPOPRNX.COM
//    Registry Domain ID: 2763258791_DOMAIN_COM-VRSN
//    Registrar WHOIS Server: whois.hosting.kr
//    Registrar URL: http://HOSTING.KR
//    Updated Date: 2026-03-09T22:40:30Z
//    Creation Date: 2023-03-07T02:31:40Z
//    Registry Expiry Date: 2028-03-07T02:31:40Z
//    Registrar: Megazone Corp., dba HOSTING.KR
//    Registrar IANA ID: 1489
//    Registrar Abuse Contact Email: abuse@hosting.kr
//    Registrar Abuse Contact Phone: +82.216447378
//    Domain Status: clientTransferProhibited https://icann.org/epp#clientTransferProhibited
//    Name Server: CHRIS.NS.CLOUDFLARE.COM
//    Name Server: KAMI.NS.CLOUDFLARE.COM
//    DNSSEC: unsigned
//    URL of the ICANN Whois Inaccuracy Complaint Form: https://www.icann.org/wicf/
// >>> Last update of whois database: 2026-03-12T01:03:52Z <<<
//
// For more information on Whois status codes, please visit https://icann.org/epp
//
// NOTICE: The expiration date displayed in this record is the date the
// registrar's sponsorship of the domain name registration in the registry is
// currently set to expire. This date does not necessarily reflect the expiration
// date of the domain name registrant's agreement with the sponsoring
// registrar.  Users may consult the sponsoring registrar's Whois database to
// view the registrar's reported date of expiration for this registration.
//
// TERMS OF USE: You are not authorized to access or query our Whois
// database through the use of electronic processes that are high-volume and
// automated except as reasonably necessary to register domain names or
// modify existing registrations; the Data in VeriSign Global Registry
// Services' ("VeriSign") Whois database is provided by VeriSign for
// information purposes only, and to assist persons in obtaining information
// about or related to a domain name registration record. VeriSign does not
// guarantee its accuracy. By submitting a Whois query, you agree to abide
// by the following terms of use: You agree that you may use this Data only
// for lawful purposes and that under no circumstances will you use this Data
// to: (1) allow, enable, or otherwise support the transmission of mass
// unsolicited, commercial advertising or solicitations via e-mail, telephone,
// or facsimile; or (2) enable high volume, automated, electronic processes
// that apply to VeriSign (or its computer systems). The compilation,
// repackaging, dissemination or other use of this Data is expressly
// prohibited without the prior written consent of VeriSign. You agree not to
// use electronic processes that are automated and high-volume to access or
// query the Whois database except as reasonably necessary to register
// domain names or modify existing registrations. VeriSign reserves the right
// to restrict your access to the Whois database in its sole discretion to ensure
// operational stability.  VeriSign may restrict or terminate your access to the
// Whois database for failure to abide by these terms of use. VeriSign
// reserves the right to modify these terms at any time.
//
// The Registry database contains ONLY .COM, .NET, .EDU domains and
// Registrars."""
//         }
    }

    fun parseExpiryEpoch(whoisResult: String): Long? {
        // println("whoisResult: $whoisResult")
        val pattern = Regex("""(?i)Expir\w*\s*Date\s*:\s*(.+)""")
        // val match = pattern.find(whoisResult) ?: return null
        val dateStr = pattern
            .find(whoisResult)
            ?.groupValues[1]
            ?.trim()
            ?.replace(Regex("""\s""", RegexOption.DOT_MATCHES_ALL), "")
            ?.replace(Regex("""^(.*)(\.+)$""", RegexOption.DOT_MATCHES_ALL), "$1")
            ?.replace(Regex("""\.""", RegexOption.DOT_MATCHES_ALL), "-")
        return try {
            when (dateStr?.length ?: 0) {
                0 -> null
                10 -> LocalDate.parse(dateStr!!, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(ZoneOffset.UTC).toInstant().epochSecond
                else -> ZonedDateTime.parse(dateStr!!, DateTimeFormatter.ISO_ZONED_DATE_TIME).toEpochSecond()
            }
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
                val expiryEpoch = queryWhois(domain)
                    .let { it ->
                        //
                        parseExpiryEpoch(it)
                            .let { epoch ->
                                // println("epoch: $epoch")
                                if (epoch == null) {
                                    val whoisStr = it.replace("\n", " ")
                                    val match = Regex("""(.*)(whois:\s+)([^\s]*)(\s*)(.*)""", RegexOption.IGNORE_CASE).matches(whoisStr)
                                    // println("match: $match")
                                    if (match) {
                                        val whoIsHost = whoisStr.replace(Regex("""(.*)(whois:\s+)([^\s]*)(\s*)(.*)""", RegexOption.IGNORE_CASE), "$3")
                                        // println("whoIsHost: $whoIsHost")
                                        val whoIsResSub = queryWhois(domain, whoIsHost)
                                        // println("whoIsResSub: $whoIsResSub")
                                        parseExpiryEpoch(whoIsResSub)
                                    }
                                    else null
                                }
                                else {
                                    epoch
                                }
                            }
                    }
                // println("expiryEpoch: $expiryEpoch")
                if (expiryEpoch != null) {
                    sb.appendLine("domain_expiry_timestamp_seconds{domain=\"$domain\"} $expiryEpoch")
                }

                //
                // val expiryEpoch = parseExpiryEpoch(whoisResult)
                // if (expiryEpoch != null) {
                //     sb.appendLine("domain_expiry_timestamp_seconds{domain=\"$domain\"} $expiryEpoch")
                // }
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
