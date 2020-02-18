/*
 * Copyright 2018 ABSA Group Limited
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

package za.co.absa.hyperdrive.ingestor.implementation.writer.parquet

import org.apache.commons.configuration2.Configuration
import org.apache.logging.log4j.LogManager
import za.co.absa.hyperdrive.ingestor.api.PropertyMetadata
import za.co.absa.hyperdrive.ingestor.api.writer.{StreamWriter, StreamWriterFactory, StreamWriterFactoryProvider}
import za.co.absa.hyperdrive.ingestor.implementation.writer.parquet.AbstractParquetStreamWriter._
import za.co.absa.hyperdrive.shared.configurations.ConfigurationsKeys.ParquetStreamWriterKeys.{KEY_DESTINATION_DIRECTORY, KEY_EXTRA_CONFS_ROOT}

private[writer] class ParquetStreamWriter(destination: String, extraConfOptions: Map[String, String]) extends AbstractParquetStreamWriter(destination, extraConfOptions)

object ParquetStreamWriter extends StreamWriterFactory {

  def apply(config: Configuration): StreamWriter = {
    val destinationDirectory = getDestinationDirectory(config)
    val extraOptions = getExtraOptions(config)

    LogManager.getLogger.info(s"Going to create ParquetStreamWriter instance using: destination directory='$destinationDirectory', extra options='$extraOptions'")

    new ParquetStreamWriter(destinationDirectory, extraOptions)
  }

  override def getName: String = "Parquet Stream Writer"

  override def getDescription: String = "This writer saves ingested data in Parquet format on a filesystem (e.g. HDFS)"

  override def getProperties: Map[String, PropertyMetadata] = Map(
    KEY_DESTINATION_DIRECTORY -> PropertyMetadata("Destination directory", Some("A path to a directory"), required = true)
  )

  override def getExtraConfigurationPrefix: Option[String] = Some(KEY_EXTRA_CONFS_ROOT)
}

class ParquetStreamWriterLoader extends StreamWriterFactoryProvider {
  override def getComponentFactory: StreamWriterFactory = ParquetStreamWriter
}
