package monkasoft.server.client_server;

/*Java built-in packages imports*/
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
/*Cassandra driver packages imports*/
import com.datastax.driver.core.exceptions.DriverException;

/**
 * ClientServer                        Main server class in charge of serving clients
 * @author                             Lorenzo More'
 */
public final class ClientServer
{
   private static final int PORT_NUMBER=7777;
   private static final int THREAD_NUMBER=20;
   private static final Set<String> contactPointsIPAddresses;

   static
   {
      Set<String> temp=new HashSet<>();
      temp.add("127.0.0.1");
      contactPointsIPAddresses=Collections.unmodifiableSet(temp);
   }
   public static void main(String[] args)
   {
      ExecutorService executor=null;
      try(ServerSocket serverSocket=new ServerSocket(PORT_NUMBER);)
      {
         executor=Executors.newFixedThreadPool(THREAD_NUMBER);

         System.out.println("Server ready for clients");
         ClientDBConnector.connect(contactPointsIPAddresses);

         while(true)
         {
            Runnable clientHandler=new ClientHandler(serverSocket.accept());
            executor.execute(clientHandler);
         }
      }
      catch(IOException ioe)
      {
         System.err.println("Unable to listen on port "+PORT_NUMBER);
         System.err.println(ioe.getMessage());
      }
      finally
      {
         if(executor!=null)
         {
            executor.shutdown();
         }
      }
   }
}