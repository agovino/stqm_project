# 1. Sample Solution for the Worksheet "OO Integration Strategies"

## Task 1
The sample demonstrates two ways to test abstract classes:
1. by deriving a concrete class and write tests for the concrete class
2. by using mocking frameworks without the need to derive a concrete class

## Task 2
The sample shows how integration errors can be detected by writing additional integration tests.
There are two failing tests which reveal the integration errors, that were both fixed 
- passing firstName, name in wrong order to Bill constructor in DbMRSServices.createRental
- moving creation of Rental instance into try-catch in DbMRSServices.createRental

There remains one problem with long user names. The Bill.print method accepts only 8 chars long names, the User class 30 chars long. This raises an IllegalArgumentException
It's up to you to find a good solution to fix this problem.

Others:

The pom file has been extended to execute also the integration test (those test classes starting with the prefix "IT"); those tests are NOT executed by maven during the Unit testing phase. This behavior is by convention of maven and allows to separate execution of integration tests from unit tests.

# 2. Sample Solution for the Worksheet "DB Testing"

## Task 1
Need to add the Failsafe plugin to POM file for integration testing with mvn
(Note: has already been added with the previous worksheet; see more details above under "others")

## Task 2
added ITMovieDao integration tests 
added additionally ITRentalDao integration tests

To run the datagenerator against the database you have to...
**1. Fill Database with Data**
1.1 Install the hsqldb server
    for convenience the directory libs contains the executable hsqlbd sever.
    you can start and stop the sever and the db client from there 
1.2 Start the hsqldb server with
   java -cp hsqldb.jar org.hsqldb.server.Server --database.0 file:mrs --dbname.0 mrs
   
   naming the dababase "mrs"
   The datagenerator uses the same name
   Note: you can start the sever using the provided batch file under windows; you can create your own shell script file for linux 
   
1.3 Run the datagenerator and fill the data

   Note: You can view the database using the povided database client (is in the same jar file)
   
   you can start the provided database client
   java -cp ./hsqldb.jar org.hsqldb.util.DatabaseManagerSwing
   
   When starting the client select "dbsever" and provide the "mrs" database.
   
   Note: 
   You can start is also using the provided batch file under windows .   
   
**2. Run tests against database**
2.1 In the MRSfx project
    Change the connection string in each Integration test (IT** class) to point to the database as specified in the datagenerator 
   (in the file HsqlDatabase.java) and build the system 
2.2 Run the tests

**3. Run the application**
3.1 The application will run but will show the wrong data
3.2 You have to change the connection string in the DbMRSServices class accordiningly
3.3 Restart application. 
    Now it will show you the data in the database.


 
   




