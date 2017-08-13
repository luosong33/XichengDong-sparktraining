package org.training.spark.core

import org.apache.spark._

import scala.collection.immutable.HashSet

/**
 * 年龄段在“18-24”的男性年轻人，最喜欢看哪10部电影
  * 首先获得筛选条件的用户集合（userid，age）
  * 用记录表（userid，mvid）筛选出这些用户看的电影filter { x => broadcastUserSet.value.contains(x._1) }
  * 并进行累加，排序map { x => (x._2, 1) }.reduceByKey(_ + _).map { x => (x._2, x._1) }，.sortByKey(false).map { x => (x._2, x._1)  }.take(10)
  * 最后与电影表关联出电影详情即可topKmovies.map(x => (movieID2Name.getOrElse(x._1, null), x._2))
 */
object PopularMovieAnalyzer {

  def main(args: Array[String]) {
    val startTime = System.currentTimeMillis()
    var dataPath = "data/ml-1m"
    val conf = new SparkConf().setAppName("PopularMovieAnalyzer")
    if(args.length > 0) {
      dataPath = args(0)
    } else {
      conf.setMaster("local[1]")
    }

    val sc = new SparkContext(conf)

    /**
     * Step 1: Create RDDs
     */
    val DATA_PATH = dataPath
    val USER_AGE = "18"

    val usersRdd = sc.textFile(DATA_PATH + "/users.dat")
    val moviesRdd = sc.textFile(DATA_PATH + "/movies.dat")
    val ratingsRdd = sc.textFile(DATA_PATH + "/ratings.dat")

    /**
     * Step 2: users Extract columns from RDDs
     */
    //users: RDD[(userID, age)]
    val users = usersRdd.map(_.split("::")).map { x =>
      (x(0), x(2))
    }.filter(_._2.equals(USER_AGE))

    //Array[String]
    val userlist = users.map(_._1).collect()

    //broadcast
    val userSet = HashSet() ++ userlist  //  集合想加
    val broadcastUserSet = sc.broadcast(userSet)  //  广播变量，多个excutor共享
//    val broadcastUserSet_ = broadcastUserSet.collect()
    println(broadcastUserSet)

    /**
     * Step 3: map-side join RDDs
     */
    val topKmovies = ratingsRdd.map(_.split("::")).map { x =>
      (x(0), x(1))  //  (userid,mvid)
    }.filter { x =>
      broadcastUserSet.value.contains(x._1)  //  筛选出用户观看的电影  (18,2987)
    }.map { x =>
      (x._2, 1)  //  去掉userid，只操作mvid  (2987,1)
    }.reduceByKey(_ + _).map { x =>
      (x._2, x._1)  //  对mvid计数
    }.sortByKey(false).map { x =>
      (x._2, x._1)  //  根据key排序
    }.take(10)  //  取top10  (2858,715)  （mvid，count）

    /**
     * Transfrom filmID to fileName
     */
    val movieID2Name_ = moviesRdd.map(_.split("::")).map { x =>
      (x(0), x(1))
    }.collect()   //  以数组的形式返回数据集的所有元素  (1,Toy Story (1995))
    val movieID2Name = movieID2Name_.toMap  //  元组是不同类型的值的聚集 val a = (2.6,"fred"),如果Array的元素类型是Tuple，
    // 调用Array的toMap方法， 可以将Array转换为Map  (710,Celtic Pride (1996))

    topKmovies.map(x => (movieID2Name.getOrElse(x._1, null), x._2))  //  x => (map取值，count)
      /*
        // 如果map中没有参数指定的key，则会报出异常，在调用前可以使用contains检查是否存在key
        val bobsScore = if (scores.contains("Bob")) scores("Bob") else 0
        // 可以使用getOrElse方法实现上述功能
        val bobsScore = scores.getOrElse("Bob",0)
      */
        .foreach(println)

    val endTime = System.currentTimeMillis()
    println("耗时为： " + ( endTime - startTime ))
    println(System.currentTimeMillis())  //  毫秒数

    sc.stop()
  }
}
