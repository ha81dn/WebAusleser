package com.ha81dn.webausleser.backend;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * Created by har on 21.04.2015.
 */

/* neue Erkenntnisse:
    - ich mache sämtlichen Scheiß mit WebViews auf dem Service-Thread direkt ohne Kaspereien
    - das ganze Drumrum (Hantier mit Datenbank und Kontrolletti von überhaupt allem) im AsyncTask
    - zur periodischen Geschichte muss ein AlarmDingens drumrumgebaut werden
    - das onStartCommand läuft beim zweiten Aufruf wieder, auch wenn der Service schon läuft;
      über doppelte Geschichten muss man sich keine Sorgen machen
    - man muss einfach in die Datenbank reinhantieren, wat wie doppelt und wann auflief
*/

public class WebService extends Service {

    public WebView wv;
    AsyncTask<Context, Void, Void> at;
    Handler handler;

    @Override
    public void onCreate() {
        // Handler will get associated with the current thread,
        // which is the main thread.
        handler = new Handler();
        super.onCreate();
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /*Runnable r = new Runnable() {
            @Override
            public void run() {
                wv = new WebView(getApplicationContext());
                WebSettings webSettings = wv.getSettings();
                webSettings.setJavaScriptEnabled(true);
                MyClient client = new MyClient(wv);
                JIFace iface = new JIFace(2);
                wv.setWebViewClient(client);
                wv.addJavascriptInterface(iface, "droid");
                wv.getSettings().setAppCacheEnabled(false);
                Toast.makeText(getApplicationContext(), "loadUrl", Toast.LENGTH_LONG).show();
                wv.loadUrl("http://www.spiegel.de");
            }
        };
        runOnUiThread(r);*/
        /*at = new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context... contexts) {
                if (contexts[0] == null) Toast.makeText(getApplicationContext(), "null context", Toast.LENGTH_LONG).show();
                if (contexts[0] != null) Toast.makeText(getApplicationContext(), "non-null context", Toast.LENGTH_LONG).show();
                Looper.prepare();
                wv = new WebView(contexts[0]);
                WebSettings webSettings = wv.getSettings();
                webSettings.setJavaScriptEnabled(true);
                MyClient client = new MyClient(wv);
                JIFace iface = new JIFace(2);
                wv.setWebViewClient(client);
                wv.addJavascriptInterface(iface, "droid");
                wv.getSettings().setAppCacheEnabled(false);
                Toast.makeText(getApplicationContext(), "loadUrl", Toast.LENGTH_LONG).show();
                wv.loadUrl("http://www.spiegel.de");
                return null;
            }
        };
        at.execute(getApplicationContext());*/

        wv = new WebView(getApplicationContext());
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        MyClient client = new MyClient(wv);
        JIFace iface = new JIFace(2);
        wv.setWebViewClient(client);
        wv.addJavascriptInterface(iface, "droid");
        wv.getSettings().setAppCacheEnabled(false);
        Toast.makeText(getApplicationContext(), "loadUrl", Toast.LENGTH_LONG).show();
        wv.loadUrl("http://www.spiegel.de");


        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class MyClient extends WebViewClient {
        private GetHTML runner;
        WebView web;

        private class GetHTML implements Runnable {
            GetHTML(WebView wv) {
                web = wv;
            }

            @Override
            public void run() {
                web.loadUrl("javascript:window.droid.print(document.getElementsByTagName('html')[0].innerHTML);");
            }
        }

        public MyClient(WebView web) {
            runner = new GetHTML(web);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            //runOnUiThread(runner);
            web.loadUrl("javascript:window.droid.print(document.getElementsByTagName('html')[0].innerHTML);");
        }
    }

    private class JIFace {
        int whichPage;

        public JIFace(int p) {
            whichPage = p;
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void print(String data) {
            String tmp = "<html>"+data+"</html>";
            Toast.makeText(getApplicationContext(), tmp.substring(0, 100), Toast.LENGTH_LONG).show();
        }
    }
}
