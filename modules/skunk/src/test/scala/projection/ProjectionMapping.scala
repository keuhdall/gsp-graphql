// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package projection

import cats.effect.{Resource, Sync}
import skunk.Session

import edu.gemini.grackle.Mapping
import edu.gemini.grackle.skunk.{SkunkMappingCompanion, SkunkMonitor}

import grackle.test.SqlProjectionSpec
import utils.{DatabaseSuite, SkunkTestMapping}

object ProjectionMapping extends SkunkMappingCompanion {
  def mkMapping[F[_]: Sync](pool: Resource[F, Session[F]], monitor: SkunkMonitor[F]): Mapping[F] =
    new SkunkTestMapping(pool, monitor) with SqlProjectionMapping[F]
}

final class ProjectionSpec extends DatabaseSuite with SqlProjectionSpec {
  lazy val mapping = ProjectionMapping.mkMapping(pool)
}