package com.kote.actor

import java.time.LocalDateTime

import com.kote.model._

object AuctionScrapperProtocol {

  sealed trait PriceScrapperCommand

  final case class ScrapWebsite(website: Website) extends PriceScrapperCommand

  final case object ExtractUrls extends PriceScrapperCommand

  final case object ExtractAuctionUrls extends PriceScrapperCommand

  final case class ExtractAuctions(urlBatch: Batch) extends PriceScrapperCommand

  sealed trait CreateAuction extends PriceScrapperCommand {
    val batchId: String
    val externalId: String
    val url: String
    val title: String
    val isSold: Boolean
    val sellerNickname: String
    val sellerLocation: String
    val startPrice: Price
    val finalPrice: Option[Price]
    val startDate: LocalDateTime
    val endDate: Option[LocalDateTime]
    val thumbnailUrl: String
    val largeImageUrl: String
  }

  object CreateAuction {
    def apply(auctionType: AuctionType,
              batchId: String,
              externalId: String,
              url: String,
              title: String,
              isSold: Boolean,
              sellerNickname: String,
              sellerLocation: String,
              startPrice: Price,
              finalPrice: Option[Price],
              startDate: LocalDateTime,
              endDate: Option[LocalDateTime],
              thumbnailUrl: String,
              largeImageUrl: String,
              bids: List[Bid]): CreateAuction =
      auctionType match {
        case BidType =>
          CreateAuctionBid(
            batchId,
            externalId,
            url,
            title,
            isSold,
            sellerNickname,
            sellerLocation,
            startPrice,
            finalPrice,
            startDate,
            endDate,
            thumbnailUrl,
            largeImageUrl,
            bids)
        case FixedPriceType =>
          CreateAuctionFixedPrice(
            batchId,
            externalId,
            url,
            title,
            isSold,
            sellerNickname,
            sellerLocation,
            startPrice,
            finalPrice,
            startDate,
            endDate,
            thumbnailUrl,
            largeImageUrl,
            bids.headOption)
      }
  }

  final case class CreateAuctionBid(batchId: String,
                                    externalId: String,
                                    url: String,
                                    title: String,
                                    isSold: Boolean,
                                    sellerNickname: String,
                                    sellerLocation: String,
                                    startPrice: Price,
                                    finalPrice: Option[Price],
                                    startDate: LocalDateTime,
                                    endDate: Option[LocalDateTime],
                                    thumbnailUrl: String,
                                    largeImageUrl: String,
                                    bids: List[Bid]) extends CreateAuction

  final case class CreateAuctionFixedPrice(batchId: String,
                                           externalId: String,
                                           url: String,
                                           title: String,
                                           isSold: Boolean,
                                           sellerNickname: String,
                                           sellerLocation: String,
                                           startPrice: Price,
                                           finalPrice: Option[Price],
                                           startDate: LocalDateTime,
                                           endDate: Option[LocalDateTime],
                                           thumbnailUrl: String,
                                           largeImageUrl: String,
                                           bid: Option[Bid]) extends CreateAuction

  final case class ScrapAuction(url: String) extends PriceScrapperCommand
}
