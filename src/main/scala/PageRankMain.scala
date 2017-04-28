/**
  * Created by Biyanta on 13/03/17.
  */
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.immutable.List

object PageRankMain {

  def main(args: Array[String]) {

    try{
      val inputFile = args(0)
      val outputFile = args(1)
      val conf = new SparkConf(true).setAppName("PageRank")
//                    .setMaster("yarn")
                    .setMaster("local")

      // Create a scala spark context
      val sc = new SparkContext(conf)

      // Pre-Processing (Cleaning the data, removing pages with ~ and calculating dangling nodes)
      val pagesWithoutTilde = sc.textFile(inputFile, sc.defaultParallelism)
        // getting each line formatted as pagename and outlink list
        .map(record => Bz2WikiParser.parseXML(record))

        // picking up records without tilde
        .filter(record => !record.contains("tilde record"))

        // splitting record with "BIYANTA", used during formatting
        .map(record => record.split("BIYANTA"))

        .map(record => if(record.length == 1){
                          (record(0),List()) // if there are no links then assign an empty list
                       }
                       else {
                        // used ~ as a delimeter since we don't have any pages with ~
                          (record(0), record(1).split("~").toList)
                       })

      // Ensuring these pages stay around for faster access
      pagesWithoutTilde.persist()

      // Compute dangling nodes
      val pagesWithDanglingNodes = pagesWithoutTilde.values
        // Combining the outlinks list
        .flatMap (page => page)
        .keyBy(page => page)

        // Combining all the outlinks of one key
        .reduceByKey((outlink1,outlink2) => outlink1.++(outlink2))

        // Creating a key value pair with outlink as key and an empty list as value
        .map(record => (record._1,List[String]()))

      // Combining both of them to get the final list of pages
      val finalPages = pagesWithoutTilde.union(pagesWithDanglingNodes)
        .reduceByKey((outlink1,outlink2) => outlink1.++(outlink2))

      // Getting the total number of pages
      val totalRecords = finalPages.count()

      // Calculating page rank by iterating 10 times

      // default page rank for each node at the starting of the iteration
      val initialPageRank: Double = (1.0 / totalRecords)
      // random jump probability
      val alpha: Double = 0.15

      // Assign the initial page rank to all the pages
      var finalPagesWithRanks = finalPages.keys.map(record => (record,initialPageRank))

      for (i <- 1 to 10) {
        try{
          // To store the dangling mass
          var danglingMass = sc.accumulator(0.0)

          // Contains the page name, its outlink list and its page rank, required for each iteration
          var pageRankCalc = finalPages.join(finalPagesWithRanks).values

            .flatMap {
              case (outLinks, pageRank) => {

                val size = outLinks.size

                // for the dangling node
                if (size == 0) {
                  // Update the dangling mass and return an empty list
                  danglingMass += pageRank
                  List()
                }
                else {
                  // For every link name in the outlink list, divide the pageRank by the size to
                  // calculate Σ(P(m)/C(m)) for m ∈ L(n), where L(n) are links pointing to a page n
                  outLinks.map(link => (link, pageRank / size))

                }
              }
            }.reduceByKey(_+_) // to add all the values of the same keys.

          // dangling statistics will be zero, if any action not performed on RDD
          pageRankCalc.first()

          // Storing the danglingMass to update the page rank
          val delta : Double = danglingMass.value

          // To get the pages having no inlinks but containing outlinks
          val noInlinks = finalPagesWithRanks.subtractByKey(pageRankCalc)
          val allPages = noInlinks.map(page => (page._1,0.0)).union(pageRankCalc)

          // Calculate the page rank by the formula
          finalPagesWithRanks = allPages.mapValues[Double](accumulatePageRank => (alpha / totalRecords
            + (1- alpha) * (delta / totalRecords + accumulatePageRank)))

        }
        catch {
          case e : Exception => println("Exception : " + e);
        }
      }

      // Calculating the top-K

      //Finding the top 100 page ranks with their pages
      val sorted = finalPagesWithRanks.takeOrdered(100)(Ordering[Double].reverse.on { line => line._2 })

      // To combine the sorted list in one file
      sc.parallelize(sorted).saveAsTextFile(outputFile)
    }
    catch {
      case e : Exception => println("Exception : " + e);
    }
  }

}

