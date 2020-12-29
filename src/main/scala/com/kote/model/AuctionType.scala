package com.kote.model

sealed trait AuctionType

final case object BidType extends AuctionType

final case object FixedPriceType extends AuctionType