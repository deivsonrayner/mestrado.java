package academico.cne;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jillesvangurp.geo.GeoGeometry;

public class AvaliaSetoresVizinhos {
	
	public static Collection getIBGEAmostra() {
		
		int[] amostra = new int[] {231000,230350,230370,230395,230440,230765,230770,230960,230970,231085,230428,230495,230523,230625,231240,
		310930,310945,317040,520010,520017,520025, 520030, 520400, 520549, 520551, 520580, 520620, 520800, 521250, 521305, 521523, 521560, 521730, 521760, 521975, 522185, 522220, 530010,
		410020,410030,410040,410180,410230,410310,410400,410410,410420,410425,410520,410580,410620,410690,412863,410765,411125,411320,411430,411915,411910,411950,412080,412220,412120,412230,412550,412760,412788};
		
		Collection col = new ArrayList<>();
		Collections.addAll(col, amostra);
		return col;
	}
	
	
	public static class RelacionamentoSetor {
		
		String id = null;
		String ibge = null;
		String ibge6= null;
		String setorObservado = null;
		int indiceDistancia = 0;
		double distancia = 0;
		String setorVizinho = null;
		
	}
	
	public static class SetorArea {
		String setor = null;
		String ibge = null;
		String ibge6=null;
		double area = 0;
	}
	
	public static String converteCoordenadas(JSONArray json) {
		int coordSize = json.length();
		String polygon = ""; 
		for (int idx2 = 0; idx2 < coordSize; idx2++) {
			if (polygon.isEmpty()) {
				polygon = "POLYGON((";
			} else {
				polygon += ",";
			}
			JSONArray point = json.getJSONArray(idx2);
			String lat = "" + point.getDouble(0);
			String lng = "" + point.getDouble(1);
			polygon += lat + "%20" + lng;
		}
		polygon += "))";
		
		return polygon;
	}
	
	public static double[][] converteCoordenadasToArray(JSONArray json) {
		int coordSize = json.length();
		double[][] polygon = new double[coordSize][]; 
		for (int idx = 0; idx < coordSize; idx++) {
			
			JSONArray point = json.getJSONArray(idx);
			Double lat = point.getDouble(0);
			Double lng = point.getDouble(1);
			double[] item = new double[] {lat,lng};
			polygon[idx] = item;
		}
		
		return polygon;
	}
	
	public static void writeRelationToCSV(Collection<RelacionamentoSetor> col, String target) throws IOException {
		BufferedWriter tWriter = new BufferedWriter(new FileWriter(target+"-vizinhos.csv"));
		
		String line ="id_relacionamento,setor_observado,setor_vizinho,idx_distancia,distancia_mts,ibge,ibge6";
		tWriter.write(line);
		tWriter.newLine();
		
		for (RelacionamentoSetor relSetor : col) {
			line = relSetor.id+","+relSetor.setorObservado+","+relSetor.setorVizinho+","+relSetor.indiceDistancia+","+relSetor.distancia+","+relSetor.ibge+","+relSetor.ibge6;
			tWriter.write(line);
			tWriter.newLine();
		}
		
		tWriter.flush();
	}
	
	public static void writeAreaToCSV(Collection<SetorArea> col, String target) throws IOException {
		BufferedWriter tWriter = new BufferedWriter(new FileWriter(target+"-area.csv"));
		
		String line ="setor,area_mts,ibge,ibge6";
		tWriter.write(line);
		tWriter.newLine();
		
		for (SetorArea relSetor : col) {
			line = relSetor.setor+","+relSetor.area+","+relSetor.ibge+","+relSetor.ibge6;
			tWriter.write(line);
			tWriter.newLine();
		}
		
		tWriter.flush();
	}
	

	public static void main(String[] args) throws ClientProtocolException, IOException, URISyntaxException {
		String cloudantLink = "https://2ee1011a-ce7c-400b-93e0-9822e6b7c2e0-bluemix:e0ddf884123cd2f492662c9cc3588b252c496d344767170abae1c6a19098d202@2ee1011a-ce7c-400b-93e0-9822e6b7c2e0-bluemix.cloudant.com/censo-geo";
		HttpClient client = HttpClientBuilder.create().evictExpiredConnections().build();
		String target = "D:\\mestrado\\dados\\rel.setores\\rel-setores";
		
		int total = 314447;
		
		Collection<RelacionamentoSetor> relSetorCol = new ArrayList<RelacionamentoSetor>();
		Collection<SetorArea> setorAreaCol = new ArrayList<SetorArea>();
		
		
		for (int idx = 0; idx < 314447; idx++) {
			
			String url = cloudantLink + "/_all_docs?limit=1&include_docs=true&skip="+idx;
			//String url = cloudantLink + "/_design/dd/_view/ibge?include_docs=true&key=231000"+
			HttpGet get = new HttpGet(url);
			HttpResponse response = client.execute(get);
			
			if (response.getStatusLine().getStatusCode() == 200) {
				
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				response.getEntity().writeTo(output);
				JSONObject geoObject =  new JSONObject(output.toString());
				
				
				if (geoObject.getJSONArray("rows").length() > 0) {
					
					JSONObject setorObservado = ((JSONObject) geoObject.getJSONArray("rows").get(0)).getJSONObject("doc");
					JSONArray coordenadas =  setorObservado.getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0);
					
					if ( !AvaliaSetoresVizinhos.getIBGEAmostra().contains(Integer.parseInt(setorObservado.getJSONObject("properties").getString("setor").substring(0, 6)))) {
						System.out.println("FORA DA AMOSTRA --> SKIP: " +idx+ " IBGE: "+setorObservado.getJSONObject("properties").getString("setor").substring(0, 6));
						continue;
					}
					
					SetorArea setorArea = new SetorArea();
					setorArea.setor = setorObservado.getJSONObject("properties").getString("setor");
					setorArea.ibge  = setorObservado.getJSONObject("properties").getString("setor").substring(0,7);
					setorArea.ibge6 = setorArea.ibge.substring(0,6);
					setorArea.area  = GeoGeometry.area(converteCoordenadasToArray(coordenadas));
					
					setorAreaCol.add(setorArea);
					
					String polygon = converteCoordenadas(coordenadas);
					
					System.out.println("SKIP: " +idx+ " SETOR: "+setorArea.setor+" AREA (mts): "+ setorArea.area +" "+polygon);
					
					URI uri = new URI(cloudantLink + "/_design/indices/_geo/geo?nearest=true&include_docs=true&limit=101&g="+polygon);
					get = new HttpGet(uri.normalize().toString());
					response = client.execute(get);
					
					if (response.getStatusLine().getStatusCode() == 200) {
						output = new ByteArrayOutputStream();
						response.getEntity().writeTo(output);
						geoObject =  new JSONObject(output.toString());
						if (geoObject.getJSONArray("rows").length() > 0) {
							
							
							JSONArray rows = geoObject.getJSONArray("rows");
							for (int idx3 = 0; idx3 < rows.length(); idx3++) {
								
								if (idx3 == 0) 
									continue; //o primeiro registro eh o proprio setor
								
								JSONObject setorVizinho = rows.getJSONObject(idx3).getJSONObject("doc");
								RelacionamentoSetor relSetor = new AvaliaSetoresVizinhos.RelacionamentoSetor();
								
								relSetor.id = setorVizinho.getJSONObject("properties").getString("setor") + (idx3);
								relSetor.indiceDistancia = (idx3);
								relSetor.setorObservado = setorObservado.getJSONObject("properties").getString("setor");
								relSetor.setorVizinho = setorVizinho.getJSONObject("properties").getString("setor");
								relSetor.ibge = setorObservado.getJSONObject("properties").getString("setor").substring(0, 7);
								relSetor.ibge6= relSetor.ibge.substring(0,6);
								
								JSONArray coordenadasObservadas = setorObservado.getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0);
								JSONArray coordenadasVizinhas  = setorVizinho.getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0);
								
								double[] centroPolObs = GeoGeometry.polygonCenter(converteCoordenadasToArray(coordenadasObservadas));
								double[] centroPolViz = GeoGeometry.polygonCenter(converteCoordenadasToArray(coordenadasVizinhas));
								
								relSetor.distancia = GeoGeometry.distance(centroPolObs, centroPolViz);
								
								System.out.println("SETOR_VIZINHO: "+relSetor.setorVizinho+" INDICE: "+relSetor.indiceDistancia+ " DISTANCIA (mts): "+relSetor.distancia);
								relSetorCol.add(relSetor);
								
							}
						}
						
					}
					
						
				} else {
					System.out.println("--------------> Retorno Vazio rows[] ou null  ROW:");
					
				}
				

				
			} else {
				System.out.println("************* HTTP ERROR");
			}
			
			
		}
		
		
		System.out.println("Gravando Arquivo de Relacionamento");
		if (!relSetorCol.isEmpty()) {
			AvaliaSetoresVizinhos.writeRelationToCSV(relSetorCol, target);
		}
		

		System.out.println("Gravando Arquivo de Area");
		if (!setorAreaCol.isEmpty()) {
			AvaliaSetoresVizinhos.writeAreaToCSV(setorAreaCol, target);
		}


		System.out.println("Processamento Finalizado");
		
		

	}

}
