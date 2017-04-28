
—————————————————
STANDALONE MODE
—————————————————
Building and execution of PageRank in Standalone mode.

1. Create a new Maven Project in the IDE.

2. Give the Group ID and the artifact ID.

3. A pom.xml will be created, add the required dependencies. 

4. The src/main/java should contain the source code files.
	
5. Create an input folder which should contain the input file (provided in the assignment).
wikipedia-simple-html.bz2

6. Include the config folder which has the stand-alone configurations: core-site.xml, hdfs-site.xml, mapred-site.xml, yarn-site.xml. 

7. In the Makefile for standalone mode change
spark.root = spark location on your local system
jar.name = jar file that will be created
jar.path = target/${jar.name}
job.name = change it to the path of your main file

Updated alone command (used spark-submit) : ${spark.root}/bin/spark-submit --class ${job.name} --master local[*] ${jar.path} ${local.input} ${local.output}

8. pom.xml, Makefile, src, config and input folders should be in the same project directory.

After the above steps:
Run 2 commands into the terminal of the IDE
1. make switch-standalone.

2. make alone.

Once the make alone finishes execution we will get a target folder and an output folder. The target and output folders are deleted and created every time we run make alone.

The target folder contains the .class files and .jar file.

We will get one output folder which will have the top 100 records with pages and page ranks in descending order.

—————————————————
AWS EMR
—————————————————

1. These are the plugins and dependencies your pom.xml should contain
(I have added scald-maven-plugin, replaced maven-shade-plugin with maven-assembly-plugin, replace hadoop dependencies with scala and spark dependencies)

<plugins>
    <plugin>
        <groupId>net.alchim31.maven</groupId>
        <artifactId>scala-maven-plugin</artifactId>
        <version>3.2.2</version>
            <executions>
                <execution>
                    <id>scala-compile-first</id>
                    <phase>process-resources</phase>
                    <goals>
                        <goal>add-source</goal>
                        <goal>compile</goal>
                    </goals>
                </execution>
            	<execution>
                    <id>scala-test-compile</id>
                    <phase>process-test-resources</phase>
                    <goals>
                        <goal>testCompile</goal>
                    </goals>
                </execution>
            </executions>
   </plugin>
	 <plugin>
	      <artifactId>maven-compiler-plugin</artifactId>
              <version>3.3</version>
              <configuration>
                  <source>1.8</source>
                  <target>1.8</target>
              </configuration>
         </plugin>

         <plugin>
              <artifactId>maven-assembly-plugin</artifactId>
              <version>2.4</version>
              <configuration>
                  <descriptorRefs>
     	                <descriptorRef>jar-with-dependencies</descriptorRef>
                  </descriptorRefs>
              </configuration>
              <executions>
                  <execution>
                      <id>make-assembly</id>
                      <phase>package</phase>
                      <goals>
                          <goal>single</goal>
                      </goals>
                  </execution>
              </executions>      
        </plugin>

        </plugins>
    </build>

    <dependencies>

        <dependency>
            <groupId>org.scala-lang</groupId>
            <artifactId>scala-library</artifactId>
            <version>2.11.8</version>
        </dependency>

        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-core_2.11</artifactId>
            <version>2.1.0</version>
        </dependency>

        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>3.8.1</version>
            <scope>test</scope>
        </dependency>

    </dependencies>

2. In the Makefile for AWS execution change
aws.region = to the region of your aws cluster
aws.bucket.name = bucket_name you want to create on S3
aws.subnet.id = subnet_id of your region from VPC subnet list
aws.input = name of the input folder on S3
aws.output = name of the output folder on S3
aws.log.dir = name of the log folder
aws.num.nodes =  number of worker machines
aws.instance.type = type of the machine to use.

For run-1 your aws.num.nodes=5 and aws.instance.type=m4.large
For run-2 your aws.num.nodes=10 and aws.instance.type=m4.large

Updated the cloud command (applicationsName, steps and added configurations option):

--applications Name=Spark \
--steps '[{"Name":"Spark Program", "Args":["--class", "${job.name}", "--master", "yarn", "--deploy-mode", "cluster", "s3://${aws.bucket.name}/${jar.name}", "s3://${aws.bucket.name}/${aws.input}","s3://${aws.bucket.name}/${aws.output}"],"Type":"Spark","Jar":"s3://${aws.bucket.name}/${jar.name}","ActionOnFailure":"TERMINATE_CLUSTER"}]' \
--log-uri s3://${aws.bucket.name}/${aws.log.dir} \

3. In the source code, replace 
val conf = new SparkConf(true).setAppName("PageRank").setMaster("local") with
val conf = new SparkConf(true).setAppName("PageRank").setMaster("yarn")

4. On the terminal go to the directory where your source code, Makefile, pom.xml, input folder and .jar file exists. In the empty input folder add the 4 files mentioned in the assignment (full wiki data set for 2006).
Upload data on S3 with : make upload-input-aws

This will upload the data in your input folder and will make a bucket on S3 with the name as in your aws.bucket.name (in Makefile)

5. Log into the console on AWS.

6. Run ‘make cloud’ on the terminal to launch your cluster on EMR.

7. Go to the AWS console, to the cluster to see how the Cluster is running. Once it’s terminated with steps completed, check the syslog.

8. Go to S3 and in the output folder you will see a part-00000 file which will have your final 100 pages and page ranks. Download that part-00000.

The commands executed from step 4-8 should all be executed in that same directory on the terminal.

———————————————————————————————————————————————————
DIRECTORY STRUCTURE OF THE PROGRAMS
———————————————————————————————————————————————————
For the Intellij IDE

1. File Structure : PageRankScala/src/main/scala

a. Main Files: 

1. Bz2WikiParser.java: Parsing the compressed Bz2 file and returning a string of page names and outlines (for each line) separated by a delimiter. It is called from PageRankMain.scala

2. PageRankMain.scala: The main file, where an object of PageRankMain is created, and all preprocessing, calculating page ranks for 10 iterations and top K calculation is done here.

For a normal run (not from the terminal) on the IDE,

Run->Edit Configurations->Applications
Click on the configuration tab on the right side pane
1. Enter your path for your Main class
2. Program arguments : input output (folders from input will be taken for the code and output generated respectively)

Then click on the run button for a successful run

The output folder will have the the final top k output (output)
