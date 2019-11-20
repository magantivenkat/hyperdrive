/*
 * Copyright 2018-2019 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.hyperdrive.ingestor.implementation.finalizer.noop

import org.apache.commons.configuration2.Configuration
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.streaming.StreamingQuery
import za.co.absa.hyperdrive.ingestor.api.finalizer.{IngestionFinalizer, IngestionFinalizerFactory}

private[finalizer] class NoOpFinalizer extends IngestionFinalizer {
  /**
   * Performs any cleanup / finalization tasks after the ingestionQuery has been stopped.
   */
  override def finalize(ingestionQuery: StreamingQuery): Unit = {
    // no-op
  }
}

object NoOpFinalizer extends IngestionFinalizerFactory {
  private val logger = LogManager.getLogger
  override def apply(config: Configuration): IngestionFinalizer = {
    logger.info("Going to create NoOpFinalizer")
    new NoOpFinalizer()
  }
}