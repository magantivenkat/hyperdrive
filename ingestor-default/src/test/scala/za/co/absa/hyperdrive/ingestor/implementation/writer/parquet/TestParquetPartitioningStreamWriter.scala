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

import org.apache.commons.configuration2.BaseConfiguration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.execution.streaming.MemoryStream
import org.apache.spark.sql.functions.lit
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import za.co.absa.commons.io.TempDirectory
import za.co.absa.commons.spark.SparkTestBase
import za.co.absa.hyperdrive.ingestor.api.writer.StreamWriterCommonAttributes

class TestParquetPartitioningStreamWriter extends FlatSpec with SparkTestBase with Matchers with BeforeAndAfter {

  import spark.implicits._

  private var baseDir: TempDirectory = _
  private def baseDirPath = baseDir.path.toUri.toString
  private def destinationDir = s"$baseDirPath/destination"
  private def checkpointDir = s"$baseDirPath/checkpoint"
  private val random = scala.util.Random

  private val fs = FileSystem.get(spark.sparkContext.hadoopConfiguration)

  behavior of "ParquetPartitioningStreamWriter"

  before {
    baseDir = TempDirectory("testparquetpartitioning").deleteOnExit()
  }

  after {
    baseDir.delete()
  }

  it should "write partitioned by date and version=1 where destination directory is empty" in {
    // given
    val config = new BaseConfiguration()
    config.addProperty("writer.parquet.destination.directory", destinationDir)
    config.addProperty("writer.parquet.partitioning.report.date", "2020-02-29")
    config.addProperty(StreamWriterCommonAttributes.keyCheckpointBaseLocation, checkpointDir)
    val underTest = ParquetPartitioningStreamWriter(config)

    val df = getDummyReadStream().toDF()

    // when
    val streamingQuery = underTest.write(df)
    streamingQuery.processAllAvailable()

    // then
    fs.exists(new Path(s"$destinationDir/hyperdrive_date=2020-02-29/hyperdrive_version=1")) shouldBe true
    spark.read.parquet(destinationDir)
      .select("value").map(_ (0).asInstanceOf[Int]).collect() should contain theSameElementsAs List.range(0, 100)
  }

  it should "write to partition version=2 when version=1 already exists for the same date" in {
    // given
    val config = new BaseConfiguration()
    config.addProperty("writer.parquet.destination.directory", destinationDir)
    config.addProperty("writer.parquet.partitioning.report.date", "2020-02-29")
    config.addProperty(StreamWriterCommonAttributes.keyCheckpointBaseLocation, checkpointDir)
    val underTest = ParquetPartitioningStreamWriter(config)

    val previousDayConfig = new BaseConfiguration()
    previousDayConfig.addProperty("writer.parquet.destination.directory", destinationDir)
    previousDayConfig.addProperty("writer.parquet.partitioning.report.date", "2020-02-28")
    previousDayConfig.addProperty(StreamWriterCommonAttributes.keyCheckpointBaseLocation, checkpointDir)
    val previousDayWriter = ParquetPartitioningStreamWriter(previousDayConfig)

    // when
    val stream = getDummyReadStream()
    previousDayWriter.write(stream.toDF()).processAllAvailable()
    stream.addData(List.range(1000, 1100))
    previousDayWriter.write(stream.toDF()).processAllAvailable()

    stream.addData(List.range(2000, 2100))
    underTest.write(stream.toDF()).processAllAvailable()
    stream.addData(List.range(3000, 3100))
    underTest.write(stream.toDF()).processAllAvailable()

    // then
    fs.exists(new Path(s"$destinationDir/hyperdrive_date=2020-02-29/hyperdrive_version=1")) shouldBe true
    fs.exists(new Path(s"$destinationDir/hyperdrive_date=2020-02-29/hyperdrive_version=2")) shouldBe true

    val df = spark.read.parquet(destinationDir)
    df.select("value")
      .filter(df("hyperdrive_date") === lit("2020-02-29"))
      .filter(df("hyperdrive_version") === 1)
      .map(_ (0).asInstanceOf[Int]).collect() should contain theSameElementsAs List.range(2000, 2100)

    val df2 = spark.read.parquet(destinationDir)
    df2.select("value")
      .filter(df2("hyperdrive_date") === lit("2020-02-29"))
      .filter(df2("hyperdrive_version") === 2)
      .map(_ (0).asInstanceOf[Int]).collect() should contain theSameElementsAs List.range(3000, 3100)
  }

  it should "not throw if there is an empty commit in the destination" in {
    // given
    val config = new BaseConfiguration()
    config.addProperty("writer.parquet.destination.directory", destinationDir)
    config.addProperty("writer.parquet.partitioning.report.date", "2020-02-29")
    config.addProperty(StreamWriterCommonAttributes.keyCheckpointBaseLocation, checkpointDir)
    val underTest = ParquetPartitioningStreamWriter(config)

    val emptyStream = MemoryStream[Int](random.nextInt(), spark.sqlContext)

    // when
    underTest.write(emptyStream.toDF()).processAllAvailable()
    emptyStream.addData(List.range(0, 100))
    underTest.write(emptyStream.toDF()).processAllAvailable()

    // then
    fs.exists(new Path(s"$destinationDir/hyperdrive_date=2020-02-29/hyperdrive_version=1")) shouldBe true
    fs.exists(new Path(s"$destinationDir/hyperdrive_date=2020-02-29/hyperdrive_version=2")) shouldBe false
    val df = spark.read.parquet(s"$destinationDir/hyperdrive_date=2020-02-29/hyperdrive_version=1")
    df.count() shouldBe 100
  }

  private def getDummyReadStream() = {
    val input = MemoryStream[Int](random.nextInt(), spark.sqlContext)
    input.addData(List.range(0, 100))
    input
  }
}
