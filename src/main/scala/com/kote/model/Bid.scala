package com.kote.model

import java.time.LocalDateTime

case class Bid(nickname: String,
               bidPrice: Price,
               quantity: Int,
               isAutomaticBid: Boolean,
               bidAt: LocalDateTime)