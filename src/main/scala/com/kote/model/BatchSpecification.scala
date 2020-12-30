package com.kote.model

import spray.json._

case class BatchSpecification(providerId: Int,
                              familyId: Int,
                              url: String,
                              categories: List[Int],
                              lastScrappedUrl: Option[String] = None)

trait BatchSpecificationJsonProtocol extends DefaultJsonProtocol {
  implicit val batchSpecificationFormat = jsonFormat5(BatchSpecification)
}