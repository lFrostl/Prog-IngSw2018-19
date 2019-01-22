package monkasoft.server.client_server.mapping;

/**
 * SensorType                          Enum representing sensors types and default measurement units and thresholds
 * @author                             Lorenzo More'
 */
public enum SensorType
{
   CO2(0,"ppm",5000.0d),
   RADON(1,"pCi/L",4.0d),
   TEMPERATURE(3,"Celsius",60.0d),
   PRESSURE(4,"Millibar",1.013d),
   LIGHT(5,"Lux",200.0d),//minimum value threshold
   HUMIDITY(6,"%",18.0d);//minimum value threshold

   private final int code;
   private final String defaultMeasurementUnit;
   private final double defaultThresholdValue;

   private SensorType(int code,String defaultMeasurementUnit,double defaultThresholdValue)
   {
      this.code=code;
      this.defaultMeasurementUnit=defaultMeasurementUnit;
      this.defaultThresholdValue=defaultThresholdValue;
   }
   public int getCode()
   {
      return this.code;
   }
   public String getDefaultMeasurementUnit()
   {
      return this.defaultMeasurementUnit;
   }
   public double getDefaultThresholdValue()
   {
      return this.defaultThresholdValue;
   }
   public static SensorType getByCode(int code)
   {
      for(SensorType st:SensorType.values())
      {
         if(code==(st.getCode()))
         {
            return st;
         }
      }
      return null;
   }
}