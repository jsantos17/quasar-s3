/*
 * Copyright 2014–2018 SlamData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quasar.physical.s3

import slamdata.Predef._
import argonaut.{DecodeJson, DecodeResult}
import org.http4s.Uri
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.instances.option._
import slamdata.Predef._

final case class S3Config(bucket: Uri, parsing: S3JsonParsing, credentials: Option[S3Credentials])

final case class AccessKey(value: String)
final case class SecretKey(value: String)
final case class Region(name: String)

final case class S3Credentials(accessKey: AccessKey, secretKey: SecretKey, region: Region)

object S3Config {
  /*  Example configuration for public buckets with line-delimited JSON:
   *  {
   *    "bucket": "<uri to bucket>",
   *    "jsonParsing": "lineDelimited"
   *  }
   *
   *  Example configuration for public buckets with array JSON:
   *  {
   *    "bucket": "<uri to bucket>",
   *    "jsonParsing": "array"
   *  }
   *
   *  Example configuration for a secure bucket with array JSON:
   *  {
   *    "bucket":"https://some.bucket.uri",
   *    "jsonParsing":"array",
   *    "credentials": {
   *      "accessKey":"some access key",
   *      "secretKey":"super secret key",
   *      "region":"us-east-1"
   *    }
   *  }
   *
   *  Example configuration for a secure bucket with line-delimited JSON:
   *  {
   *    "bucket":"https://some.bucket.uri",
   *    "jsonParsing":"lineDelimited",
   *    "credentials": {
   *      "accessKey":"some access key",
   *      "secretKey":"super secret key",
   *      "region":"us-east-1"
   *    }
   *  }
   *
   */
  private val parseStrings =
    Map[String, S3JsonParsing](
      "array" -> S3JsonParsing.JsonArray,
      "lineDelimited" -> S3JsonParsing.LineDelimited)

  private val failureMsg =
    "Failed to parse configuration for S3 connector."
  private val incompleteCredsMsg =
    "The 'credentials' key must include 'accessKey', 'secretKey' AND 'region'"

  implicit val decodeJson: DecodeJson[S3Config] =
    DecodeJson { c =>
      val b = c.get[String]("bucket").toOption >>= (Uri.fromString(_).toOption)
      val jp = c.get[String]("jsonParsing").toOption >>= (parseStrings.get(_))
      val creds = c.downField("credentials")
      val akey = creds.get[String]("accessKey").toOption.map(AccessKey(_))
      val skey = creds.get[String]("secretKey").toOption.map(SecretKey(_))
      val rkey = creds.get[String]("region").toOption.map(Region(_))

      (creds.success, b, jp) match {
        case (Some(_), Some(bk), Some(p)) =>
          (akey, skey, rkey).mapN(S3Credentials(_, _, _))
            .fold(DecodeResult.fail[S3Config](incompleteCredsMsg, c.history))(creds0 =>
              DecodeResult.ok[S3Config](S3Config(bk, p, Some(creds0))))

        case (None, Some(bk), Some(p)) =>
          DecodeResult.ok(S3Config(bk, p, None))

        case _ =>
          DecodeResult.fail(failureMsg, c.history)
      }
    }
}
