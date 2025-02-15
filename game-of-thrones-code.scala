```scala
// Databricks notebook source

// DBTITLE 1,Libraries
// Importing the required libraries
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

// Initializing Spark session
val spark = SparkSession.builder.appName("GameOfThronesData").getOrCreate()

// Path to the CSV file
val filePath = "dbfs:/FileStore/tables/game_of_thrones_deaths_data-1.csv"

// Loading the CSV file into a DataFrame
val gotDF = spark.read.option("header", "true").option("inferSchema", "true").csv(filePath)

// Displaying the first few rows of the DataFrame
gotDF.show()

// Grouping by 'allegiance' to count the number of deaths
val deathsByHouse = gotDF.groupBy("allegiance").count().orderBy(desc("count")).show()

// Grouping by 'method_cat' to count the occurrences
val commonMethods = gotDF.groupBy("method_cat").count().orderBy(desc("count")).show()

// Filtering rows where the killer is 'White Walker'
val killedByWhiteWalker = gotDF.filter(gotDF("killer") === "White Walker").show()

// Grouping by 'season' to count the number of deaths
val deathsBySeason = gotDF.groupBy("season").count().orderBy("season").show()

// Filtering rows where the reason contains "battle"
val battleDeaths = gotDF.filter(gotDF("reason").contains("battle")).show()

// Adding a new column 'importance_category'
val gotDFWithCategory = gotDF.withColumn("importance_category", 
  expr("CASE WHEN importance >= 3 THEN 'High' WHEN importance = 2 THEN 'Medium' ELSE 'Low' END")
).show()

// Calculating the average importance of deaths by 'allegiance'
val avgImportanceByHouse = gotDF.groupBy("allegiance")
  .agg(avg("importance").alias("average_importance"))
  .orderBy(desc("average_importance"))
  .show()

// Sampling houses DataFrame
val housesData = Seq(
  ("House Stark", "Winter is Coming"),
  ("House Lannister", "Hear Me Roar"),
  ("House Targaryen", "Fire and Blood")
).toDF("house", "motto")

// Joining gotDF with housesDF on 'allegiance' and 'house'
val joinedDF = gotDF.join(housesData, gotDF("allegiance") === housesData("house"), "left").show()

display(housesData)

// 1. Who killed Tywin Lannister and how? Select killer and method.
val tywinDeath = gotDF.filter(col("character_killed") === "Tywin Lannister").select("killer", "method").show()

// 2. In which season does Ned Stark die?
val nedDeathSeason = gotDF.filter(col("character_killed") === "Ned Stark").select("season").show()

// 3. Rename the column killer as assasin
val gotDFRenamed = gotDF.withColumnRenamed("killer", "assassin").show()

// 3. In a new df, add a new column making sentences with the register from the main df. For example: Arya stark killed Rorge with a sword
val gotDFWithSentence = gotDF.withColumn("death_sentence", 
  concat_ws(" ", col("killer"), lit("killed"), col("character_killed"), lit("with"), col("method"))
).select("death_sentence").show(truncate = false)

// 4. How many deaths occurred in each season?
val deathsBySeason = gotDF.groupBy("season").count().orderBy("season").show()

// 5. How many deaths were caused by Tyrion Lannister?
val tyrionDeaths = gotDF.filter(col("killer") === "Tyrion Lannister").count()

// Displaying the result
println(s"Deaths caused by Tyrion Lannister: $tyrionDeaths")

// 6. How many deaths were caused by Arya Stark? group it by method from greatest to least
val aryaDeathsByMethod = gotDF.filter(col("killer") === "Arya Stark")
  .groupBy("method")
  .count()
  .orderBy(desc("count"))
  .show()

val aryaDeaths = gotDF.filter(col("killer") === "Arya Stark").count()

// Displaying the result
println(s"Deaths caused by Arya Stark: $aryaDeaths")

// 7. Which characters have caused the most deaths?
val deathsByKiller = gotDF.groupBy("killer").count().orderBy(desc("count")).show()

// 8. What are the most common methods of killing?
val commonMethods = gotDF.groupBy("method").count().orderBy(desc("count")).show()

// 9. Which locations have had the most deaths?
val deathsByLocation = gotDF.groupBy("location").count().orderBy(desc("count")).show()

// 10. What is the number of deaths per episode in each season?
val deathsPerEpisode = gotDF.groupBy("season", "episode").count().orderBy("season", "episode").show()

// 11. What is the average importance of the killed characters by each method of killing?
val avgImportanceByMethod = gotDF.groupBy("method")
  .agg(avg("importance").alias("average_importance"))
  .orderBy(desc("average_importance"))
  .show()

// 12. What are the most common reasons characters are killed?
val commonReasons = gotDF.groupBy("reason").count().orderBy(desc("count")).show()

// 13. Which episodes have the highest number of deaths?
val deathsByEpisode = gotDF.groupBy("season", "episode").count().orderBy(desc("count")).show()

// 14. From allegiance column. Which house has suffered the most casualties and which has caused the most deaths?
// 14.1. Which house has suffered the most casualties?
val casualtiesByHouse = gotDF.groupBy("allegiance").count().orderBy(desc("count"))

// Displaying the result
val houseMostCasualties = casualtiesByHouse.limit(1).show()

// 14.2. Which has caused the most deaths?
val deathsByKillerHouse = gotDF.groupBy("killer").count().orderBy(desc("count"))

// Displaying the result
val houseMostDeathsCaused = deathsByKillerHouse.limit(1).show()

// 15. In a single df, obtain the 3 alliances that have caused the most deaths and the 3 alliances that have caused the least
// 15.1. 3 alliances that have caused the most deaths
val mostDeathsCaused = deathsByKillerHouse.limit(3)

// 15.2. 3 alliances that have caused the least deaths
val leastDeathsCaused = deathsByKillerHouse.orderBy("count").limit(3)

// Displaying the result
val combinedDeathsCaused = mostDeathsCaused.union(leastDeathsCaused).show()

// 1 - Create a new dataframe that adds a new column (i.e: n_deaths) that counts the number of deaths per season and method_cat
val deathsBySeasonAndMethod = gotDF.groupBy("season", "method_cat").agg(count("character_killed").as("n_deaths")).show()

// 2 - Find for each allegiance in which season didn't die any member
// DataFrame with all combinations of allegiances and seasons
val allCombinations = gotDF.select("allegiance").distinct().crossJoin(gotDF.select("season").distinct())

// Combinations where deaths occurred
val deaths = gotDF.select("allegiance", "season").distinct()

// Combinations where no deaths occurred
val noDeaths = allCombinations.except(deaths).show()

// DataFrame that shows the number of deaths of each character, for each allegiance
val deathsByCharacterByAllegiance = gotDF.groupBy("allegiance", "character_killed")
  .agg(count("character_killed").as("n_deaths"))
  .orderBy("allegiance", "n_deaths", "character_killed")
  .show()

val deathsByCharacterByAllegianceTest = gotDF.groupBy("allegiance", "character_killed")
  .agg(count("character_killed").as("n_deaths"))
  .orderBy("allegiance", "n_deaths", "character_killed")

// Defining the output path
val outputPath = "dbfs:/FileStore/tables/deaths_by_character_by_allegiance_test.csv"

// Writing the DataFrame to a CSV file
deathsByCharacterByAllegianceTest.write.option("header", "true").csv(outputPath)

// Counting the distinct allegiances in the DataFrame
val numAllegiances = gotDF.select("allegiance").distinct().count()

// 3 - For each allegiance, create a CSV file which shows the number of deaths of each character
// Grouping by allegiance and character_killed to get the count of deaths
val deathsByCharacterByAllegiance = gotDF.groupBy("allegiance", "character_killed")
  .agg(count("character_killed").as("n_deaths"))
  .orderBy("allegiance", "n_deaths", "character_killed")

// Geting distinct allegiances
val allegiances = gotDF.select("allegiance").distinct().collect().map(_.getString(0))

// Iterating over each allegiance and writing the corresponding DataFrame to a CSV file
allegiances.foreach { allegiance =>
  // Filter by allegiance
  val filteredDF = deathsByCharacterByAllegiance.filter(col("allegiance") === allegiance)
  
  // Defining the output path, removing or replacing characters that are not allowed in file names
  val sanitizedAllegiance = allegiance.replaceAll("[^a-zA-Z0-9]", "_")
  val outputPath = s"dbfs:/FileStore/tables/deaths_by_character_by_allegiance_$sanitizedAllegiance.csv"
  
  // Writing the DataFrame to a CSV file
  filteredDF.write.mode(SaveMode.Overwrite).option("header", "true").csv(outputPath)
}

// Grouping by allegiance and counting the total number of deaths
val deathsByAllegiance = gotDF.groupBy("allegiance")
  .agg(count("character_killed").as("total_deaths"))
  .orderBy(desc("total_deaths"))

// Writing the deaths by allegiance to a CSV file
deathsByAllegiance.coalesce(1).write
  .mode(SaveMode.Overwrite)
  .option("header", "true")
  .csv("dbfs:/FileStore/tables/deaths_by_allegiance.csv")

// Grouping by season and counting the total number of deaths
val deathsBySeason = gotDF.groupBy("season")
  .agg(count("character_killed").as("total_deaths"))
  .orderBy("season")

// Writing the deaths by season to a CSV file
deathsBySeason.coalesce(1).write
  .mode(SaveMode.Overwrite)
  .option("header", "true")
  .csv("dbfs:/FileStore/tables/deaths_by_season.csv")

// Path to the CSV file
val filePath = "dbfs:/FileStore/tables/deaths_by_season.csv"

// Reading the CSV file into a DataFrame
val df = spark.read.option("header", "true").csv(filePath).show()

// Path to the CSV file
val filePath = "dbfs:/FileStore/tables/deaths_by_allegiance.csv"

// Reading the CSV file into a DataFrame
val df = spark.read.option("header", "true").csv(filePath).show()

// Path to the CSV file
val filePath = "dbfs:/FileStore/tables/deaths_by_character_by_allegiance_Wise_Masters.csv"

// Reading the CSV file into a DataFrame
val df = spark.read.option("header", "true").csv(filePath).show()
