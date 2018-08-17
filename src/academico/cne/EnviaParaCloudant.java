package academico.cne;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;

public class EnviaParaCloudant {

	public static void main(String[] args) throws IOException {
		
		long startTime = System.currentTimeMillis();
		
		String dirOrigem = args[0];
		String outPut    = args[1];
		
		
		BufferedWriter tWriter = new BufferedWriter(new FileWriter(outPut));

		/**
		CloudantClient client = ClientBuilder.account("2ee1011a-ce7c-400b-93e0-9822e6b7c2e0-bluemix")
                .username("2ee1011a-ce7c-400b-93e0-9822e6b7c2e0-bluemix")
                .password("e0ddf884123cd2f492662c9cc3588b252c496d344767170abae1c6a19098d202")
                .build();
		*/
		
		CloudantClient client = ClientBuilder.url(new URL("http://159.89.235.38:5984"))
                .username("admin")
                .password("cnesfciunb")
                .build();
		
		
		
		File dir = new File(dirOrigem);
		
		for (File subDir : dir.listFiles()) {
			
			System.out.println("Processando Diretorio: " + subDir.getName());
			System.out.println("=================================================================");
			for (File file : subDir.listFiles()) {
				System.out.println("Inserindo Arquivo: " + file.getName());
				
				FileInputStream fileInput = new FileInputStream(file);
				byte[] content = new byte[(int) file.length()];
				fileInput.read(content);
				
				JSONArray json = new JSONArray(new String(content));
				
				for (int jIdx = 0; jIdx < json.length(); jIdx++) {
					json.getJSONObject(jIdx).getJSONObject("properties").put("regiao", subDir.getName());
					json.getJSONObject(jIdx).put("_id", json.getJSONObject(jIdx).getJSONObject("properties").getString("setor"));
				}
				
				
				Database database = client.database("censo-geo", true);
				List<Response> responses = database.bulk(json.toList());
				System.out.println("Processando Retorno Cloudant");
				
				for (Response response : responses) {
					String line = "ID: "+response.getId()+" STATUS: "+response.getStatusCode()+" ERROR: "+response.getError();
					System.out.println(line);
					
					tWriter.write(line);
					tWriter.newLine();
					
				}
					
			}
			
		}
		

		
		System.out.println("Tempo total de processamento (s): "+ ((System.currentTimeMillis() - startTime)/1000));
		
		
 	

	}

}
