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
		String type = "RELACIONAMENTO";
		
	}

	public static class Estabelecimento {
		String id = null;
		int tipUnidade = 0;
		String cnes = null;
		int nivDependencia = 0;
		int vincSus = 0;
		String tpGestao = null;
		int atividade = 0;
		int turnoAt = 0;
		int atendAmbulatorial = 0;
		int centroNeoNatal = 0;
		int atendHospitalar = 0;
		int urgEmergencia = 0;
		int centroCirurgico = 0;
		int centroObstetrico = 0;
		int nivelAtendAmb = 0;
		int nivelAtendHos = 0;
		double longitude = 0;
		double latitude  = 0;
		int atencaoBasica = 0;
		int mediaComplexidade = 0;
		int altaComplexidade = 0;
		int totalEquipes = 0;
		int clientel = 0;
		int atendPr = 0;
		int ano = 0;
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
	public static Collection<Estabelecimento> estabelecimentos = new ArrayList<Estabelecimento>();
	
	public static void carregarSetores(String dirOrigem, String regiao) throws IOException {
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
					if (!getIBGEAmostra(Integer.parseInt(json.getJSONObject(jIdx).getJSONObject("properties").getString("setor").substring(0, 6)),regiao)) {
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

	public static void main(String[] args) throws IOException, InterruptedException {
		
		long startTime = System.currentTimeMillis();
		
		String dirOrigem     = "C:\\projetos\\mestrado\\dados\\geojson-setores\\amostra";
		String operacao      = "2";
		String objeto        = "C:\\projetos\\mestrado\\dados\\final\\cnes-objeto-geo.csv";
		
		// INPUTS - OPERACAO 1 e 2 = DEFAULT
		//final double distanciaMax = 250;
		//final int numMinVizinhos  = 4;
		//final String regiao = "CE";
		//final int numMaxVizinhos = 10000;
		
		final double distanciaMax = 50;
		final int numMinVizinhos  = 1;
		String regiao = null;
		final int numMaxVizinhos = 1;
		int distanciaIncremental = 25;
		
		GeoProcessor.carregarSetores(dirOrigem, regiao);
		
		if (operacao.equalsIgnoreCase("1")) {
			String outPut        = "C:\\projetos\\mestrado\\processamento\\censo";
			executeVizinhancaSetor(distanciaMax,numMinVizinhos);
			gerarArquivos(outPut, true);
		}
		
		if (operacao.equalsIgnoreCase("2")) {
			String outPut        = "C:\\projetos\\mestrado\\processamento\\estabelecimentos";
			GeoProcessor.carregarObjetos(objeto);
			executaVizinhaObjeto(distanciaMax,numMinVizinhos,numMaxVizinhos,distanciaIncremental);
			gerarArquivos(outPut, false);
		}
		
		
		// INPUTS - OPERACAO 3
		
		
		final int distanciaInicial = 500;
		distanciaIncremental = 100;
		regiao = "PR";
		
		if (operacao.equalsIgnoreCase("3")) {
			String outPut        = "C:\\projetos\\mestrado\\processamento\\setor-to-estabelecimento-gephi-"+regiao;
			
			System.out.println("[1] - CARREGAR ESTABELECIMENTOS"); 
			Thread.sleep(2000);
			
			GeoProcessor.carregarEstabelecimentos("C:\\projetos\\mestrado\\r-projeto\\mestrado-r\\to-gephi\\estabelecimentos-gephi-vertices-"+regiao+".csv");
			
			System.out.println("[2] - RELACIONAR SETORES COM A ATENCAO BASICA"); 
			Thread.sleep(2000);
			
			executarVizinhoEstabelecimento(distanciaInicial,distanciaIncremental, regiao);
			
			System.out.println("[3] - RELACIONAR ESTABELECIMENTOS COM DEMAIS NIVEIS DE COMPLEXIDADE"); 
			Thread.sleep(2000);
			
			executarVizinhoEstabelecimentoToEstabelecimento(distanciaInicial, distanciaIncremental, regiao);
			
			gerarArquivostoGephi(outPut);
		}
		
	}
	
	public static  int avaliaNivelAtencaoVizinha(Estabelecimento estabelecimento) {
		
		if (estabelecimento.mediaComplexidade == 1 & estabelecimento.altaComplexidade == 1) {
			return 0;
		}
		
		if (estabelecimento.mediaComplexidade == 1 & estabelecimento.altaComplexidade == 0) {
			return 3;
		}
		
		if (estabelecimento.mediaComplexidade == 0 & estabelecimento.altaComplexidade == 0) {
			return 2;
		}
		return 0;
		
	}
	
	
public static void executarVizinhoEstabelecimentoToEstabelecimento(int distanciaInicial, int distanciaIncremental, String regiao) {
		
		
		for (Estabelecimento estabelecimentoObservado : estabelecimentos) {
			
			int numVizinhos = 0;
			int distanciaAlvo = distanciaInicial;
			Map<String, Double> processados = new HashMap<String,Double>();
			
			while (numVizinhos == 0) {
				
				int atencaoAlvo = avaliaNivelAtencaoVizinha(estabelecimentoObservado);
				
				if (atencaoAlvo == 0 || estabelecimentoObservado.vincSus == 0)
					break;
				
				for (Estabelecimento estabelecimento : estabelecimentos) {
					
					if (((estabelecimento.atencaoBasica     == 1 && atencaoAlvo == 1) ||
						(estabelecimento.mediaComplexidade == 1 && atencaoAlvo == 2) ||
						(estabelecimento.altaComplexidade  == 1 && atencaoAlvo == 3)) && estabelecimento.vincSus == 1) {
						
						double distance = GeoGeometry.distance(estabelecimentoObservado.latitude, estabelecimentoObservado.longitude,
								 estabelecimento.latitude, estabelecimento.longitude);
			
						if (distance <= distanciaAlvo) {
							
							
							RelacionamentoSetor relacionamento = new RelacionamentoSetor();
							relacionamento.id = "ST."+estabelecimentoObservado.id + "-ST." + estabelecimento.id;
							relacionamento.objetoObservado = "ST."+estabelecimentoObservado.id;
							relacionamento.setorVizinho = "ST."+estabelecimento.id;
							relacionamento.distancia = distance;
							relacionamento.type = "ESTABELECIMENTO-TO-ESTABELECIMENTO";
							
							if (!processados.containsKey(relacionamento.id)) {
								relacionamentos.add(relacionamento);
								processados.put(relacionamento.id, relacionamento.distancia);
								numVizinhos++;
							}
						} else {
							//System.out.println("REJEITADO: "+ setorVizinho.id +" DISTANCIA: "+distance);
						}
						
					} else {
						//System.out.println("IGNORANDO ESTABELECIMENTO: "+estabelecimento.cnes+ " ATENCAO BASICA: "+ estabelecimento.atencaoBasica + " MEDIA COMPLEXIDADE: "+ estabelecimento.mediaComplexidade + " ALTA COMPLEXIDADE: "+ estabelecimento.altaComplexidade);
						continue;
					}
				}
				
				if (numVizinhos > 0)
					System.out.println("ESTABELECIMENTO: "+estabelecimentoObservado.id+ " COM VIZINHOS: "+numVizinhos+" A DISTANCIA: "+distanciaAlvo);
				
				distanciaAlvo += distanciaIncremental;
				if (distanciaInicial > 100000) {
					System.out.println("FINALIZANDO SETOR: "+estabelecimentoObservado.id+ " COM VIZINHOS: "+numVizinhos+" A DISTANCIA: "+distanciaAlvo);
					break;
				}
				
			}
			
		}
		
	}	
	
	
	
public static void executarVizinhoEstabelecimento(int distanciaInicial, int distanciaIncremental, String regiao) {
		
		
		for (Setor setor : setoresCache.values()) {
			
			if (!getIBGEAmostra(Integer.parseInt(setor.ibge6),regiao)) {
				System.out.println("FORA DA AMOSTRA -->  IBGE: "+setor.ibge6);
				continue;
			}
			
			int numVizinhos = 0;
			Map<String, Double> processados = new HashMap<String,Double>();
			int distanciaAlvo = distanciaInicial;
			
			while (numVizinhos == 0) {
				
				for (Estabelecimento estabelecimento : estabelecimentos) {
					
					if (estabelecimento.atencaoBasica == 1 && estabelecimento.vincSus == 1) {
						
						double distance = GeoGeometry.distance(setor.centroMassLat, setor.centroMassLng,
								 estabelecimento.latitude, estabelecimento.longitude);
			
						if (distance <= distanciaAlvo) {
							
							
							RelacionamentoSetor relacionamento = new RelacionamentoSetor();
							relacionamento.id = "SETOR."+setor.id + "-ST." + estabelecimento.id;
							relacionamento.objetoObservado = "SETOR."+setor.id;
							relacionamento.setorVizinho = "ST."+estabelecimento.id;
							relacionamento.distancia = distance;
							relacionamento.type = "SETOR-TO-ESTABELECIMENTO";
							
							if (!processados.containsKey(relacionamento.id)) {
								relacionamentos.add(relacionamento);
								processados.put(relacionamento.id, relacionamento.distancia);
								numVizinhos++;
							}
						} else {
							//System.out.println("REJEITADO: "+ setorVizinho.id +" DISTANCIA: "+distance);
						}
						
					}
				}
				
				if (numVizinhos > 0)
					System.out.println("SETOR: "+setor.id+ " COM VIZINHOS: "+numVizinhos+" A DISTANCIA: "+distanciaAlvo);
				
				distanciaAlvo += distanciaIncremental;
				if (distanciaAlvo > 100000) {
					System.out.println("FINALIZANDO SETOR: "+setor.id+ " COM VIZINHOS: "+numVizinhos+" A DISTANCIA: "+distanciaAlvo);
					break;
				}
				
			}
			
		}
		
	}
	
	
	public static void carregarEstabelecimentos(String dirObjetos) throws IOException {
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
			Estabelecimento estabelecimento = new Estabelecimento();
			estabelecimento.tipUnidade = lineSplit[0].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[0]);
			estabelecimento.cnes = lineSplit[1];
			estabelecimento.id = lineSplit[1];
			estabelecimento.nivDependencia = lineSplit[2].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[2]);
			estabelecimento.vincSus = lineSplit[3].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[3]);
			estabelecimento.tpGestao = lineSplit[4];
			estabelecimento.atividade = lineSplit[5].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[5]);
			estabelecimento.clientel = lineSplit[6].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[6]);
			estabelecimento.turnoAt = lineSplit[7].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[7]);
			estabelecimento.atendAmbulatorial = lineSplit[8].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[8]);
			estabelecimento.centroNeoNatal = lineSplit[9].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[9]);
			estabelecimento.atendHospitalar = lineSplit[10].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[10]);
			estabelecimento.urgEmergencia = lineSplit[11].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[11]);
			estabelecimento.centroCirurgico = lineSplit[12].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[12]);
			estabelecimento.centroObstetrico = lineSplit[13].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[13]);
			estabelecimento.nivelAtendAmb = lineSplit[14].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[14]);
			estabelecimento.nivelAtendHos = lineSplit[15].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[15]);
			estabelecimento.atendPr = lineSplit[16].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[16]);
			estabelecimento.longitude = lineSplit[21].equalsIgnoreCase("NA")?-1:Double.parseDouble(lineSplit[21]);
			estabelecimento.latitude = lineSplit[22].equalsIgnoreCase("NA")?-1:Double.parseDouble(lineSplit[22]);
			estabelecimento.ano = lineSplit[23].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[23]);
			estabelecimento.totalEquipes = lineSplit[24].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[24]);
			estabelecimento.atencaoBasica = lineSplit[25].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[25]);
			estabelecimento.mediaComplexidade = lineSplit[27].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[27]);
			estabelecimento.altaComplexidade = lineSplit[28].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[28]);
	
			estabelecimentos.add(estabelecimento);
		}
		
	
}
	
	
	public static void gerarArquivostoGephi(String outPut) throws IOException {
		
		
		BufferedWriter tWriterVizinhos = new BufferedWriter(new FileWriter(outPut+"-relacionamentos.csv"));
		
		String line = "id,Source,Target,distancia,type";
		tWriterVizinhos.write(line);
		
		
		for (RelacionamentoSetor relacionamento : relacionamentos) {
			tWriterVizinhos.newLine();
			line = relacionamento.id+","+relacionamento.objetoObservado+","+relacionamento.setorVizinho+","+relacionamento.distancia+","+relacionamento.type;
			tWriterVizinhos.write(line);
		}
		tWriterVizinhos.flush();
		tWriterVizinhos.close();
	}
	
	public static void gerarArquivos(String outPut, boolean gerarSetor) throws IOException {
		
		BufferedWriter tWriterSetores  = null;
		BufferedWriter tWriterVizinhos = new BufferedWriter(new FileWriter(outPut+"-relacionamentos.csv"));
		
		if (gerarSetor) {
			tWriterSetores  = new BufferedWriter(new FileWriter(outPut+"-setores.csv"));
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
	
	public static void executeVizinhancaSetor(double distanciaMax, int numMinVizinhos) {
		
		
		for (Setor setorObservado : setoresCache.values()) {
			
			if (!getIBGEAmostra(Integer.parseInt(setorObservado.ibge6),null)) {
				System.out.println("FORA DA AMOSTRA -->  IBGE: "+setorObservado.ibge6);
				continue;
			}
			
			int numVizinhos = 0;
			double distanciaMaxLocal = distanciaMax;
			Map<String, Double> processados = new HashMap<String,Double>();
			
			while (numVizinhos < numMinVizinhos) {
				
				
					
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
	
public static void executaVizinhaObjeto(double distanciaMax, int numMinVizinhos, int numMaxVizinhos, int distanciaIncremental) {
		
		
		for (GeoObjeto geo : geoObjetos) {
			
			int numVizinhos = 0;
			double distanciaMaxLocal = distanciaMax;
			Map<String, Double> processados = new HashMap<String,Double>();
			boolean numMax = false;
			
			while (numVizinhos < numMinVizinhos && !numMax) {
				
				for (Setor setorVizinho : setoresCache.values()) {
					
					if (!getIBGEAmostra(Integer.parseInt(setorVizinho.ibge6),null)) {
						System.out.println("FORA DA AMOSTRA -->  IBGE: "+setorVizinho.ibge6);
						continue;
					}
					
					numMax = numVizinhos >= numMaxVizinhos;
					
					if (numMax) {
						break;
					}
					
					double distance = GeoGeometry.distance(geo.lat, geo.lng,
										 setorVizinho.centroMassLat, setorVizinho.centroMassLng);
					
					if (distance <= distanciaMaxLocal) {
						
						RelacionamentoSetor relacionamento = new RelacionamentoSetor();
						relacionamento.id = geo.id + "." + setorVizinho.id;
						relacionamento.objetoObservado = geo.id;
						relacionamento.setorVizinho = setorVizinho.id;
						relacionamento.distancia = distance;
						
						if (processados.containsKey(relacionamento.id)) {
							continue;
						}
						
						relacionamentos.add(relacionamento);
						processados.put(relacionamento.id, relacionamento.distancia);
						numVizinhos++;
						
						//System.out.println("RELACIONAMENTO: "+relacionamento.id+ " DISTANCIA: "+relacionamento.distancia);
					} else {
						//System.out.println("REJEITADO: "+ setorVizinho.id +" DISTANCIA: "+distance);
					}
						
				}
				
				if (numMax)  {
					System.out.println("SETOR: "+geo.id+ " NUM_MAX: "+numMax+" A DISTANCIA: "+distanciaMaxLocal);
				}
				
				if (numVizinhos > 0)
					System.out.println("SETOR: "+geo.id+ " COM VIZINHOS: "+numVizinhos+" A DISTANCIA: "+distanciaMaxLocal);
				
				distanciaMaxLocal += distanciaIncremental;
				if (distanciaMaxLocal > 3000) {
					System.out.println("FINALIZANDO SETOR: "+geo.id+ " COM VIZINHOS: "+numVizinhos+" A DISTANCIA: "+distanciaMaxLocal);
					break;
				}
				
			}
			
		}
		
	}
	
	public static boolean getIBGEAmostra(int ibge, String regiao) {
		int[] amostra = null;
		
		if (regiao == null) {
			amostra = new int[] {231000,230350,230370,230395,230440,230765,230770,230960,230970,231085,230428,230495,230523,230625,231240,
					310930,310945,317040,520010,520017,520025, 520030, 520400, 520549, 520551, 520580, 520620, 520800, 521250, 521305, 521523, 521560, 521730, 521760, 521975, 522185, 522220, 530010,
					410020,410030,410040,410180,410230,410310,410400,410410,410420,410425,410520,410580,410620,410690,412863,410765,411125,411320,411430,411915,411910,411950,412080,412220,412120,412230,412550,412760,412788};
		}
		
		if (regiao != null && regiao.equalsIgnoreCase("CE")) {
			amostra = new int[] {231000,230350,230370,230395,230440,230765,230770,230960,230970,231085,230428,230495,230523,230625,231240};
		}
		
		if (regiao != null && regiao.equalsIgnoreCase("DF")) {
			amostra = new int[] {310930,310945,317040,520010,520017,520025, 520030, 520400, 520549, 520551, 520580, 520620, 520800, 521250, 521305, 521523, 521560, 521730, 521760, 521975, 522185, 522220, 530010};
		}
		
		if (regiao != null && regiao.equalsIgnoreCase("PR")) {
			amostra = new int[] {410020,410030,410040,410180,410230,410310,410400,410410,410420,410425,410520,410580,410620,410690,412863,410765,411125,411320,411430,411915,411910,411950,412080,412220,412120,412230,412550,412760,412788};
		}
		 
		for (int item : amostra) {
			if (item == ibge) {
				return true;
			}
		}

		return false;
	}

}
