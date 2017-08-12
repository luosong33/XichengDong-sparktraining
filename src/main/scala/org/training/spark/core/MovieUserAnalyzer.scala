package org.training.spark.core

import org.apache.spark._

/**
  * 离线计算
 * 看过“Lord of the Rings, The (1978)”用户和年龄性别分布
  * 筛选出记录表的电影，关联用户表
 */
object MovieUserAnalyzer {

  def main(args: Array[String]) {
    var dataPath = "data/ml-1m"
    val conf = new SparkConf().setAppName("PopularMovieAnalyzer")
    if(args.length > 0) {
      dataPath = args(0)
    } else {
      conf.setMaster("local[1]")  //  local[1]代表开一个线程
    }

    val sc = new SparkContext(conf)

    /**
     * Step 1: Create RDDs
     */
    val DATA_PATH = dataPath
    val MOVIE_TITLE = "Lord of the Rings, The (1978)"
    val MOVIE_ID = "2116"

    //  加载数据
    val usersRdd = sc.textFile(DATA_PATH + "/users.dat")  //  用户表，每个用户的基本信息
    val ratingsRdd = sc.textFile(DATA_PATH + "/ratings.dat")  //  观看记录表，每个用户看过的电影及其等级时长

    /**
     * Step 2: Extract columns from RDDs
     */
    //users: RDD[(userID, (gender, age))]
    val users = usersRdd.map(_.split("::")).map { x =>  //  749::M::35::18::56303
      (x(0), (x(1), x(2)))  //  (749,(M,35))
    }
    val users_ = users.collect();  //  验证


    //rating: RDD[Array(userID, movieID, ratings, timestamp)]
    val rating = ratingsRdd.map(_.split("::"))  //  1、切分，处理为kv
    val rating_ = rating.map { x =>
      (x(0), x(1))  //  (1,1193)
    }.collect();  //  验证

    //usermovie: RDD[(userID, movieID)]
    val usermovie = rating.map { x =>
      (x(0), x(1))
    }.filter(_._2.equals(MOVIE_ID))  //  每行第二个元素比对2116  (17,2116)   2、筛选出看过2116电影的用户
    val usermovie_ = usermovie.collect();  //  验证

    /**
     * Step 3: join RDDs
     */
    //useRating: RDD[(userID, (movieID, (gender, age)))]
    /*val u = "2116::M::18::4::49546"
    val r = "117::2116::4::9775019796"
    val _users = sc.textFile(u).map(_.split("::")).map { x => (x(0), x(1))}
    val _usermovie = sc.textFile(r).map(_.split("::")).map { x => (x(0), x(1))}
    val _userRating = _usermovie.join(_users).collect()*/

    val userRating = usermovie.join(users)  //  (749,(2116,(M,35)))  左链接，只包含左的
    val userRating_ = userRating.collect()  //  验证

    //userRating.take(1).foreach(print)

    //movieuser: RDD[(movieID, (movieTile, (gender, age))]
    val userDistribution = userRating.map { x =>
      (x._2._2, 1)  //  （性别、年龄），userid
    }.reduceByKey(_ + _)  //  相同的累加一次

    val userDistribution_ = userDistribution.collect
    userDistribution.collect.foreach(println)

    sc.stop()
  }
}