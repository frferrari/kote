package com.kote.model

case class WebsiteConfig(website: Website,
                         url: String,
                         lastScrappedUrl: Option[String])
