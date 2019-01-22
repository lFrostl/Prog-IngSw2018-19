package monkasoft.server.client_server.mapping;

/*Java built-in packages imports*/
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

/**
 * City                                Class used to represent areas of interest to users
 * @author                             Lorenzo More'
 */
public class City implements Serializable
{
   private static final long serialVersionUID=1L;

   private String cityName;
   private Map<String,Zone> cityMap;

   public City(String cityName)
   {
      this.cityName=cityName;
      this.cityMap=new HashMap<String,Zone>();
   }

   public String getName()
   {
      return this.cityName;
   }

   public void setName(String newCityName)
   {
      this.cityName=newCityName;
   }

   public Map<String,Zone> getMap()
   {
      return this.cityMap;
   }

   public Zone getZone(String zoneName)
   {
      return cityMap.get(zoneName);
   }

   public Zone addZone(String newZoneName)
   {
      Zone newZone=this.new Zone(newZoneName);
      cityMap.put(newZoneName,newZone);
      return newZone;
   }

   /*inner class Zone*/
   public class Zone
   {
      private String zoneName;
      private Map<String,Building> zoneMap;

      public Zone(String zoneName)
      {
         this.zoneName=zoneName;
         this.zoneMap=new HashMap<String,Building>();
      }

      public String getName()
      {
         return this.zoneName;
      }

      public void setName(String newZoneName)
      {
         this.zoneName=newZoneName;
      }

      public Map<String,Building> getMap()
      {
         return this.zoneMap;
      }

      public Building getBuilding(String buildingName)
      {
         return zoneMap.get(buildingName);
      }

      public Building addBuilding(String newBuildingName)
      {
         Building newBuilding=this.new Building(newBuildingName);
         zoneMap.put(newBuildingName,newBuilding);
         return newBuilding;
      }

      /*inner class Building of inner class Zone*/
      public class Building
      {
         private String buildingName;
         private Map<String,Room> buildingMap;

         public Building(String buildingName)
         {
            this.buildingName=buildingName;
            this.buildingMap=new HashMap<String,Room>();
         }

         public String getName()
         {
            return this.buildingName;
         }

         public void setName(String newBuildingName)
         {
            this.buildingName=newBuildingName;
         }

         public Map<String,Room> getMap()
         {
            return this.buildingMap;
         }

         public Room getRoom(String roomName)
         {
            return buildingMap.get(roomName);
         }

         public Room addRoom(String newRoomName)
         {
            Room newRoom=this.new Room(newRoomName);
            buildingMap.put(newRoomName,newRoom);
            return newRoom;
         }

         /*inner class Room of inner class Building of inner class Zone*/
         public class Room
         {
            private String roomName;
            private Map<String,Sensor> roomMap;

            public Room(String roomName)
            {
               this.roomName=roomName;
               this.roomMap=new HashMap<String,Sensor>();
            }

            public String getName()
            {
               return this.roomName;
            }

            public void setName(String newRoomName)
            {
               this.roomName=newRoomName;
            }

            public Map<String,Sensor> getMap()
            {
               return this.roomMap;
            }

            public Sensor getSensor(String sensorCode)
            {
               return roomMap.get(sensorCode);
            }

            public Sensor addSensor(String newSensorCode,SensorType newSensorType)
            {
               Sensor newSensor=this.new Sensor(newSensorCode,newSensorType);
               roomMap.put(newSensorCode,newSensor);
               return newSensor;
            }

            /*inner class Sensor of inner class Room of inner class Building of inner class Zone*/
            public class Sensor
            {
               private final String sensorCode;
               private final SensorType sensorType;
               private double thresholdValue;
               private String measurementUnit;

               public Sensor(String sensorCode,SensorType sensorType)
               {
                  this.sensorCode=sensorCode;
                  this.sensorType=sensorType;
                  this.thresholdValue=sensorType.getDefaultThresholdValue();
                  this.measurementUnit=sensorType.getDefaultMeasurementUnit();
               }

               public Sensor(String sensorCode,SensorType sensorType,double thresholdValue,String measurementUnit)
               {
                  this.sensorCode=sensorCode;
                  this.sensorType=sensorType;
                  this.thresholdValue=thresholdValue;
                  this.measurementUnit=measurementUnit;
               }

               public String getSensorCode()
               {
                  return this.sensorCode;
               }

               public SensorType getSensorType()
               {
                  return this.sensorType;
               }

               public double getThresholdValue()
               {
                  return this.thresholdValue;
               }

               public void setThresholdValue(double newThresholdValue)
               {
                  this.thresholdValue=newThresholdValue;
               }

               public String getMeasurementUnit()
               {
                  return this.measurementUnit;
               }

               public void setMeasurementUnit(String newMeasurementUnit)
               {
                  this.measurementUnit=newMeasurementUnit;
               }
            }
         }
      }
   }
}