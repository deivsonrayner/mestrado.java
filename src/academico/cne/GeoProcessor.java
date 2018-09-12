package academico.cne;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.jillesvangurp.geo.GeoGeometry;

public class GeoProcessor {
	
public static class RelacionamentoSetor {
		
		String id = null;
		String objetoObservado = null;
		double distancia = 0;
		String setorVizinho = null;
		
	}
	
	public static class Setor {
		String id = null;
		String regiao = null;
		double centroMassLng = 0;
		double centroMassLat = 0;
		String ibge = null;
		String ibge6=null;
		double area = 0;
	}
	
	public static class GeoObjeto {
		String id = null;
		double lng = 0;
		double lat = 0;
	}
	
	public static Map<String, Setor> setoresCache = new HashMap<String, Setor>();
	public static Collection<GeoObjeto> geoObjetos = new ArrayList<GeoObjeto>();
	public static Collection<RelacionamentoSetor> relacionamentos = new ArrayList<RelacionamentoSetor>();
	
	public static void carregarSetores(String dirOrigem) throws IOException {
		File dir = new File(dirOrigem);
		
		for (File subDir : dir.listFiles()) {
			
			System.out.println("Processando Diretorio: " + subDir.getName());
			System.out.println("=================================================================");
			for (File file : subDir.listFiles()) {
				System.out.println("Avaliando Arquivo: " + file.getName());
				
				FileInputStream fileInput = new FileInputStream(file);
				byte[] content = new byte[(int) file.length()];
				fileInput.read(content);
				
				JSONArray json = new JSONArray(new String(content));
				
				for (int jIdx = 0; jIdx < json.length(); jIdx++) {
					if (!getIBGEAmostra(Integer.parseInt(json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0, 6)))) {
						System.out.println("FORA DA AMOSTRA -->  IBGE: "+json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0, 6));
						continue;
					}
					
					JSONArray coordenadas = json.getJSONObject(jIdx).getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0);
					Setor setor = new Setor();
					
					double[] ponto = PolygonUtilities.centerOfMass(PolygonUtilities.converteCoordenadasToPoint(coordenadas));
					setor.centroMassLng = ponto[0];
					setor.centroMassLat = ponto[1];
					
					double area = GeoGeometry.area(PolygonUtilities.converteCoordenadasToArray(coordenadas));
					setor.area = area;
					
					setor.regiao = subDir.getName();
					setor.ibge = json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0, 7);
					setor.ibge6 = json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0, 6);
					setor.id =  json.getJSONObject(jIdx).getJSONObject("properties").getString("setor");
					
					
					GeoProcessor.setoresCache.put(setor.id, setor);
					
				}
					
			}
			
		}
	}
	
	public static void carregarObjetos(String dirObjetos) throws IOException {
			File file = new File(dirObjetos);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			boolean firstLine = true;
			while ((line = reader.readLine()) != null) {
				if (firstLine) {
					firstLine = !firstLine;
					continue;
				}
				
				String[] lineSplit = line.split(",");
				if (lineSplit[1].equalsIgnoreCase("NA")) {
					continue;
				}
				GeoObjeto geo = new GeoObjeto();
				geo.id = lineSplit[0];
				geo.lng = Double.parseDouble(lineSplit[1]);
				geo.lat = Double.parseDouble(lineSplit[2]);
				geoObjetos.add(geo);
			}
			
		
	}

	public static void main(String[] args) throws IOException {
		
		long startTime = System.currentTimeMillis();
		
		String dirOrigem     = "C:\\projetos\\mestrado\\dados\\geojson-setores\\amostra";
		String outPut        = "C:\\projetos\\mestrado\\processamento\\censo";
		String operacao      = "1";
		String objeto        = "C:\\projetos\\mestrado\\r-projeto\\mestrado-r\\cnes-objeto-geo.csv";
		
		final double distanciaMax = 250;
		final boolean gerarSetor  = true;
		
		GeoProcessor.carregarSetores(dirOrigem);
		
		if (operacao.equalsIgnoreCase("1")) {
			executeVizinhancaSetor(distanciaMax);
			gerarArquivos(outPut, gerarSetor);
		}
		
		if (operacao.equalsIgnoreCase("2")) {
			GeoProcessor.carregarObjetos(objeto);
			executaVizinhaObjeto(distanciaMax);
			gerarArquivos(outPut, gerarSetor);
		}
		
	}
	
	public static void gerarArquivos(String outPut, boolean gerarSetor) throws IOException {
		
		BufferedWriter tWriterSetores  = new BufferedWriter(new FileWriter(outPut+"-setores.csv"));
		BufferedWriter tWriterVizinhos = new BufferedWriter(new FileWriter(outPut+"-relacionamentos.csv"));
		
		if (gerarSetor) {
			
			String line = "id,regiao,centroMassLng,centroMassLat,ibge,ibge6,area";
			tWriterSetores.write(line);
			
			
			for (Setor setor : setoresCache.values()) {
				tWriterSetores.newLine();
				line = setor.id+","+setor.regiao+","+setor.centroMassLng+","+setor.centroMassLat+","+setor.ibge+","+setor.ibge6+","+setor.area;
				tWriterSetores.write(line);
			}
			tWriterSetores.flush();
			tWriterSetores.close();
		}
		
		String line = "id,objObservado,setorVizinho,distancia";
		tWriterVizinhos.write(line);
		
		
		for (RelacionamentoSetor relacionamento : relacionamentos) {
			tWriterVizinhos.newLine();
			line = relacionamento.id+","+relacionamento.objetoObservado+","+relacionamento.setorVizinho+","+relacionamento.distancia;
			tWriterVizinhos.write(line);
		}
		tWriterVizinhos.flush();
		tWriterVizinhos.close();
	}
	
	public static void executeVizinhancaSetor(double distanciaMax) {
		
		
		for (Setor setorObservado : setoresCache.values()) {
			
			if (!getIBGEAmostra(Integer.parseInt(setorObservado.ibge6))) {
				System.out.println("FORA DA AMOSTRA -->  IBGE: "+setorObservado.ibge6);
				continue;
			}
			
			int numVizinhos = 0;
			double distanciaMaxLocal = distanciaMax;
			Map<String, Double> processados = new HashMap<String,Double>();
			
			while (numVizinhos < 3) {
				
				
					
				for (Setor setorVizinho : setoresCache.values()) {
					
					if (setorObservado.id.equalsIgnoreCase(setorVizinho.id)) {
						continue;
					}
					
					
					double distance = GeoGeometry.distance(setorObservado.centroMassLat, setorObservado.centroMassLng,
										 setorVizinho.centroMassLat, setorVizinho.centroMassLng);
					
					if (distance <= distanciaMaxLocal) {
						
						RelacionamentoSetor relacionamento = new RelacionamentoSetor();
						relacionamento.id = setorObservado.id + "." + setorVizinho.id;
						relacionamento.objetoObservado = setorObservado.id;
						relacionamento.setorVizinho = setorVizinho.id;
						relacionamento.distancia = distance;
						
						if (processados.containsKey(relacionamento.id)) {
							continue;
						}
						
						relacionamentos.add(relacionamento);
						processados.put(relacionamento.id, relacionamento.distancia);
						numVizinhos++;
						//System.out.println("RELACIONAMENTO: "+relacionamento.id+ " DISTANCIA: "+relacionamento.distancia);
					}
						
				}
				
				if (numVizinhos > 0)
					System.out.println("SETOR: "+setorObservado.id+ " COM VIZINHOS: "+numVizinhos+" A DISTANCIA: "+distanciaMaxLocal);
				
				distanciaMaxLocal += 100;
				
			}
			
			
			
		}
		
	}
	
public static void executaVizinhaObjeto(double distanciaMax) {
		
		
		for (Setor setorObservado : setoresCache.values()) {
			
			if (!getIBGEAmostra(Integer.parseInt(setorObservado.ibge6))) {
				System.out.println("FORA DA AMOSTRA -->  IBGE: "+setorObservado.ibge6);
				continue;
			}
			
			for (GeoObjeto geo : geoObjetos) {
				
				
				double distance = GeoGeometry.distance(setorObservado.centroMassLat, setorObservado.centroMassLng,
									 geo.lat, geo.lng);
				
				if (distance <= distanciaMax) {
					RelacionamentoSetor relacionamento = new RelacionamentoSetor();
					relacionamento.id = geo.id + "." + setorObservado.id;
					relacionamento.objetoObservado = geo.id;
					relacionamento.setorVizinho = setorObservado.id;
					relacionamento.distancia = distance;
					relacionamentos.add(relacionamento);
					System.out.println("RELACIONAMENTO: "+relacionamento.id+ " DISTANCIA: "+relacionamento.distancia);
				}
				
				
			}
			
		}
		
	}
	
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

}
