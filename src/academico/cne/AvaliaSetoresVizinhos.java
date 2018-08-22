package academico.cne;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jillesvangurp.geo.GeoGeometry;

public class AvaliaSetoresVizinhos {
	
	public static BufferedWriter tWriterVizinhos = null;
	public static BufferedWriter tWriterArea     = null;
	
	public static boolean getIBGEAmostra(int ibge) {
		
		int[] amostra = new int[] {231000,230350,230370,230395,230440,230765,230770,230960,230970,231085,230428,230495,230523,230625,231240,
		310930,310945,317040,520010,520017,520025, 520030, 520400, 520549, 520551, 520580, 520620, 520800, 521250, 521305, 521523, 521560, 521730, 521760, 521975, 522185, 522220, 530010,
		410020,410030,410040,410180,410230,410310,410400,410410,410420,410425,410520,410580,410620,410690,412863,410765,411125,411320,411430,411915,411910,411950,412080,412220,412120,412230,412550,412760,412788};
		
		for (int item : amostra) {
			if (item == ibge) {
				return true;
			}
		}

		return false;
	}
	
	
	
	
	public static class RelacionamentoSetor {
		
		String id = null;
		String ibge = null;
		String ibge6= null;
		String setorObservado = null;
		int indiceDistancia = 0;
		double distancia = 0;
		double distanciaMass = 0;
		double setorVizCentroLat = 0;
		double setorVizCentroLng = 0;
		double setorVizCentroMassLat = 0;
		double setorVizCentroMassLng = 0;
		
		String setorVizinho = null;
		
	}
	
	public static class SetorArea {
		String setor = null;
		double setorObsCentroLat = 0;
		double setorObsCentroLng = 0;
		double setorObsCentroMassLat = 0;
		double setorObsCentroMassLng = 0;
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
			String lat = "" + point.getDouble(1);
			String lng = "" + point.getDouble(0);
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
			Double lat = point.getDouble(1);
			Double lng = point.getDouble(0);
			double[] item = new double[] {lng,lat};
			polygon[idx] = item;
		}
		
		return polygon;
	}
	
	public static Point2D[] converteCoordenadasToPoint(JSONArray json) {
		int coordSize = json.length();
		Point2D[] points = new Point2D[coordSize];
		for(int idx = 0; idx < coordSize; idx++) {
			JSONArray point = json.getJSONArray(idx);
			Double lat = point.getDouble(1);
			Double lng = point.getDouble(0);
			Point2D point2d = new Point2D.Double(lng,lat);
			points[idx] = point2d;
		}
		return points;
	}
	
	public static void writeRelationToCSV(RelacionamentoSetor relSetor, String target, boolean header, String idx) throws IOException {
		if (tWriterVizinhos == null)
			tWriterVizinhos = new BufferedWriter(new FileWriter(target+"-vizinhos.csv"));
		
		if (header) {
			String line ="idx_processamento,id_relacionamento,setor_observado,setor_vizinho,idx_distancia,distancia_mts,distancia_mass_mts,ibge,ibge6,setor_viz_centro_lat,setor_viz_centro_lng,setor_viz_centromass_lat,setor_viz_centromass_lng";
			tWriterVizinhos.write(line);
			tWriterVizinhos.newLine();
		} else {
			String line = idx+","+relSetor.id+","+relSetor.setorObservado+","+relSetor.setorVizinho+","+relSetor.indiceDistancia+","+relSetor.distancia+","+relSetor.distanciaMass+","+relSetor.ibge+","+relSetor.ibge6+","+relSetor.setorVizCentroLat+","+relSetor.setorVizCentroLng+","+relSetor.setorVizCentroMassLat+","+relSetor.setorVizCentroMassLng;
			tWriterVizinhos.write(line);
			tWriterVizinhos.newLine();
		}
		tWriterVizinhos.flush();
	}
	
	public static void writeAreaToCSV(SetorArea relSetor, String target, boolean header, int idx) throws IOException {
		if (tWriterArea == null)
			tWriterArea = new BufferedWriter(new FileWriter(target+"-area.csv"));
		
		if (header) {
			String line ="idx_processamento,setor,area_mts,ibge,ibge6,setor_centro_lat,setor_centro_lng,setor_centromass_lat,setor_centromass_lng";
			tWriterArea.write(line);
			tWriterArea.newLine();
		} else {
			String line = idx+","+relSetor.setor+","+relSetor.area+","+relSetor.ibge+","+relSetor.ibge6+","+relSetor.setorObsCentroLat+","+relSetor.setorObsCentroLng+","+relSetor.setorObsCentroMassLat+","+relSetor.setorObsCentroMassLng;
			tWriterArea.write(line);
			tWriterArea.newLine();
		}
		tWriterArea.flush();
	}
	

	public static void main(String[] args) throws ClientProtocolException, IOException, URISyntaxException {
		
		
		String cloudantLink = "https://2ee1011a-ce7c-400b-93e0-9822e6b7c2e0-bluemix:e0ddf884123cd2f492662c9cc3588b252c496d344767170abae1c6a19098d202@2ee1011a-ce7c-400b-93e0-9822e6b7c2e0-bluemix.cloudant.com/censo-geo";
		HttpClient client = HttpClientBuilder.create().evictExpiredConnections().build();
		String target = "C:\\projetos\\mestrado\\processamento\\rel-setores";
		
		int total = 314447;
		
		Collection<RelacionamentoSetor> relSetorCol = new ArrayList<RelacionamentoSetor>();
		Collection<SetorArea> setorAreaCol = new ArrayList<SetorArea>();
		
		
		try {
			
			writeAreaToCSV(null, target, true, -1);
			writeRelationToCSV(null, target, true, null);
			
			for (int idx = 2190; idx < 35761; idx++) {
				
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
						
						double[] centroPolObs = GeoGeometry.polygonCenter(converteCoordenadasToArray(coordenadas));
						double[] centroPolObsMass = PolygonUtilities.centerOfMass(converteCoordenadasToPoint(coordenadas));

						
						if ( !AvaliaSetoresVizinhos.getIBGEAmostra(Integer.parseInt(setorObservado.getJSONObject("properties").getString("setor").substring(0, 6)))) {
							System.out.println("FORA DA AMOSTRA --> SKIP: " +idx+ " IBGE: "+setorObservado.getJSONObject("properties").getString("setor").substring(0, 6));
							continue;
						}
						
						SetorArea setorArea = new SetorArea();
						setorArea.setor = setorObservado.getJSONObject("properties").getString("setor");
						setorArea.ibge  = setorObservado.getJSONObject("properties").getString("setor").substring(0,7);
						setorArea.ibge6 = setorArea.ibge.substring(0,6);
						setorArea.area  = GeoGeometry.area(converteCoordenadasToArray(coordenadas));
						setorArea.setorObsCentroLat = centroPolObs[1];
						setorArea.setorObsCentroLng = centroPolObs[0];
						setorArea.setorObsCentroMassLat = centroPolObsMass[1];
						setorArea.setorObsCentroMassLng = centroPolObsMass[0];
						
						writeAreaToCSV(setorArea, target, false, idx);
						
						//setorAreaCol.add(setorArea);
						
						//String polygon = converteCoordenadas(coordenadas);
						String point = "point("+setorArea.setorObsCentroLng+"%20"+setorArea.setorObsCentroLat+")";
						
						System.out.println("SKIP: " +idx+ " SETOR: "+setorArea.setor+" AREA (mts): "+ setorArea.area +" "+point);
						
						URI uri = new URI(cloudantLink + "/_design/indices/_geo/geo?nearest=true&include_docs=true&limit=101&g="+point);
						get = new HttpGet(uri.normalize().toString());
						response = client.execute(get);
						
						if (response.getStatusLine().getStatusCode() == 200) {
							output = new ByteArrayOutputStream();
							response.getEntity().writeTo(output);
							geoObject =  new JSONObject(output.toString());
							if (geoObject.getJSONArray("rows").length() > 0) {
								
								
								JSONArray rows = geoObject.getJSONArray("rows");
								int idxDistancia = 1;
								
								for (int idx3 = 0; idx3 < rows.length(); idx3++) {
									
									JSONObject setorVizinho = rows.getJSONObject(idx3).getJSONObject("doc");
									
									if (setorVizinho.getJSONObject("properties").getString("setor").equalsIgnoreCase(setorObservado.getJSONObject("properties").getString("setor"))) {
										continue;
									}
									
									RelacionamentoSetor relSetor = new AvaliaSetoresVizinhos.RelacionamentoSetor();
									JSONArray coordenadasVizinhas  = setorVizinho.getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0);
									
									double[] centroPolViz = GeoGeometry.polygonCenter(converteCoordenadasToArray(coordenadasVizinhas));
									double[] centroPolVizMass = PolygonUtilities.centerOfMass(converteCoordenadasToPoint(coordenadasVizinhas));
									
									relSetor.id = setorVizinho.getJSONObject("properties").getString("setor") + "." + setorObservado.getJSONObject("properties").getString("setor");
									relSetor.indiceDistancia = (idxDistancia);
									relSetor.setorObservado = setorObservado.getJSONObject("properties").getString("setor");
									relSetor.setorVizinho = setorVizinho.getJSONObject("properties").getString("setor");
									relSetor.ibge = setorObservado.getJSONObject("properties").getString("setor").substring(0, 7);
									relSetor.ibge6= relSetor.ibge.substring(0,6);
									relSetor.setorVizCentroLat = centroPolViz[1];
									relSetor.setorVizCentroLng = centroPolViz[0];
									relSetor.setorVizCentroMassLat = centroPolVizMass[1];
									relSetor.setorVizCentroMassLng = centroPolVizMass[0];
									
									relSetor.distancia = GeoGeometry.distance(centroPolObs, centroPolViz);
									relSetor.distanciaMass = GeoGeometry.distance(centroPolObsMass, centroPolVizMass);
									
									
									System.out.println("SETOR_VIZINHO: "+relSetor.setorVizinho+" INDICE: "+relSetor.indiceDistancia+ " DISTANCIA (mts): "+relSetor.distancia + " DISTANCIA Mass (MTS): "+relSetor.distanciaMass);
									writeRelationToCSV(relSetor, target, false, idx+"."+idxDistancia);
									idxDistancia++;
									//relSetorCol.add(relSetor);
									
								}
							} else {
								System.out.println("--------------> Retorno Vazio rows[] ou null  from NN Search IDX: " +idx);
							}
							
						} else {
							System.out.println("************* HTTP ERROR on Vizinhos IDX: "+idx+" URL: "+uri.normalize().toString());
						}
						
							
					} else {
						System.out.println("--------------> Retorno Vazio rows[] ou null  ROW:");
						
					}
					

					
				} else {
					System.out.println("************* HTTP ERROR");
				}
				
				
			}
		} catch (Exception e) {
			
			throw e;
		}

		System.out.println("Processamento Finalizado");
		
	}

}
