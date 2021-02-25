package com.dividat.docless.swagger

import enumeratum._

sealed trait CollectionFormat extends EnumEntry with EnumEntry.Lowercase

object CollectionFormat
    extends CirceEnum[CollectionFormat]
    with Enum[CollectionFormat] {
  case object CSV   extends CollectionFormat
  case object SSV   extends CollectionFormat
  case object TSV   extends CollectionFormat
  case object Pipes extends CollectionFormat
  case object Multi extends CollectionFormat

  override def values = findValues
}
