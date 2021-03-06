package com.example.daniel.podcastplayer.download;

import android.support.v7.widget.RecyclerView;

import com.example.daniel.podcastplayer.data.Episode;
import com.example.daniel.podcastplayer.data.Podcast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class ResultParser {

    private static ResultParser instance = new ResultParser();
    private String desc;
    private ResultParser(){}

    public static ResultParser getInstance(){ return instance; }

    public List<Podcast> parseSearch(String json, RecyclerView rv){
        List<Podcast> result = new ArrayList<>();
        try {
            JSONObject parentObject = new JSONObject(json);
            JSONArray resultArray = parentObject.getJSONArray("results");
            for (int i = 0 ; i < resultArray.length(); i++)
                result.add(new Podcast(resultArray.getJSONObject(i),rv));
        }
        catch(JSONException je) { je.printStackTrace(); }

        return result;
    }

    public List<Podcast> parseTopCategory(InputStream is, RecyclerView rv){
        final List<Podcast> result = new ArrayList<>();
        try{
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document d = builder.parse(is);
            NodeList items = d.getElementsByTagName("entry");
            for (int i = 0 ; i < items.getLength(); i++){
                Element p = (Element)items.item(i);
                result.add(new Podcast(p, rv));
                //Downloader.OnPodcastParsedReceiver receiver = new Downloader.OnPodcastParsedReceiver() {
                    //@Override
                    //public void receivePodcasts(List<Podcast> podcast) {
                     //   if (podcast.size()>0)
                     //       result.add(podcast.get(0));
                    //}
                //};
                //Downloader.parsePodcasts(p.getElementsByTagName("title").item(0).getTextContent().replace(' ','+')
                  //      ,rv,receiver);
            }
        } catch (Exception e){ e.printStackTrace(); }
        return result;
    }

    public List<Episode> parseFeed(InputStream is, int podcastId){
        return parseFeed(is, Integer.MAX_VALUE, podcastId);
    }

    private List<Episode> parseFeed(InputStream is, int limit, int podcastId){
        List<Episode> result = new ArrayList<>();
        try{
            if (is != null){
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document d = builder.parse(is);

                //Get podcast description from RSS
                NodeList descList = d.getElementsByTagName("description");
                if (descList.getLength() > 0)
                    desc = descList.item(0).getTextContent();

                //Parse episodes from RSS
                NodeList episodes = d.getElementsByTagName("item");
                for (int i = 0; i < episodes.getLength(); i++) {
                    Episode e = new Episode(podcastId);
                    Element n = (Element) episodes.item(i);
                    e.setEpTitle(n.getElementsByTagName("title").item(0).getTextContent());

                    e.setEpDate(getDate(n.getElementsByTagName("pubDate").item(0).getTextContent()));
                    if (n.getElementsByTagName("itunes:duration").item(0) != null)
                        e.setLength(getMiliseconds(n.getElementsByTagName("itunes:duration").item(0).getTextContent()));
                    else
                        e.setLength(getMiliseconds("0:00"));
                    if (n.getElementsByTagName("description").item(0) != null)
                        e.setDescription(n.getElementsByTagName("description").item(0).getTextContent());
                    else
                        if (n.getElementsByTagName("itunes:summary").item(0) != null)
                            e.setDescription(n.getElementsByTagName("itunes:summary").item(0).getTextContent());

                    Element url = (Element) n.getElementsByTagName("enclosure").item(0);
                    //e.setLength(Integer.valueOf(url.getAttribute("length")));
                    e.setEpURL(url.getAttribute("url"));

                    if (url.getAttribute("type").matches("audio/(.*)"))
                        result.add(e);

                    if (result.size() == limit) break;
            }
            }
        } catch (Exception e) { e.printStackTrace();}

        return result;
    }

    private int getMiliseconds(String duration){
        int result = 0;
        if (duration.indexOf(':') > 0) {
            int timeComponents = (duration.length() > 5) ? 2 : 1;   //it can come as 2(mins):30(secs)
            for (int i = timeComponents; i >= 0; i--) {
                result = result + getTimeComponent(duration) * (int) Math.pow(60, i);
                duration = duration.substring(duration.indexOf(':') + 1);
            }
        } else result = Integer.valueOf(duration);  //ya viene en segundos la duracion
        return result * 1000;
    }

    //get either hour, minute or second, adding until finding a :
    private int getTimeComponent(String time){
        int index = 0;
        StringBuilder aux = new StringBuilder();
        while (index < time.length() && time.charAt(index)!=':'){
            aux.append(time.charAt(index));
            index++;
        }
        return Integer.parseInt(aux.toString());
    }

    public String getDesc() {
        return desc;
    }

    //Remove the timezone data and hour
    private String getDate(String pubDate){
        int index = pubDate.length() - 1;
        int ammount = 2;
        while (ammount > 0){
            index--;
            if (pubDate.charAt(index)==' ')
                ammount = ammount - 1;
        }

        String result = pubDate.substring(5,index);//.replace(' ','-');
        SimpleDateFormat ogFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        Date d = null;
        try{
            d = ogFormat.parse(result);
        } catch(ParseException e){ e.printStackTrace(); }
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return dbFormat.format(d);
    }


}
