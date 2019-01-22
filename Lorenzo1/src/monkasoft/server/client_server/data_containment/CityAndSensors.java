package monkasoft.server.client_server.data_containment;

/*Java built-in packages imports*/
import java.util.Map;
/*User-defined packages imports*/
import monkasoft.server.client_server.mapping.City;

/**
 * CityAndSensors                      Class used to aggregate City object(representation of client area of interest, destined to client) and a sensor code to id map used by server to facilitate database queries
 * @author                             Lorenzo More'
 */
public final class CityAndSensors
{
   private City clientCity;
   private Map<String,Integer> sensorsCodeIDMap;

   public CityAndSensors(City clientCity,Map<String,Integer> sensorsCodeIDMap)
   {
      this.clientCity=clientCity;
      this.sensorsCodeIDMap=sensorsCodeIDMap;
   }

   public City getClientCity()
   {
      return this.clientCity;
   }

   public Map<String,Integer> getSensorsCodeIDMap()
   {
      return this.sensorsCodeIDMap;
   }
}