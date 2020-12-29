package com.kote.model

import spray.json._

case class ScrapSpecification(provider: Int,
                              familyId: Int,
                              url: String,
                              categories: List[Int])

trait ScrapSpecificationJsonProtocol extends DefaultJsonProtocol {
  implicit val scrapSpecificationFormat = jsonFormat4(ScrapSpecification)
}