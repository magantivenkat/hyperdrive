/*
 *
 * Copyright 2019 ABSA Group Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package za.co.absa.hyperdrive.writers.impl

import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.streaming.{OutputMode, StreamingQuery, Trigger}
import za.co.absa.hyperdrive.offset.OffsetManager
import za.co.absa.hyperdrive.writers.StreamWriter

class ParquetStreamWriter(destination: String) extends StreamWriter(destination) {

  def write(dataFrame: DataFrame, offsetManager: OffsetManager): StreamingQuery = {
    val outStream = dataFrame
      .writeStream
      .trigger(Trigger.Once())
      .format("parquet")
      .outputMode(OutputMode.Append())

    offsetManager.configureOffsets(outStream)
      .start(destination)
  }
}
