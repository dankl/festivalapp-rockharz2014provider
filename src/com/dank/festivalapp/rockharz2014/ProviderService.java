package com.dank.festivalapp.rockharz2014;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.util.Log;
import com.dank.festivalapp.lib.Band;
import com.dank.festivalapp.lib.FlavorProvider;
import com.dank.festivalapp.lib.News;
import com.dank.festivalapp.lib.ProviderServiceBase;

public class ProviderService extends ProviderServiceBase {

	private static String festivalUrl = "http://www.rockharz-festival.com/";
	
	
	public ProviderService() 
	{
		super();
	}

	@Override
	protected String getFestivalName() 
	{
		Resources res = getResources();		
		return res.getString(R.string.app_name);
	}

	/**
	 * download the band index page and parse urls, urls are temporary stored in band description.
	 * @param page
	 */
	@Override
	protected List<Band> getBands() {
		String page = downloadFile.downloadUrl(festivalUrl + "bands/");
		
		Document doc = Jsoup.parse(page, "UTF-8");
		List<Band> bands = new ArrayList<Band>();		

		for (Element n : doc.getElementsByClass("band_item") )
		{			
			String name = n.select("a").attr("title");
			String relHref = n.select("a").attr("href"); 

			Log.d("getBands()", name + " " + relHref);

			Band band = new Band(name, relHref);
			bands.add(band);
		}
		
		return bands;
	}

	@SuppressLint("DefaultLocale")
	private String normalizeBandname(String bandname)
	{
		String res = "";
		String[] words = bandname.split(" ");
		for (int i = 0; i < words.length; i++) 
		{
			if (i > 0) res += " ";  
			res += words[i].substring(0,1).toUpperCase() + words[i].substring(1).toLowerCase();        	
		}
		return res;
	}
	
	private String getFixedBandName(String bandname, String logoUrl)
	{
		String tmp = logoUrl.substring(logoUrl.lastIndexOf("/")+1);		
		tmp = tmp.substring(0, tmp.lastIndexOf("."));
		tmp = tmp.replaceAll("-", " ");
		
		if (bandname.length() < tmp.length() )
			return bandname;
		
		return tmp;
	}
	
	/**
	 * method to make some band detail actions, e.g. in case band
	 * details are on a seconds url
	 * @param band
	 * @return
	 */
	protected Band getBandDetailed(Band band)
	{
		Log.w("parseBandInfos", band.getBandname());
		String page = downloadFile.downloadUrl(band.getDescription() );
	
		Document doc = Jsoup.parse(page, "UTF-8");

		// get band description
		Element e = doc.getElementsByClass("post").first();
		if (e != null)
		{
			Element descElem = e.getElementsByClass("entry").first();
			if (descElem != null)
			{
				String desc = descElem.text();
				band.setDescription(desc);
				Log.d("desc", desc);
			}

			// get band logo
			String logoUrl = e.getElementsByTag("img").first().attr("src");		
			String logoFileName = getBandLogo(logoUrl, band.getBandname());

			if ( logoFileName != null)
				band.setLogoFile(logoFileName);

			// fix bandname - get bandname from picture name
			String bandname = getFixedBandName(band.getBandname(), logoUrl);
			band.setBandname(normalizeBandname(bandname));
			
			// get band picture
			// TODO: there is no band picture

			// extract describing elements from the list 
			// get added date
			String date = e.getElementsByTag("small").first().text();
			band.setAddDate( extractDate(date) );

			// get flavors
			FlavorProvider fp = new FlavorProvider();		
			for (String flavor : fp.getFlavors(band.getBandname()))
				band.addFlavor(flavor);
		}

		return band;
	}
	
		
	/**
	 * returns a list of all current News for this festival
	 * @return
	 */
	@Override
	protected List<News> getNewsShort() 
	{		
		List<News> newsList = new ArrayList<News>();
		String page = downloadFile.downloadUrl(festivalUrl + "news/");
		
		Document doc = Jsoup.parse(page, "UTF-8");
		
		for (Element n : doc.getElementsByClass("newstitle") )
		{
			String subject = n.text();
			String url = n.attr("href");

			newsList.add( new News(subject, url) );

			Log.d("parseAndUpdate", subject + " " + url );				
		}
		
		return newsList;
	}

	
	public Date extractDate(String date)
	{
		date = date.replaceFirst(" ", ". ");
		
		date = date.replaceAll("st, "," " );
		date = date.replaceAll("rd, "," " );
		date = date.replaceAll("nd, "," " );
		date = date.replaceAll("th, "," " );
				
		try {
			return new SimpleDateFormat("MMM dd yyyy", Locale.GERMANY).parse(date);

		} catch (ParseException e) {			
			e.printStackTrace();
		}
		return null;		
	}
	
	/**
	 * returns details to the given news, the used url was temporary stored as message
	 * an another url  
	 * @param news
	 * @return
	 */
	protected News getNewsDetailed(News news)
	{
		String newsDetailPage = downloadFile.downloadUrl(news.getMessage());

		// preserve linebreaks
		// .replaceAll("<br />", "FestivalAppbr2n") - 
		Document doc = Jsoup.parse(newsDetailPage.replaceAll("<br />", "FestivalAppbr2n"), "UTF-8");
		
		// extract detailed message
		Element e = doc.getElementsByClass("post").first();
		String msg = e.getElementsByClass("entry").text();
		
		String resMsg = msg.replaceAll("FestivalAppbr2n", "\n") ;
		news.setMessage(resMsg);

		// extract date
		String date = e.getElementsByTag("small").first().text();
		news.setDate( extractDate(date) );
		
		return news;
	}

		
	
	@Override
	protected List<BandGigTime> getRunningOrder() {
				
		List<BandGigTime> allTimesList = new ArrayList<BandGigTime>();
		return allTimesList;	
	}
	
}
