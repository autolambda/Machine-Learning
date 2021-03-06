package com.edu.hust.MLlib.Utils

/**
  * Spark's Vector implementations don't support algebraic operations. Unfortunately, the Spark API no longer supports to convert SparkVectors into BreezeVectors
  * via the methods asBreeze and fromBreeze, because these methods have been made package private with respect to the spark package.
  *
  * Created by liangjian on 2017/1/4.
  */

import breeze.linalg.{Vector => BreezeVector, DenseVector => DenseBreezeVector, SparseVector => SparseBreezeVector}
import org.apache.spark.mllib.linalg.{Vector => SparkVector, DenseVector => DenseSparkVector, SparseVector => SparseSparkVector}

package object myPackage {

  implicit class RichSparkVector[I <: SparkVector](vector: I) {
    def asBreeze[O <: BreezeVector[Double]](implicit converter: Spark2BreezeConverter[I, O]): O = {
      converter.convert(vector)
    }
  }

  implicit class RichBreezeVector[I <: BreezeVector[Double]](breezeVector: I) {
    def fromBreeze[O <: SparkVector](implicit converter: Breeze2SparkConverter[I, O]): O = {
      converter.convert(breezeVector)
    }
  }
}

trait Spark2BreezeConverter[I <: SparkVector, O <: BreezeVector[Double]] {
  def convert(sparkVector: I): O
}

/**
  * Spark2Breeze转换工具
  */
object Spark2BreezeConverter {

  implicit val denseSpark2DenseBreezeConverter = new Spark2BreezeConverter[DenseSparkVector, DenseBreezeVector[Double]] {
    override def convert(sparkVector: DenseSparkVector): DenseBreezeVector[Double] = {
      new DenseBreezeVector[Double](sparkVector.values)
    }
  }

  implicit val sparkSpark2SparseBreezeConverter = new Spark2BreezeConverter[SparseSparkVector, SparseBreezeVector[Double]] {
    override def convert(sparkVector: SparseSparkVector): SparseBreezeVector[Double] = {
      new SparseBreezeVector[Double](sparkVector.indices, sparkVector.values, sparkVector.size)
    }
  }

  implicit val defaultSpark2BreezeConverter = new Spark2BreezeConverter[SparkVector, BreezeVector[Double]] {
    override def convert(sparkVector: SparkVector): BreezeVector[Double] = {
      sparkVector match {
        case dv: DenseSparkVector => denseSpark2DenseBreezeConverter.convert(dv)
        case sv: SparseSparkVector => sparkSpark2SparseBreezeConverter.convert(sv)
      }
    }
  }
}

trait Breeze2SparkConverter[I <: BreezeVector[Double], O <: SparkVector] {
  def convert(breezeVector: I): O
}

/**
  * Breeze2Spark转换工具
  */
object Breeze2SparkConverter {
  implicit val denseBreeze2DenseSparkVector = new Breeze2SparkConverter[DenseBreezeVector[Double], DenseSparkVector] {
    override def convert(breezeVector: DenseBreezeVector[Double]): DenseSparkVector = {
      new DenseSparkVector(breezeVector.data)
    }
  }

  implicit val sparseBreeze2SparseSparkVector = new Breeze2SparkConverter[SparseBreezeVector[Double], SparseSparkVector] {
    override def convert(breezeVector: SparseBreezeVector[Double]): SparseSparkVector = {
      val size = breezeVector.activeSize
      val indices = breezeVector.array.index.take(size)
      val data = breezeVector.data.take(size)
      new SparseSparkVector(size, indices, data)
    }
  }

  implicit val defaultBreeze2SparkVector = new Breeze2SparkConverter[BreezeVector[Double], SparkVector] {
    override def convert(breezeVector: BreezeVector[Double]): SparkVector = {
      breezeVector match {
        case dv: DenseBreezeVector[Double] => denseBreeze2DenseSparkVector.convert(dv)
        case sv: SparseBreezeVector[Double] => sparseBreeze2SparseSparkVector.convert(sv)
      }
    }
  }
}

