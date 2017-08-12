package org.training.spark.core

import org.apache.spark.{SparkConf, SparkContext}

class Test {

  def main(args: Array[String]) {
    val users = (17,"F");  //  验证
    val usermovie = (17,2116);  //  验证

    /**
      * Step 3: join RDDs
      */
    //useRating: RDD[(userID, (movieID, (gender, age)))]
//    val userRating = usermovie.join(users)
//    val userRating_ = userRating.collect();  //  验证

  }

}
