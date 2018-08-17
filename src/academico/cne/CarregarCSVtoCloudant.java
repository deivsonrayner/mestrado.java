package academico.cne;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.json.JSONArray;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.google.gson.JsonObject;

public class CarregarCSVtoCloudant {

	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		/*
		String dirOrigem = args[0];
		String outPut    = args[1];
		String fieldKey  = "Cod_setor";
		String databasePrefix = "censo-agregado-";
		String splitBy = ";";
		String splitBy2 = ",";
		String[] skipDir = new String[]{"AC","AL","AM","AP","BA","CE","DF","ES","GO","MA","MG","MS","MT","PA","PB","PE","PI","PR","RJ","RN","RO","RR","RS","SC","SE","SP Capital","SP Exceto a Capital"};
		*/
		String dirOrigem = "d:\\Projects\\academico\\to-cloudant-cne";
		String outPut    = "";
		//String fieldKey  = "CNES,SERV_ESP,CLASS_SR";
		//String fieldKey  = "CNES,CODLEITO,X_ANO";
		//String fieldKey  = "CNES,IDEQUIPE,X_ANO";
		//String fieldKey  = "CNES,TIPEQUIP,CODEQUIP,X_ANO";
		//String fieldKey  = "CNES,CBO,CNS_PROF,X_ANO";
		String fieldKey  = "CNES,X_ANO";
		String databasePrefix = "";
		String splitBy = ";";
		String splitBy2 = ",";
		String[] skipDir = new String[]{"DEZ_2017_Fila","DEZ_2017_Processado"};
		int skipLines = 0;
		
		//BufferedWriter tWriter = new BufferedWriter(new FileWriter(outPut));
		BufferedReader fileReader = null;
		
		CloudantClient client = ClientBuilder.url(new URL("http://159.89.235.38:5984"))
                .username("admin")
                .password("cnesfciunb")
                .build();
		

		
		
		
		File dir = new File(dirOrigem);
		HashSet<String> ignore = new HashSet<String>(Arrays.asList(skipDir));
		
		
		for (File subDir : dir.listFiles()) {
			
			if (ignore.contains(subDir.getName())) {
				System.out.println("Ignorando Diretorio: " + subDir.getName());
				System.out.println("=================================================================");
				continue;
			}
			
			System.out.println("Processando Diretorio: " + subDir.getName());
			System.out.println("=================================================================");
			for (File file : subDir.listFiles()) {
				long start = System.currentTimeMillis();
				
				if (file.getAbsolutePath().indexOf("___PROCESSADO") > 0) {
					System.out.println("Ignorando Arquivo: " + file.getName());
					continue;
				}
				
				System.out.println("Inserindo Arquivo: " + file.getName());
				Database database = client.database(databasePrefix+file.getName().split("_")[0].toLowerCase(), true);
				
				fileReader = new BufferedReader(new FileReader(file));
				String line = null;
				String[] header = null;
				
				JSONArray jsonArray = new JSONArray();
				String[] csvLine = null;
				int lineCount = 0;
				while ((line = fileReader.readLine()) != null) {
					lineCount++;
					if (lineCount < skipLines && lineCount != 1) {
						System.out.println("Pulando Linha: "+lineCount);
						continue;
					}
					csvLine = line.split(splitBy);
					if (csvLine.length < 2) {
						String splitO = splitBy;
						splitBy = splitBy2;
						splitBy2 = splitO;
						csvLine = line.split(splitBy);
					}
					
					if (header == null) {
						header = line.split(splitBy);
						continue;
					}
					JsonObject object = new JsonObject();
					int idx = 0;
					try {
						for (; idx < csvLine.length; idx++) {
							if (header[idx].trim().isEmpty()) {
								continue;
							}
							
							object.addProperty(header[idx].replaceAll("\"", ""), csvLine[idx].replaceAll("\"", ""));
						}
					} catch(ArrayIndexOutOfBoundsException e) {
						System.out.println("Index Error: "+e.getMessage()+"  Idx:"+idx+" Line: "+lineCount);
					}
					String id = "";
					
					try {
						String[] keys = fieldKey.split(",");
						
						for (String key : keys) {
							if (!id.isEmpty()) {
								id = id + ".";
							}
							id = id + object.get(key).getAsString().replaceAll("\"", "");
						}
						
						object.addProperty("_id", id);	
					} catch (Exception e) {
						System.out.println("Index Error: "+e.getMessage()+"  Idx:"+idx+" Line: "+lineCount+" _id:"+id);
					}
					
					
					jsonArray.put(object);
					
					if (jsonArray.length() == 100) {
						List<Response> responses = CarregarCSVtoCloudant.enviarCloudant(jsonArray, database, lineCount);
						jsonArray = new JSONArray();
					}
					
				}
				
				if (jsonArray.length() > 0) {
					List<Response> responses = CarregarCSVtoCloudant.enviarCloudant(jsonArray, database, lineCount);
					jsonArray = new JSONArray();
				}
				
				System.out.println("Finalizado Processamento do Arquivo: "+file.getName());
				for (String key : sumario.keySet()) {
					System.out.println("Status: "+key+" TOTAL: "+sumario.get(key));
				}
				System.out.println("Processing Time (s): "+((System.currentTimeMillis() - start)/1000));
				System.out.println("Total Time (s): "+((System.currentTimeMillis() - startTime)/1000));
				System.out.println("=================================================================");
				sumario = new HashMap<String, Integer>();
				
				file.renameTo(new File(file.getAbsolutePath()+"___PROCESSADO"));
					
					
			}
			
		}
		
		

	}
	
	public static HashMap<String, Integer> sumario = new HashMap<String, Integer>();
	
	public static List<Response> enviarCloudant(JSONArray jsonArray, Database database, int lineCount) throws InterruptedException {
		List<Response> responses = null;
		try { 
			responses = database.bulk(jsonArray.toList());
		} catch (Exception e) {
			System.out.println("[ERROR] - "+e.getMessage()+ " - Sleeping 3s and trying again...");
			Thread.sleep(3000);
			responses = database.bulk(jsonArray.toList());
		}
			
		if (sumario.containsKey("SUMARIO")) {
			sumario.put("SUMARIO", sumario.get("SUMARIO").intValue() + responses.size());
		} else {
			sumario.put("SUMARIO", responses.size());
		}
		
		
		
		for (Response response : responses) {
			if (response.getError() != null) {
				System.out.println("[ERROR] - ID: "+response.getId()+" STATUS: "+response.getStatusCode()+" ERROR: "+response.getError() + " Line: "+lineCount);
				if (sumario.containsKey(response.getError())) {
					sumario.put(response.getError(), sumario.get(response.getError()).intValue() + 1);
				} else {
					sumario.put(response.getError(), 1);
				}
			} else {
				System.out.println("[OKOKO] - ID: "+response.getId()+" STATUS: "+response.getStatusCode()+" REV: "+response.getRev() + " Line: "+lineCount);
				
				if (sumario.containsKey("OKOKOK")) {
					sumario.put("OKOKOK", sumario.get("OKOKOK").intValue() + 1);
				} else {
					sumario.put("OKOKOK", 1);
				}
				
			}
		}
		
		
		
		return null;
	}

}
