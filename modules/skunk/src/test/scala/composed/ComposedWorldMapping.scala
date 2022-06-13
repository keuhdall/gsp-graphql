// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package composed

import cats.effect.{Resource, Sync}
import skunk.Session

import edu.gemini.grackle.Mapping
import edu.gemini.grackle.skunk.{SkunkMappingCompanion, SkunkMonitor}

import utils.{DatabaseSuite, SkunkTestMapping}
import grackle.test.SqlComposedWorldSpec

object WorldMapping extends SkunkMappingCompanion {
  def mkMapping[F[_]: Sync](pool: Resource[F, Session[F]], monitor: SkunkMonitor[F]): Mapping[F] =
    new SkunkTestMapping[F](pool, monitor) with SqlWorldMapping[F]
}

object ComposedMapping extends SkunkMappingCompanion {
  def mkMapping[F[_]: Sync](pool: Resource[F, Session[F]], monitor: SkunkMonitor[F]): Mapping[F] =
    new SqlComposedMapping[F](WorldMapping.mkMapping(pool, monitor), CurrencyMapping[F])
}

final class ComposedWorldSpec extends DatabaseSuite with SqlComposedWorldSpec {
  lazy val mapping = ComposedMapping.mkMapping(pool)
}