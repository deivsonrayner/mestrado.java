package academico.cne;

import java.awt.geom.Path2D;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.jillesvangurp.geo.GeoGeometry;

public class GeoProcessorV2 extends AbstractGeoProcessor {
	
	public static HashMap<String,Relacionamento> relacionamentos = new HashMap<String,Relacionamento>();
	
	
	public static void gerarArquivostoGephi(String outPut, HashMap<String,Relacionamento> relacionamentos) throws IOException {
		
		
		BufferedWriter tWriterVizinhos = new BufferedWriter(new FileWriter(outPut+"-relacionamentos.csv"));
		
		String line = "id,Source,Target,distancia,type";
		tWriterVizinhos.write(line);
		
		
		for (Relacionamento relacionamento : relacionamentos.values()) {
			tWriterVizinhos.newLine();
			line = relacionamento.id+","+relacionamento.source+","+relacionamento.target+","+relacionamento.distancia+","+relacionamento.type;
			tWriterVizinhos.write(line);
		}
		tWriterVizinhos.flush();
		tWriterVizinhos.close();
	}
	
	public static void gerarSetoresAsGephiNodes(String outPut, HashMap<String,Setor> setores) throws IOException {
		
		
		BufferedWriter tWriterVizinhos = new BufferedWriter(new FileWriter(outPut+"-setores.csv"));
		
		String line = "id,ibge,regiao,latitude,longitude";
		tWriterVizinhos.write(line);
		
		
		for (Setor setor : setores.values()) {
			tWriterVizinhos.newLine();
			line = setor.id+","+setor.ibge+","+setor.regiao+","+setor.centroMassLat+","+setor.centroMassLng+","+"SETOR-CENSITARIO";
			tWriterVizinhos.write(line);
		}
		tWriterVizinhos.flush();
		tWriterVizinhos.close();
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
		String censoGeoShapeDir = "C:\\projetos\\mestrado\\dados\\geojson-setores\\all";
		//String cnesGeoFile = "C:\\projetos\\mestrado\\dados\\cnes\\geo\\20180612.csv";
		String cnesGeoFile = "C:\\projetos\\mestrado\\dados\\cnes\\geo\\cnes-geo-selecao.csv";
		
		
		String inputRegiao = "ALL";
		//int inputDistanciaInicial = 300;
		//int inputDistanciaAdicional = 0;
		//double inputPercToleranciaDist = 0.2;
		//nt inputNumMinVizinhos = 1;

		Collection<EstabelecimentoLocal> estabelecimentos = AbstractGeoProcessor.carregarEstabelecimentoLocalSimples(cnesGeoFile);
		AbstractGeoProcessor.carregarSetores(censoGeoShapeDir, inputRegiao);
		
		Collection<Collection<EstabelecimentoLocal>> particoes = particionar(estabelecimentos, 30000000); 
		int particaoIdx = 0;
		System.out.println("INICIANDO UM TOTAL DE "+particoes.size() + " PARTICOES");
		Thread.currentThread().sleep(3000);
		
		for (Collection<EstabelecimentoLocal> particao : particoes) {
			final int finalParticao = particaoIdx++;
			
			Thread t = new Thread( new Runnable() {
				
				@Override
				public void run() {
					
					//HashMap<String,Relacionamento> relacionamentos = GeoProcessorV2.processarRelacionamentosInsideness(particao, finalParticao);
					HashMap<String,Relacionamento> relacionamentos = GeoProcessorV2.processarRelacionamentosMenorDistTolerancia(particao, 300, 0.2, finalParticao);
					try {
						//GeoProcessorV2.gerarArquivostoGephi("C:\\projetos\\mestrado\\dados\\final\\pmaq\\"+inputRegiao+"link-insideness-v1-"+finalParticao, relacionamentos);
						GeoProcessorV2.gerarArquivostoGephi("C:\\projetos\\mestrado\\dados\\final\\pmaq\\"+inputRegiao+"link-menordistancia-v1-"+finalParticao, relacionamentos);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			});
			
			t.start();
		}
		
	}
	
	private static Collection<Collection<EstabelecimentoLocal>> particionar(Collection<EstabelecimentoLocal> collection, int particoes) {
		Collection<Collection<EstabelecimentoLocal>> retorno = new ArrayList<Collection<EstabelecimentoLocal>>();
		Collection<EstabelecimentoLocal> innerCollection = new ArrayList<EstabelecimentoLocal>();
		
		for (EstabelecimentoLocal obj : collection) {
			innerCollection.add(obj);
			
			if ((innerCollection.size() % particoes) == 0) {
				retorno.add(innerCollection);
				innerCollection = new ArrayList<EstabelecimentoLocal>();
			}
		}
		
		if (innerCollection.size() > 0) {
			retorno.add(innerCollection);
		}
		
		return retorno;
	}
	
	/**
	 * 
	 * @param estabelecimentos
	 * @param distanciaInicial
	 * @param numMinVizinhos
	 * @param distanciaAdicional
	 * @param PercToleranciaDist 
	 * @param tipoDistancia (1 - Vizinhos dentro de um Raio / 2 - O mais próximo centro de massa / 3 - Insideness)
	 */
	
	public static HashMap<String,Relacionamento> processarRelacionamentosInsideness(Collection<EstabelecimentoLocal> estabelecimentos, int particao) {

		int count = 1;
		
		HashMap<String,Relacionamento> relacionamentos = new HashMap<String, Relacionamento>();
		
		for (EstabelecimentoLocal estabelecimento : estabelecimentos) {

			for (Setor setor : setoresCache.values()) {

				boolean match = false;
				double distance = GeoGeometry.distance(estabelecimento.latitude, estabelecimento.longitude,
						setor.centroMassLat, setor.centroMassLng);

				Path2D path = new Path2D.Double();
				path.moveTo(setor.coordenadas[0].getX(), setor.coordenadas[0].getY()); // primeiro ponto

				for (int i = 1; i < setor.coordenadas.length; i++) {
					path.lineTo(setor.coordenadas[i].getX(), setor.coordenadas[i].getY());
				}
				path.closePath();
				match = path.contains(estabelecimento.longitude, estabelecimento.latitude);
				distance = GeoGeometry.distance(estabelecimento.latitude, estabelecimento.longitude,
						setor.centroMassLat, setor.centroMassLng);

				if (match) {

					Relacionamento relacionamento = new Relacionamento();

					relacionamento.id =  estabelecimento.id + "-" + setor.id;

					relacionamento.source = estabelecimento.id;
					relacionamento.target = setor.id;
					relacionamento.distancia = distance;
					relacionamento.type = "ESTABELECIMENTO-INTO-SETOR";

					relacionamentos.put(relacionamento.id, relacionamento);

					//System.out.println("[ACEITANDO] SETOR: " + setor.id + " RELACIONADO COM: " + estabelecimento.id
						//	+ " DISTANCIA: " + distance + " SETOR.LTD: "+setor.centroMassLat + " SETOR.LNG: "+ setor.centroMassLng + " ESTABELECIMENTO.LTD: "+ estabelecimento.latitude + " ESTABELECIMENTO.LNG : "+ estabelecimento.longitude);
					
					break;

				} else {
					// System.out.println("[IGNORANDO] SETOR: " + setor.id
					// + " RELACIONADO COM: " + estabelecimento.id + " DISTANCIA: " + distance);
				}

			}
			count++;
			if ((count % 15) == 0)
				System.out.println("[ENCERRANDO - Particao: "+ particao + " Registro: " + count +" ]");

		}
		
		return relacionamentos;

	}
	
	/**
	 * 
	 * @param estabelecimentos
	 * @param distanciaInicial
	 * @param numMinVizinhos
	 * @param distanciaAdicional
	 * @param PercToleranciaDist 
	 * @param tipoDistancia (1 - Vizinhos dentro de um Raio / 2 - O mais próximo centro de massa / 3 - Insideness)
	 */
	
	public static HashMap<String,Relacionamento> processarRelacionamentosMenorDistTolerancia(Collection<EstabelecimentoLocal> estabelecimentos, int distanciaInicial, double percToleranciaDist, int particao) {
		
		HashMap<String,Relacionamento> relacionamentos = new HashMap<String, Relacionamento>();
		int count = 1;
		int total = setoresCache.values().size();
		
		for (Setor setor : setoresCache.values()) {
						
			double menorDistancia = 1000000000;
			int numVizinhos = 0;
			double raioPesquisa = distanciaInicial;
			
			boolean continuar = true;
			boolean aumentouRaio = false;
			
			while (continuar) {	
				
				
				for (EstabelecimentoLocal estabelecimento : estabelecimentos) {
					
					boolean match = false;
					double distance = GeoGeometry.distance(estabelecimento.latitude, estabelecimento.longitude, setor.centroMassLat, setor.centroMassLng);
					
					
					match = distance < raioPesquisa;
					menorDistancia = Math.min(distance, menorDistancia);
					
					if (aumentouRaio) {
						continuar = false;
					}
					
					
					if (match) {
						
						Relacionamento relacionamento = new Relacionamento();
						
						relacionamento.id = setor.id+"-"+estabelecimento.id;
						
						relacionamento.source = setor.id;
						relacionamento.target = estabelecimento.id;
						relacionamento.distancia = distance;
						relacionamento.type = "SETOR-TO-ESTABELECIMENTO";
						
						relacionamentos.put(relacionamento.id,relacionamento);
						
						System.out.println("[PARTICAO:"+particao+" ACEITANDO] SETOR: " + setor.id
								+ " RELACIONADO COM: " + estabelecimento.id + " DISTANCIA: " + distance+ " RAIO: "+raioPesquisa);
						
					} else {
						//System.out.println("[PARTICAO:"+particao+"IGNORANDO] SETOR: " + setor.id
								//+ " RELACIONADO COM: " + estabelecimento.id + " DISTANCIA: " + distance);
					}
					
					
				}

				raioPesquisa = (int)(menorDistancia + (menorDistancia * percToleranciaDist));
				aumentouRaio = true;
				
			}
				
			System.out.println("     ["+total+" / "+ count++ +"]");
		}
		
		return relacionamentos;
		
	}
	
	/**
	 * 
	 * @param estabelecimentos
	 * @param distanciaInicial
	 * @param numMinVizinhos
	 * @param distanciaAdicional
	 * @param PercToleranciaDist 
	 * @param tipoDistancia (1 - Vizinhos dentro de um Raio / 2 - O mais próximo centro de massa / 3 - Insideness)
	 */
	
	public static void processarRelacionamentosViaArco(Collection<EstabelecimentoLocal> estabelecimentos, int distanciaInicial, int distanciaAdicional) {
		
		for (Setor setor : setoresCache.values()) {
						
			double menorDistancia = 1000000000;
			int numVizinhos = 0;
			double raioPesquisa = distanciaInicial;
			Map<String, Double> processados = new HashMap<String, Double>();
			
			while (numVizinhos < 1) {
				
				
				
				for (EstabelecimentoLocal estabelecimento : estabelecimentos) {
					
					boolean match = false;
					double distance = GeoGeometry.distance(estabelecimento.latitude, estabelecimento.longitude, setor.centroMassLat, setor.centroMassLng);
					
					match = distance < raioPesquisa;
					
					
					if (match) {
						
						Relacionamento relacionamento = new Relacionamento();
						
					
						relacionamento.id = setor.id+"-"+estabelecimento.id;
						
						relacionamento.source = setor.id;
						relacionamento.target = estabelecimento.id;
						relacionamento.distancia = distance;
						relacionamento.type = "SETOR-TO-ESTABELECIMENTO";
						relacionamentos.put(relacionamento.id,relacionamento);
						
						processados.put(relacionamento.id, relacionamento.distancia);
						numVizinhos = processados.size();
						
						System.out.println("[ACEITANDO] SETOR: " + setor.id
								+ " RELACIONADO COM: " + estabelecimento.id + " DISTANCIA: " + distance);
						
						
					} else {
						System.out.println("[IGNORANDO] SETOR: " + setor.id
								+ " RELACIONADO COM: " + estabelecimento.id + " DISTANCIA: " + distance);
					}
					
					
				}
				
				if (numVizinhos > 0)
					System.out.println("[FIM] SETOR: " + setor.id + " RELACIONADO COM: "
							+ numVizinhos);
				
				raioPesquisa += distanciaAdicional;
				
				
			}
		}
		
	}

}
