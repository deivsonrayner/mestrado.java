package academico.cne;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

public class PreparacaoST_LinkCENSO {

	public static void main(String[] args) throws IOException, InterruptedException {
		//point(-47.42729187011719%20-15.63626194364076)
		String cnesLatLng = "D:\\Projects\\academico\\to-cloudant-cne\\CSV\\cnes-estabelecimentos_.csv";
		String cloudantLink = "https://2ee1011a-ce7c-400b-93e0-9822e6b7c2e0-bluemix:e0ddf884123cd2f492662c9cc3588b252c496d344767170abae1c6a19098d202@2ee1011a-ce7c-400b-93e0-9822e6b7c2e0-bluemix.cloudant.com/censo-geo/_design/dd/_geo/newGeoIndex?relation=covered_by&g=";
		String target = "D:\\Projects\\academico\\to-cloudant-cne\\CSV\\cnes-estabelecimentos_censo.csv";
		String log = "D:\\Projects\\academico\\CNES-ST-CENSO_3.log";
		String line = null;
		String splitBy = ",";
		Map<String, JSONObject> cache = new HashMap<String, JSONObject>();
		
		
		BufferedReader pReader = new BufferedReader(new FileReader(cnesLatLng));
		BufferedWriter tWriter = new BufferedWriter(new FileWriter(target));
		BufferedWriter tLogWriter = new BufferedWriter(new FileWriter(log));
		
		HttpClient client = HttpClientBuilder.create().evictExpiredConnections().build();
		boolean firstLine = true;
		
		String[] geoFirstLine = null;
		int count = 0;
		String firstLineStr = "";
		
		long startTime = System.currentTimeMillis();
		
		while ((line = pReader.readLine()) != null) {
			
			if (firstLine) {
				firstLineStr = line +",X_IBGE_SETOR, X_SETOR";
				tWriter.write(firstLineStr);
				tWriter.newLine();
				firstLine = false;
				continue;
			}
			
			
			
			String[] cneLine = line.split(splitBy);
			String lat = cneLine[cneLine.length-2];
			String lng = cneLine[cneLine.length-3];
			JSONObject geoObject = null;
			
			String ibge = "NA";
			String setor= "NA";
			
			//if (Integer.parseInt(cneLine[0]) <= 291411) {
			//	continue;
			//}
			
			 
			
			if (!cneLine[1].equalsIgnoreCase("NA") && !lat.equalsIgnoreCase("NA") && !lat.equalsIgnoreCase("")) {
				
				geoObject = cache.get(cneLine[1]);
				
				if (geoObject == null) {
					String point = "point("+lng+"%20"+lat+")";
					
					System.out.println("ROW: "+cneLine[1]+" Pesquisando Ponto: "+ point);
					//tLogWriter.write("ROW: "+cneLine[1]+" Pesquisando Ponto: "+ point);
					//tLogWriter.newLine();
					
					String url = cloudantLink + point + "&include_docs=true&limit=1";
					HttpGet get = new HttpGet(url);
					Thread.currentThread().sleep(50);
					HttpResponse response =  client.execute(get);
					//System.out.println("Executando URL: "+url);
					
					if (response.getStatusLine().getStatusCode() == 200) {
						
						ByteArrayOutputStream output = new ByteArrayOutputStream();
						response.getEntity().writeTo(output);
						geoObject =  new JSONObject(output.toString());
						
						cache.put(cneLine[1], geoObject);
						
						if (geoObject.getJSONArray("rows").length() > 0) {
							
							ibge = geoObject.getJSONArray("rows").getJSONObject(0).getJSONObject("doc").getJSONObject("properties").getString("setor").substring(0,7);
							setor = geoObject.getJSONArray("rows").getJSONObject(0).getJSONObject("doc").getJSONObject("properties").getString("setor");
							
							//System.out.println("Codigo IBGE: "+ibge);
							//System.out.println("Codigo setor: "+setor);	
						} else {
							System.out.println("--------------> Retorno Vazio rows[] ou null  ROW:"+ cneLine[0]);
							tLogWriter.write("--------------> Retorno Vazio rows[] ou null ROW:"+ cneLine[0]);
							tLogWriter.newLine();
						}

						
						
					} else {
						System.out.println("************* ERROR - ROW: "+ cneLine[0] +" Reason: "+response.getStatusLine().getReasonPhrase());
						tLogWriter.write("************* ERROR - ROW: "+ cneLine[0] +" Reason: "+response.getStatusLine().getReasonPhrase());
						tLogWriter.newLine();
					}
				} else {
					if (geoObject.getJSONArray("rows").length() > 0) {
						
						ibge = geoObject.getJSONArray("rows").getJSONObject(0).getJSONObject("doc").getJSONObject("properties").getString("setor").substring(0,7);
						setor = geoObject.getJSONArray("rows").getJSONObject(0).getJSONObject("doc").getJSONObject("properties").getString("setor");
						
						//System.out.println("Codigo IBGE: "+ibge);
						//System.out.println("Codigo setor: "+setor);	
					} else {
						System.out.println("--------------> Retorno Vazio rows[] ou null  ROW:"+ cneLine[0]);
						tLogWriter.write("--------------> Retorno Vazio rows[] ou null ROW:"+ cneLine[0]);
						tLogWriter.newLine();
					}
				}
				

				String newLineStr = "";
				
				int idx = 0;
				for (String item : cneLine) {
					if (newLineStr.isEmpty()) {
						newLineStr = newLineStr + item;
					} else {
						newLineStr = newLineStr + "," + item;
					}	 
					idx++;
				}
				
				newLineStr = newLineStr +","+ibge+","+setor;
				System.out.println("ROW: "+cneLine[0]+ "[OK] Escrevendo Linha: "+newLineStr);
				
				tWriter.write(newLineStr);
				tWriter.newLine();
				tWriter.flush();
				//tLogWriter.write("ROW: "+cneLine[0]+ "[OK] Escrevendo Linha: "+newLineStr);
				//tLogWriter.newLine();
				
			} else {
				
				String newLineStr = "";
				int idx = 0;
				for (String item : cneLine) {
					if (newLineStr.isEmpty()) {
						newLineStr = newLineStr + item;
					} else {
						newLineStr = newLineStr + "," + item;
					}	 
					idx++;
				}
				
				newLineStr = newLineStr +","+ibge+","+setor;
				System.out.println("[NA] ROW: "+cneLine[0]+ "Escrevendo Linha: "+newLineStr);
				tWriter.write(newLineStr);
				tWriter.newLine();
				tWriter.flush();
			}
			
			
			
			
		}
		
		System.out.println("Tempo total de processamento (s): "+ ((System.currentTimeMillis() - startTime)/1000));
		tLogWriter.write("Tempo total de processamento (s): "+ ((System.currentTimeMillis() - startTime)/1000));
		tLogWriter.newLine();

	}

}
