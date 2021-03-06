package com.kote.validation

import cats.data.Chain
import cats.data.Validated.{Invalid, Valid}
import com.kote.model.{Batch, BatchAuctionLink, BidType, Delcampe, WebsiteConfig}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.browser.JsoupBrowser.JsoupDocument
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Try

class DelcampeValidatorTest extends AnyFlatSpec with Matchers with DelcampeValidatorTestFixtures {

  val delcampeValidator: DelcampeValidator = new DelcampeValidator

  implicit val jsoupBrowser: JsoupBrowser = JsoupBrowser.typed()
  val htmlDoc: JsoupBrowser.JsoupDocument = jsoupBrowser.get(BID_TYPE_SOLD_URL)

  it should "produce a list of all the auction links when websiteConfig.lastScrappedUrl is empty" in {
    val htmlString =
      """
        |<div class="items main">
        | <div class="item-listing">
        |   <div class="item-main-infos">
        |     <div class="image-container">
        |       <img class="image-thumb" src="http://www.example.com/img_thumb/1.jpg">
        |     </div>
        |     <div class="item-info">
        |       <a class="item-link" href="/auction1"></a>
        |     </div>
        |   </div>
        |   <div class="item-main-infos">
        |     <div class="image-container">
        |       <img class="image-thumb" src="http://www.example.com/img_thumb/2.jpg">
        |     </div>
        |     <div class="item-info">
        |       <a class="item-link" href="/auction2"></a>
        |     </div>
        |   </div>
        |   <div class="item-main-infos">
        |     <div class="image-container">
        |       <img class="image-thumb" src="http://www.example.com/img_thumb/3.jpg">
        |     </div>
        |     <div class="item-info">
        |       <a class="item-link" href="/auction3"></a>
        |     </div>
        |   </div>
        | </div>
        |</div>""".stripMargin
    val websiteConfig = WebsiteConfig(Delcampe, "http://www.example.com", None)
    delcampeValidator.fetchAuctionUrls(websiteConfig)(jsoupBrowser.parseString(htmlString)).map(_.auctionUrls) shouldBe
      Valid(
        List(
          BatchAuctionLink("http://www.example.com/auction1", "http://www.example.com/img_thumb/1.jpg"),
          BatchAuctionLink("http://www.example.com/auction2", "http://www.example.com/img_thumb/2.jpg"),
          BatchAuctionLink("http://www.example.com/auction3", "http://www.example.com/img_thumb/3.jpg")
        )
      )
  }
  it should "produce a list of the auction links appearing until the websiteConfig.lastScrappedUrl is found" in {
    val htmlString =
      """
        |<div class="items main">
        | <div class="item-listing">
        |   <div class="item-main-infos">
        |     <div class="image-container">
        |       <img class="image-thumb" src="http://www.example.com/img_thumb/1.jpg">
        |     </div>
        |     <div class="item-info">
        |       <a class="item-link" href="/auction1"></a>
        |     </div>
        |   </div>
        |   <div class="item-main-infos">
        |     <div class="image-container">
        |       <img class="image-thumb" src="http://www.example.com/img_thumb/2.jpg">
        |     </div>
        |     <div class="item-info">
        |       <a class="item-link" href="/auction2"></a>
        |     </div>
        |   </div>
        |   <div class="item-main-infos">
        |     <div class="image-container">
        |       <img class="image-thumb" src="http://www.example.com/img_thumb/3.jpg">
        |     </div>
        |     <div class="item-info">
        |       <a class="item-link" href="/auction3"></a>
        |     </div>
        |   </div>
        | </div>
        |</div>""".stripMargin
    val websiteConfig = WebsiteConfig(Delcampe, "http://www.example.com", Some("http://www.example.com/auction3"))
    delcampeValidator.fetchAuctionUrls(websiteConfig)(jsoupBrowser.parseString(htmlString)).map(_.auctionUrls) shouldBe
      Valid(
        List(
          BatchAuctionLink("http://www.example.com/auction1", "http://www.example.com/img_thumb/1.jpg"),
          BatchAuctionLink("http://www.example.com/auction2", "http://www.example.com/img_thumb/2.jpg")
        )
      )
  }
  it should "produce a Chain(AuctionLinkNotFound, ThumbnailLinkNotFound) from some invalid HTML string" in {
    val htmlString =
      """
        |<div class="items main">
        | <div class="item-listing">
        |   <div class="item-main-infos">
        |     <div class="image-container">
        |       <img class="image-thumb" src="http://www.example.com/img_thumb/1.jpg">
        |     </div>
        |     <div class="item-info">
        |       <a class="item-link-wrong-class-name" href="/auction1"></a>
        |     </div>
        |   </div>
        |   <div class="item-main-infos">
        |     <div class="image-container">
        |       <img class="image-thumb-wrong-class-name" src="http://www.example.com/img_thumb/2.jpg">
        |     </div>
        |     <div class="item-info">
        |       <a class="item-link" href="/auction2"></a>
        |     </div>
        |   </div>
        | </div>
        |</div>""".stripMargin
    val websiteConfig = WebsiteConfig(Delcampe, "http://www.example.com", None)
    delcampeValidator.fetchAuctionUrls(websiteConfig)(jsoupBrowser.parseString(htmlString)) shouldBe
      Invalid(Chain(AuctionLinkNotFound, ThumbnailLinkNotFound))
  }

  it should "produce the listing page as an HTML document" in {
    val htmlString =
      """<div class="items main">
        | <div class="item-listing">
        |   <div></div>
        |   <div></div>
        | </div>
        |</div>""".stripMargin
    val expectedDocument = jsoupBrowser.parseString(htmlString)

    def getPage(url: String): Try[JsoupDocument] = Try(expectedDocument)

    delcampeValidator.fetchListingPage(WebsiteConfig(Delcampe, "http://www.example.com", None), getPage, 20, 1) shouldBe
      Valid(expectedDocument)
  }
  it should "produce MaximumNumberOfAllowedPagesReached when the allowed limit of pages to read is reached" in {
    val htmlString = """<div class="items main"><div><h2>You have reached the limit of results to display</h2></div></div>"""

    def getPage(url: String): Try[JsoupDocument] = Try(jsoupBrowser.parseString(htmlString))

    delcampeValidator.fetchListingPage(WebsiteConfig(Delcampe, "http://www.example.com", None), getPage, 20, 1) shouldBe
      Invalid(Chain(MaximumNumberOfAllowedPagesReached))
  }
  it should "produce LastListingPageReached when the last listing page is reached" in {
    val htmlString = """<div class="items main"><div><h2></h2></div></div>"""

    def getPage(url: String): Try[JsoupDocument] = Try(jsoupBrowser.parseString(htmlString))

    delcampeValidator.fetchListingPage(WebsiteConfig(Delcampe, "http://www.example.com", None), getPage, 20, 1) shouldBe
      Invalid(Chain(LastListingPageReached))
  }

  it should "extract the auction TYPE from a SOLD auction of BID type" in {
    delcampeValidator.validateAuctionType(htmlDoc) shouldBe Valid(BidType)
  }
  it should "produce AuctionTypeNotFound from some invalid HTML string" in {
    val htmlString = """<div class="price-info"><div><i></i></div></div>"""
    delcampeValidator.validateAuctionType(jsoupBrowser.parseString(htmlString)) shouldBe
      Invalid(Chain(AuctionTypeNotFound))
  }
  it should "produce AuctionTypeNotFound when the AUCTION TYPE class attribute is invalid" in {
    val htmlString = """<div class="price-info"><div><i class="dummy"></i></div></div>"""
    delcampeValidator.validateAuctionType(jsoupBrowser.parseString(htmlString)) shouldBe
      Invalid(Chain(AuctionTypeNotFound))
  }

  it should "extract the auction SOLD FLAG from a SOLD auction of BID type" in {
    delcampeValidator.validateIsSold(htmlDoc) shouldBe Valid(true)
  }
  it should "produce false from some invalid HTML string" in {
    val htmlString = """<div id="closed-sell"><div>"""
    delcampeValidator.validateIsSold(jsoupBrowser.parseString(htmlString)) shouldBe
      Valid(false)
  }
}
