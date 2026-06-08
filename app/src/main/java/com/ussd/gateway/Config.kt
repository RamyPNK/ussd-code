package com.ussd.gateway

object Config {
    var SERVER_URL    = "http://192.168.1.9"
    var SECRET_KEY    = "MY_SECRET_2024"
    var POLL_INTERVAL = 5000L
    var SIM_SLOT      = 0

    val OFFERS_POLL_URL     get() = "$SERVER_URL/api/phone_offers_poll.php?secret=$SECRET_KEY"
    val OFFERS_REPORT_URL   get() = "$SERVER_URL/api/phone_report_offers.php"
    val PURCHASE_POLL_URL   get() = "$SERVER_URL/api/phone_poll.php?secret=$SECRET_KEY"
    val PURCHASE_REPORT_URL get() = "$SERVER_URL/api/phone_report_purchase.php"
}