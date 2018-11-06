package academico.cne;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.jillesvangurp.geo.GeoGeometry;

public class GeoProcessor extends AbstractGeoProcessor {
	
public static Collection<RelacionamentoSetor> relacionamentos = new ArrayList<RelacionamentoSetor>();


	public static void main(String[] args) throws IOException, InterruptedException {
		
		long startTime = System.currentTimeMillis();
		
		String dirOrigem     = "C:\\projetos\\mestrado\\dados\\geojson-setores\\amostra";
		String operacao      = "1";
		String objeto        = "C:\\projetos\\mestrado\\dados\\final\\cnes-objeto-geo.csv";
		
		// INPUTS - OPERACAO 1 e 2 = DEFAULT
		final double distanciaMax = 250;
		final int numMinVizinhos  = 4;
		String regiao = "DF";
		final int numMaxVizinhos = 10000;
		int distanciaIncremental = 50;
		
		//final double distanciaMax = 50;
		//final int numMinVizinhos  = 1;
		//String regiao = null;
		//final int numMaxVizinhos = 1;
		//int distanciaIncremental = 25;
		
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
		
		
		final int distanciaInicial = 1500; // Distância que em geral uma pessoal percorreria com 30 minutos de caminhada.
		distanciaIncremental = 100;
		regiao = "DF";
		
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

}
