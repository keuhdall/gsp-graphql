// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package utils

import java.time.{Duration, LocalDate, LocalTime, ZonedDateTime}
import java.util.UUID

import cats.effect.Sync

import doobie.postgres.implicits._
import doobie.util.meta.Meta
import doobie.util.transactor.Transactor

import edu.gemini.grackle._
import doobie.{DoobieMapping, DoobieMonitor}

abstract class DoobieTestMapping[F[_]: Sync](transactor: Transactor[F], monitor: DoobieMonitor[F])
  extends DoobieMapping[F](transactor, monitor) with SqlTestMapping[F] {

  def bool: Codec = (Meta[Boolean], false)
  def text: Codec = (Meta[String], false)
  def varchar: Codec = (Meta[String], false)
  def bpchar(len: Int): Codec = (Meta[String], false)
  def int2: Codec = (Meta[Int], false)
  def int4: Codec = (Meta[Int], false)
  def int8: Codec = (Meta[Long], false)
  def float4: Codec = (Meta[Float], false)
  def float8: Codec = (Meta[Double], false)
  def numeric(precision: Int, scale: Int): Codec = (Meta[BigDecimal], false)

  def uuid: Codec = (Meta[UUID], false)
  def localDate: Codec = (Meta[LocalDate], false)
  def localTime: Codec = (Meta[LocalTime], false)
  def zonedDateTime: Codec = (Meta[ZonedDateTime], false)
  def duration: Codec = (Meta[Long].timap(Duration.ofMillis)(_.toMillis), false)

  def nullable(c: Codec): Codec = (c._1, true)

  def list(c: Codec): Codec = {
    val cm = c._1.asInstanceOf[Meta[Any]]
    val decode = cm.get.get.k.asInstanceOf[String => Any]
    val encode = cm.put.put.k.asInstanceOf[Any => String]
    val cl: Meta[List[Any]] = Meta.Advanced.array[String]("VARCHAR", "_VARCHAR").imap(_.toList.map(decode))(_.map(encode).toArray)
    (cl, false)
  }
}