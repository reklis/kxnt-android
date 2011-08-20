package com.cibotechnology.audio;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class PlaylistRetriever {

    /**
     * Parses the PLS file format
     * 
     * @param manualUrl
     */
    public PlaylistRetriever(String manualUrl) {
        try {
            setUrl(new URL(manualUrl));
            fetchAndParse();
        } catch (Exception e) {
            setPlaylistItems(null);
            e.printStackTrace();
        }
    }

    private URL url;

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    private ArrayList<String> playlistItems;

    public ArrayList<String> getPlaylistItems() {
        return playlistItems;
    }

    public void setPlaylistItems(ArrayList<String> playlistItems) {
        this.playlistItems = playlistItems;
    }

    public void fetchAndParse() throws Exception {
        HttpURLConnection urlConnection = null;
        BufferedInputStream is = null;
        InputStreamReader reader = null;

        try {
            urlConnection = (HttpURLConnection) this.getUrl().openConnection();
            is = new BufferedInputStream(urlConnection.getInputStream());
            readStream(is);
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (null != is) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            urlConnection.disconnect();
        }
    }

    public void readStream(BufferedInputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ArrayList<String> list = new ArrayList<String>();
        String line = "";
        while ((line = reader.readLine()) != null) {
            String[] kvp = line.split("=");
            if (2 == kvp.length) {
                if (kvp[0].startsWith("File")) {
                    list.add(kvp[1]);
                }
            }
        }
        setPlaylistItems(list);
    }

    public String getFirstItem() {
        ArrayList<String> list = getPlaylistItems();
        if ((null != list) && (0 != list.size())) {
            return list.get(0);
        } else {
            return null;
        }
    }
}
