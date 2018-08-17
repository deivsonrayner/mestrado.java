package academico.cne;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.FindByIndexOptions;
import com.cloudant.client.api.model.Response;

public class AjustaGeoCloudant {
	
	public static HttpClient client = HttpClientBuilder.create().evictExpiredConnections().build();
	public static Map<String, String> cache = new HashMap<String, String>();

	public static void main(String[] args) throws IOException {
		File geoFile = new File("d:\\Projects\\academico\\cnes_geo\\cnesnone.csv");
		
		FileInputStream fileInput = new FileInputStream(geoFile);
		String cloudantLink = "https://e4c00652-af2f-4c75-8530-4a95fe85bc3c-bluemix:160078a2fd7da84c439bfbd2b7b57fedce23238a8eaad2df10f488db79d36bf8@e4c00652-af2f-4c75-8530-4a95fe85bc3c-bluemix.cloudant.com/censo-geo/_design/dd/_geo/newGeoIndex?relation=covered_by&g=";

		CloudantClient client = ClientBuilder.account("80b65256-b5e9-4199-9ea3-079017705daf-bluemix")
                .username("80b65256-b5e9-4199-9ea3-079017705daf-bluemix")
                .password("cec8b6cb37eee2004320457081451c943e5a150ee0fe0a83e8f3fe33c5ec302e")
                .build();
		
		Database db =  client.database("cnes-main", false);
		BufferedReader fileReader = new BufferedReader(new FileReader(geoFile));
		String line = null;
		int lineCount = 0;
		Collection<String[]> geoCollection = new ArrayList<String[]>();
		
		while ((line = fileReader.readLine()) != null) {
			lineCount++;
			lineCount++;
			
			if (lineCount == 1) {
				System.out.println("Pulando Linha: "+lineCount);
				continue;
			}
			
			String[] csvLine = line.split(",");
			geoCollection.add(csvLine);
		}
		
		List<Map> response = AjustaGeoCloudant.getCNESData(1000, 0, db);
		int nextSkip = 1000;
		while (response.size() > 0) {
			List<Map> list = new ArrayList<>();
			for (Map cne : response) {
				String codCnes = (String)cne.get("CNES");
				String competencia = (String)cne.get("COMPETEN");
				String codleito = (String)cne.get("CODLEITO");
				String lat = "NA";
				String lng = "NA";
				String origem = "NA";
				for (String[] csv : geoCollection) {
					if (csv[0].replaceAll("\"", "").equalsIgnoreCase(codCnes)) {
						origem = csv[2].replaceAll("\"", "");
						lat  = csv[4].replaceAll("\"", "");
						lng  = csv[5].replaceAll("\"", "");
						break;
					}
				}
				
				System.out.println("[OKOKO] Salvando CNES: "+codCnes+" COD_LEITO:"+codleito+" COMPETENCIA:"+ competencia+ " using LAT: "+lat +" LNG: "+ lng);
				cne.put("origem_dado", origem);
				cne.put("lat", lat);
				cne.put("long", lng);
				String setor = "NA"; 
				if (!lat.equalsIgnoreCase("NA")) {
					setor = AjustaGeoCloudant.resolveCodSetor(lat, lng);
				}
				
				System.out.println("[OKOKO] Salvando CNES: "+codCnes+" using COD_SETOR: "+setor);
				
				cne.put("COD_SETOR", setor);
				list.add(cne);
				//db.update(cne);
				
					
			}
			List<Response> responses = db.bulk(list);
			int error = 0;
			int ok = 0;
			for (Response resp : responses) {
				
				if (resp.getError() != null) {
					error++;
				} else {
					ok++;
				}
				
			}
			System.out.println("[RESULT] - OK: "+ok+" ERROR: "+error);
			response = AjustaGeoCloudant.getCNESData(1000, nextSkip, db);
			nextSkip = nextSkip + 1000;
		}
		
		
		System.out.println("FIM");
	}
	
	public static String resolveCodSetor(String lat, String lng) throws ClientProtocolException, IOException {
		if (cache.containsKey(lat+"."+lng)) {
			return cache.get(lat+"."+lng);
		}
		String point = "point("+lng+"%20"+lat+")";
		String cloudantLink = "https://e4c00652-af2f-4c75-8530-4a95fe85bc3c-bluemix:160078a2fd7da84c439bfbd2b7b57fedce23238a8eaad2df10f488db79d36bf8@e4c00652-af2f-4c75-8530-4a95fe85bc3c-bluemix.cloudant.com/censo-geo/_design/dd/_geo/newGeoIndex?relation=covered_by&g=";

		String url = cloudantLink + point + "&include_docs=true&limit=1";
		
		HttpGet get = new HttpGet(url);
		HttpResponse response =  client.execute(get);
		
		if (response.getStatusLine().getStatusCode() == 200) {
			
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			response.getEntity().writeTo(output);
			JSONObject json =  new JSONObject(output.toString());
			if (json.getJSONArray("rows").length() > 0) {
				String setor = json.getJSONArray("rows").getJSONObject(0).getJSONObject("doc").getJSONObject("properties").getString("setor");
				cache.put(lat+"."+lng, setor);
				return setor;
			} else {
				cache.put(lat+"."+lng, "NA");
				return "NA";
			}
		
		} else {
			System.out.println("************* ERROR -  Reason: "+response.getStatusLine().getReasonPhrase());
			return "NA";
		}
		
	}
	
	public static List<Map> getCNESData(int limit, int skip, Database db) {
		String selector = "\"selector\":{\"lat\":\"NA\"}";
		FindByIndexOptions options = new FindByIndexOptions();
		options.limit(limit).skip(skip);
		List<Map> response = db.findByIndex(selector, Map.class,options);
		return response;
	}

}
