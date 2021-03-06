package com.kote.model

case class BatchAuctionLink(auctionUrl: String, thumbUrl: String)

case class Batch(batchId: String,
                 websiteInfo: WebsiteConfig,
                 auctionUrls: List[BatchAuctionLink])
