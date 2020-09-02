package com.hatmini.util

import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import io.circe.generic.AutoDerivation
import io.circe.{ Decoder, Encoder }
import io.circe.parser
import org.bson.BsonDocument
import scala.util.Try

trait CirceSupport extends ErrorAccumulatingCirceSupport with AutoDerivation { 
  //will place any special deserialisers here for case classes that can't be auto-derived

  implicit val encodeDataEvelope: Encoder[BsonDocument] = new Encoder[BsonDocument] {
    final def apply(env: BsonDocument) = {
      parser.parse(env.toJson()).getOrElse(throw new Exception("Failed JSON decoding from Mongo")) //this error case is not possible
    }
  }

  implicit val decoderDataEnvelope: Decoder[BsonDocument] = Decoder.decodeJson.emapTry { json =>
    Try(BsonDocument.parse(json.noSpaces))
  }

}