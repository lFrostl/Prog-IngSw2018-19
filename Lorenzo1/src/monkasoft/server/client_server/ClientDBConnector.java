package monkasoft.server.client_server;

/*Java built-in packages imports*/
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
/*Cassandra driver packages imports*/
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
/*User-defined packages imports*/
import monkasoft.server.client_server.mapping.*;
import monkasoft.server.client_server.data_containment.*;

/**
 * ClientDBConnector                   Static utility class to handle client-server database interactions
 * @author                             Lorenzo More'
 */
public final class ClientDBConnector
{
   /*References to Cassandra cluster and session*/
   private static Cluster cluster;
   private static Session session;
   /*Constant values used to configure Cassandra Cluster*/
   private static final String KEYSPACE_NAME="monkadb";
   private static final int LOCAL_CORE_CONNECTIONS_PER_HOST=4;
   private static final int LOCAL_MAX_CONNECTIONS_PER_HOST=10;
   private static final int REMOTE_CORE_CONNECTIONS_PER_HOST=2;
   private static final int REMOTE_MAX_CONNECTIONS_PER_HOST=4;
   private static final int HEARTBEAT_INTERVAL_SECONDS=60;
   /*Constant strings used for prepared statements*/
   private static final String AUTHENTICATION_STRING="SELECT city,zone,building FROM user WHERE username=? AND password=?;";
   private static final String ZONES_RETRIEVAL_STRING="SELECT zone FROM cityzones WHERE city=?;";
   private static final String BUILDINGS_RETRIEVAL_STRING="SELECT building FROM zonebuildings WHERE zone=?;";
   private static final String ROOMS_RETRIEVAL_STRING="SELECT room FROM buildingrooms WHERE building=?;";
   private static final String SENSORS_RETRIEVAL_STRING="SELECT sid,stype,scode FROM roomsensors WHERE room=?;";
   private static final String NON_WORKING_SENSORS_RETRIEVAL_STRING="SELECT scode FROM nonworkingsensors WHERE scode in(?);";
   private static final String SENSOR_LATEST_VALUES_RETRIEVAL_STRING="SELECT value FROM sensorsdata WHERE sid=? and tstamp>timeago(1);";
   private static final String SENSOR_N_MINUTES_AVERAGE_RETRIEVAL_STRING="SELECT AVG(value) AS average FROM sensorsdata WHERE sid=? and tstamp>timeago(?);";
   /*Prepared statements references*/
   private static PreparedStatement authenticationStatement;
   private static PreparedStatement zonesRetrievalStatement;
   private static PreparedStatement buildingsRetrievalStatement;
   private static PreparedStatement roomsRetrievalStatement;
   private static PreparedStatement sensorsRetrievalStatement;
   private static PreparedStatement nonWorkingSensorsRetrievalStatement;
   private static PreparedStatement sensorLatestValuesRetrievalStatement;
   private static PreparedStatement sensorNMinutesAverageRetrievalStatement;

   /*Empty private constructor so as to avoid class instantiation*/
   private ClientDBConnector()
   {}

   /**
    * Initializes Cassandra cluster and session and prepares common database query statements
    * In case of a bigger datacenter, a local datacenter should be specified as method parameter
    * @param serverIPAddresses         IP addresses of cluster contact points
    */
   public static void connect(Set<String> serverIPAddresses)
   {
      cluster=Cluster
      .builder()
      .addContactPoints(serverIPAddresses.toArray(new String[serverIPAddresses.size()]))
      .withRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE)
      .withPoolingOptions(new PoolingOptions().setConnectionsPerHost(HostDistance.LOCAL,LOCAL_CORE_CONNECTIONS_PER_HOST,LOCAL_MAX_CONNECTIONS_PER_HOST).setConnectionsPerHost(HostDistance.REMOTE,REMOTE_CORE_CONNECTIONS_PER_HOST,REMOTE_MAX_CONNECTIONS_PER_HOST).setHeartbeatIntervalSeconds(HEARTBEAT_INTERVAL_SECONDS))
      .withReconnectionPolicy(new ConstantReconnectionPolicy(300L))
      .withLoadBalancingPolicy(new TokenAwarePolicy(DCAwareRoundRobinPolicy.builder().build()))
      .build();
      session=cluster.connect(KEYSPACE_NAME);
      System.out.println("Successfully connected to keyspace "+KEYSPACE_NAME);
      authenticationStatement=session.prepare(AUTHENTICATION_STRING);
      zonesRetrievalStatement=session.prepare(ZONES_RETRIEVAL_STRING);
      buildingsRetrievalStatement=session.prepare(BUILDINGS_RETRIEVAL_STRING);
      roomsRetrievalStatement=session.prepare(ROOMS_RETRIEVAL_STRING);
      sensorsRetrievalStatement=session.prepare(SENSORS_RETRIEVAL_STRING);
      nonWorkingSensorsRetrievalStatement=session.prepare(NON_WORKING_SENSORS_RETRIEVAL_STRING);
      sensorLatestValuesRetrievalStatement=session.prepare(SENSOR_LATEST_VALUES_RETRIEVAL_STRING);
      sensorNMinutesAverageRetrievalStatement=session.prepare(SENSOR_N_MINUTES_AVERAGE_RETRIEVAL_STRING);
   }

   /**
    * Authenticates user and returns a representation of the values for city zone and building via "CityZoneBuilding" custom object or null if authentication data doesn't have a match in the database
    * @param username                  the username the client wants to authenticate with
    * @param password                  the password used to authenticate the user
    * @return                          CityZoneBuilding object as representation of the cityName, the zoneName/s and the buildingName/s assigned to the user if successful; null otherwise
    * @see                             CityZoneBuilding class
    */
   public static CityZoneBuilding authenticateUser(String username,String password)
   {
      Row authenticationRow=null;
      ResultSet authenticationRs=session.execute(authenticationStatement.bind(username,password));

      if((authenticationRow=authenticationRs.one())!=null)
      {
         return new CityZoneBuilding(authenticationRow.getString("city"),authenticationRow.getString("zone"),authenticationRow.getString("building"));
      }
      else
      {
         return null;
      }
   }

   /**
    * Creates and returns Client area of interest and a sensor code-->sensor ID(used later to query database for sensor data) Map via "CityAndSensors" custom object using a given CityZoneBuilding object reference
    * @param czb                       CityZoneBuilding object reference used for querying database
    * @return                          CityAndSensors representation object containing: -the created City(with all relevant zones,buildings,etc.) -the sensor code-to-id Map for relevant sensors
    * @see                             CityZoneBuilding class
    * @see                             CityAndSensors class
    * @see                             City class
    * @see                             createAllZoneBuildings method
    * @see                             createAllBuildingRoomsAndSensors method
    */
   public static CityAndSensors returnAreaofInterest(CityZoneBuilding czb)
   {
      String cityName=czb.getCityName();
      String zoneName=czb.getZoneName();
      String buildingName=czb.getBuildingName();
      City clientCity=new City(cityName);
      Map<String,Integer> sensorsCodeIDMap=new HashMap<>();

      if(zoneName.equals("0"))
      {
         ResultSet zonesRs=session.execute(zonesRetrievalStatement.bind(cityName));

         for(Row zoneRow:zonesRs)
         {
            String newZone=zoneRow.getString("zone");
            City.Zone clientZone=clientCity.addZone(newZone);

            sensorsCodeIDMap.putAll(createAllZoneBuildings(clientZone));
         }
      }
      else
      if(buildingName.equals("0"))
      {
         City.Zone clientZone=clientCity.addZone(zoneName);

         sensorsCodeIDMap=createAllZoneBuildings(clientZone);
      }
      else
      {
         City.Zone clientZone=clientCity.addZone(zoneName);
         City.Zone.Building clientBuilding=clientZone.addBuilding(buildingName);

         sensorsCodeIDMap=createAllBuildingRoomsAndSensors(clientBuilding);
      }
      return new CityAndSensors(clientCity,sensorsCodeIDMap);
   }

   /**
    * Creates all buildings of a specified target zone and returns a sensor code-->sensor ID(used later to query database for sensor data) Map
    * @param targetZone                the zone to create all buildings of
    * @return                          a string to integer Map of sensor codes to IDs
    * @see                             City class
    * @see                             createAllBuildingRoomsAndSensors method
    */
   private static Map<String,Integer> createAllZoneBuildings(City.Zone targetZone)
   {
      Map<String,Integer> sensorsCodeIDMap=new HashMap<>();
      ResultSet buildingsRs=session.execute(buildingsRetrievalStatement.bind(targetZone.getName()));

      for(Row buildingRow:buildingsRs)
      {
         String newBuilding=buildingRow.getString("building");
         City.Zone.Building targetBuilding=targetZone.addBuilding(newBuilding);

         sensorsCodeIDMap.putAll(createAllBuildingRoomsAndSensors(targetBuilding));
      }
      return sensorsCodeIDMap;
   }

   /**
    * Creates all rooms ans sensors of a specified target building and returns a sensor code-->sensor ID(used later to query database for sensor data) Map
    * @param targetBuilding            the building to create all rooms and sensors of
    * @return                          a string to integer Map of sensor codes to IDs
    * @see                             City class
    * @see                             SensorType enum
    */
   private static Map<String,Integer> createAllBuildingRoomsAndSensors(City.Zone.Building targetBuilding)
   {
      Map<String,Integer> sensorsCodeIDMap=new HashMap<>();
      ResultSet roomsRs=session.execute(roomsRetrievalStatement.bind(targetBuilding.getName()));

      for(Row roomRow:roomsRs)
      {
         String newRoom=roomRow.getString("room");
         City.Zone.Building.Room targetRoom=targetBuilding.addRoom(newRoom);

         ResultSet sensorsRs=session.execute(sensorsRetrievalStatement.bind(newRoom));

         for(Row sensorRow:sensorsRs)
         {
            int newSensorId=sensorRow.getInt("sid");
            int newSensorTypeString=sensorRow.getInt("stype");
            String newSensorCode=sensorRow.getString("scode");
            SensorType newSensorType=SensorType.getByCode(newSensorTypeString);
            City.Zone.Building.Room.Sensor clientSensor=targetRoom.addSensor(newSensorCode,newSensorType);
            sensorsCodeIDMap.put(newSensorCode,new Integer(newSensorId));
         }
      }
      return sensorsCodeIDMap;
   }

   /**
    * Removes all non-working sensors from the sensor code-->sensor ID Map
    * @param sensorsCodeIDMap          previously created map using returnAreaOfInterest method
    * @return                          filtered map containing working sensors only
    */
   public static Map<String,Integer> removeNonWorkingSensors(Map<String,Integer> sensorsCodeIDMap)
   {
      Set<String> sensorscodes=new HashSet(sensorsCodeIDMap.keySet());
      ResultSet nonWorkingSensorsRs=session.execute(nonWorkingSensorsRetrievalStatement.bind(sensorscodes));

      for(Row nonWorkingSensorRow:nonWorkingSensorsRs)
      {
         String sensorToRemove=nonWorkingSensorRow.getString("scode");
         sensorsCodeIDMap.remove(sensorToRemove);
      }
      return sensorsCodeIDMap;
   }

   /**
    * Takes the previously obtained list of working sensors and builds a sensor code-->latest minute values map 
    * @param workingSensorsCodeIDMap   previously created map after removeNonWorkingSensors method filtering
    * @return                          a string to List<Float> map of sensor codes to latest minute values
    * @see                             retrieveSensorLatestValues method
    */
   public static Map<String,List<Float>> retrieveSensorsLatestValues(Map<String,Integer> workingSensorsCodeIDMap)
   {
      Map<String,List<Float>> sensorsCodeValuesMap=new HashMap<>();

      for(Map.Entry<String,Integer> sensorCodeID:workingSensorsCodeIDMap.entrySet())
      {
         String sensorCode=sensorCodeID.getKey();
         List<Float> sensorValues=retrieveSensorLatestValues(sensorCodeID.getValue());
         sensorsCodeValuesMap.put(sensorCode,sensorValues);
      }
      return sensorsCodeValuesMap;
   }

   /**
    * Takes a sensor ID and retrieves latest minutes values corresponding to that ID
    * @param sensorID                  the ID of a working sensor
    * @return                          the List of last minute values for the sensor identified by sensorID
    */
   private static List<Float> retrieveSensorLatestValues(int sensorID)
   {
      ResultSet sensorLatestValuesRs=session.execute(sensorLatestValuesRetrievalStatement.bind(sensorID));
      List<Float> sensorValues=new ArrayList<>();

      for(Row sensorLatestValuesRow:sensorLatestValuesRs)
      {
         float rowValue=sensorLatestValuesRow.getFloat("value");
         sensorValues.add(new Float(rowValue));
      }
      return sensorValues;
   }

   /**
    * Computes a N minutes average for all working sensors and returns a sensor code to average Map
    * @param workingSensorsCodeIDMap   the map of working sensors codes to  IDs
    * @param numMinutes                number of minutes to compute the average for
    * @return                          a sensor code to sensor average map
    * @see                             retrieveSensorNMinutesAverage method
    */
   public static Map<String,Float> retrieveSensorsNMinutesAverage(Map<String,Integer> workingSensorsCodeIDMap,int numMinutes)
   {
      Map<String,Float> sensorsCodeAverageMap=new HashMap<>();

      for(Map.Entry<String,Integer> sensorCodeID:workingSensorsCodeIDMap.entrySet())
      {
         String sensorCode=sensorCodeID.getKey();
         Float sensorAverage=retrieveSensorNMinutesAverage(sensorCodeID.getValue(),numMinutes);
         sensorsCodeAverageMap.put(sensorCode,sensorAverage);
      }
      return sensorsCodeAverageMap;
   }

   /**
    * Computes a N minutes average for a single working sensor and returns a Float object
    * @param sensorID                  ID of the sensor to compute the average for
    * @param numMinutes                number of minutes to compute the average for
    * @return                          a Float object representing the average
    */
   private static Float retrieveSensorNMinutesAverage(int sensorID,int numMinutes)
   {
      ResultSet sensorNMinutesAverageRs=session.execute(sensorNMinutesAverageRetrievalStatement.bind(sensorID,numMinutes));

      Row sensorNMinutesAverageRow=sensorNMinutesAverageRs.one();
      return new Float(sensorNMinutesAverageRow.getFloat("average"));
   }
}