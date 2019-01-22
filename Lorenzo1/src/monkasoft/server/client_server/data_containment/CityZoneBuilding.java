package monkasoft.server.client_server.data_containment;

/**
 * CityZoneBuilding                    Class used to aggregate city,zone and building names
 * @author                             Lorenzo More'
 */
public final class CityZoneBuilding
{
   private final String cityName;
   private final String zoneName;
   private final String buildingName;

   public CityZoneBuilding(String cityName,String zoneName,String buildingName)
   {
      this.cityName=cityName;
      this.zoneName=zoneName;
      this.buildingName=buildingName;
   }

   public String getCityName()
   {
      return this.cityName;
   }

   public String getZoneName()
   {
      return this.zoneName;
   }

   public String getBuildingName()
   {
      return this.buildingName;
   }
}