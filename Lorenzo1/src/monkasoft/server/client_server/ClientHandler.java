package monkasoft.server.client_server;

/*Java built-in packages imports*/
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
/*Cassandra driver packages imports*/
import com.datastax.driver.core.exceptions.DriverException;
/*User-defined packages imports*/
import monkasoft.server.client_server.mapping.*;
import monkasoft.server.client_server.data_containment.*;

/**
 * ClientHandler                       Runnable representing commands for the client handling thread
 * @author                             Lorenzo More'
 */
public final class ClientHandler implements Runnable
{
   private final Socket socket;

   public ClientHandler(Socket socket)
   {
      System.out.println(Thread.currentThread().getName()+" created");
      this.socket=socket;
   }

   @Override
   public void run()
   {
      CityZoneBuilding czb;
      City clientCity;
      Map<String,Integer> sensorsCodeIDMap=null;
      Map<String,Float> sensorsCodeAverageMap=null;
      String clientInput;
      boolean authenticated=false;
      boolean sendingValues=false;
      Timer timer=new Timer();

      try
      (
         BufferedReader input=new BufferedReader(new InputStreamReader(socket.getInputStream()));
         BufferedWriter output=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
         ObjectOutputStream oos=new ObjectOutputStream(socket.getOutputStream());
      )
      {
         System.out.println("Thread started with name:"+Thread.currentThread().getName());
         output.write("Connected to Server");

         while((clientInput=input.readLine())!=null)
         {
            if(!authenticated)
            {
               if(clientInput.equals("start"))
               {
                  output.write("Enter Username and Password");
                  String username=input.readLine();
                  String password=input.readLine();
                  czb=ClientDBConnector.authenticateUser(username,password);

                  if(czb!=null)
                  {
                     output.write("Authenticated successfully");
                     CityAndSensors cas=ClientDBConnector.returnAreaofInterest(czb);
                     clientCity=cas.getClientCity();
                     oos.writeObject(clientCity);
                     sensorsCodeIDMap=cas.getSensorsCodeIDMap();
                     sensorsCodeIDMap=ClientDBConnector.removeNonWorkingSensors(sensorsCodeIDMap);

                     authenticated=true;
                  }
                  else
                  {
                     output.write("Incorrect username or password");
                  }
               }
            }
            else
            {
               if(sensorsCodeIDMap!=null)
               {
                  if(clientInput.equals("Average"))
                  {
                     output.write("Enter number of minutes");
                     int numMinutes=Integer.parseInt(input.readLine());
                     sensorsCodeAverageMap=ClientDBConnector.retrieveSensorsNMinutesAverage(sensorsCodeIDMap,numMinutes);
                     oos.writeObject(sensorsCodeAverageMap);
                  }
                  else if(clientInput.equals("Stop"))
                  {
                     output.write("Disconnected from Server");
                     return;
                  }
                  else if(!sendingValues)
                  {
                     timer.schedule(new ScheduleValues(oos,sensorsCodeIDMap),0,60000);
                     sendingValues=true;
                  }
               }
            }
         }
      }
      catch(IOException ioe)
      {
         System.out.println("I/O exception: "+ioe);
      }
      catch(DriverException de)
      {
         System.err.println("Database Error: "+de.getMessage());
      }
      catch(Exception e)
      {
         System.out.println("Thread generic Exception: "+e);
      }
   }

   /**
   * ScheduleValues                    class extending timer task; used to schedule data sending at regular time intervals
   */
   class ScheduleValues extends TimerTask
   {
      Map<String,List<Float>> sensorsCodeValuesMap;
      Map<String,Integer> workingSensorsCodeIDMap;
      ObjectOutputStream oos;

      public ScheduleValues(ObjectOutputStream oos,Map<String,Integer> workingSensorsCodeIDMap)
      {
         this.oos=oos;
         this.workingSensorsCodeIDMap=workingSensorsCodeIDMap;
      }

      @Override
      public void run()
      {
         try
         {
            sensorsCodeValuesMap=ClientDBConnector.retrieveSensorsLatestValues(workingSensorsCodeIDMap);
            oos.writeObject(sensorsCodeValuesMap);
         }
         catch(IOException ioe)
         {
         System.out.println("I/O exception: "+ioe);
         }
      }
   }
}