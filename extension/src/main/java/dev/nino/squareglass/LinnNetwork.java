package dev.nino.squareglass;

import android.app.Application;
import android.net.*;
import java.io.IOException;
import java.net.*;

/** Bind only Linn sockets, never the process or TIDAL's Internet traffic. */
final class LinnNetwork implements OpenHome.NetworkBinding {
    final ConnectivityManager manager;
    LinnNetwork(Application app,Runnable changed){
        manager=(ConnectivityManager)app.getSystemService(Application.CONNECTIVITY_SERVICE);
        manager.registerNetworkCallback(new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET).build(),
            new ConnectivityManager.NetworkCallback(){
                public void onAvailable(Network n){changed.run();}
                public void onLost(Network n){changed.run();}
                public void onLinkPropertiesChanged(Network n,LinkProperties p){changed.run();}
            });
    }
    Network route(InetAddress address) throws IOException {
        Network fallback=null;
        for(Network n:manager.getAllNetworks()){
            NetworkCapabilities c=manager.getNetworkCapabilities(n);
            if(c==null || c.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                !(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)))continue;
            if(fallback==null)fallback=n;
            LinkProperties p=manager.getLinkProperties(n);
            if(p!=null)for(RouteInfo r:p.getRoutes())if(!r.isDefaultRoute() && r.matches(address))return n;
        }
        if(fallback!=null)return fallback;
        throw new NoRouteToHostException("Linn requires a local Wi-Fi or Ethernet connection");
    }
    public void bind(Socket socket,InetAddress target)throws IOException{route(target).bindSocket(socket);}
    public void bind(DatagramSocket socket,InetAddress target)throws IOException{route(target).bindSocket(socket);}
}
