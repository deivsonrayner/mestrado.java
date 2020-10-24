package academico.cne.metodo;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.jillesvangurp.geo.GeoGeometry;

import academico.cne.AbstractGeoProcessor;
import academico.cne.Equipamento;
import academico.cne.Estabelecimento;
import academico.cne.Relacionamento;
import academico.cne.RelacionamentoEquipamento;
import academico.cne.Servico;
import academico.cne.Setor;

public class UnidadeToEquipamento extends AbstractGeoProcessor {
	
	public static BufferedWriter tWritterMatch = null;
	public static BufferedWriter tWritterAccepting = null;
	public static BufferedWriter tWritterNotFound = null;
	public static int interacoes = 0;
	public static int count = 0;
	
	public static void gerarArquivostoGephi(String outPut, HashMap<String,Relacionamento> relacionamentos) throws IOException {
		
		
		BufferedWriter tWriterVizinhos = new BufferedWriter(new FileWriter(outPut+"-relacionamentos.csv"));
		
		String line = "id,Source,Target,distancia,type,subtype,classifier,layer";
		tWriterVizinhos.write(line);
		
		
		for (Relacionamento relacionamento : relacionamentos.values()) {
			tWriterVizinhos.newLine();
			line = relacionamento.id+","+relacionamento.source+","+relacionamento.target+","+relacionamento.distancia+","+relacionamento.type+","+relacionamento.subType+","+relacionamento.classifier+","+relacionamento.layer;
			tWriterVizinhos.write(line);
		}
		tWriterVizinhos.flush();
		tWriterVizinhos.close();
	}
	
	public static void gerarSetoresAsGephiNodes(String outPut, Map<String,Setor> setores) throws IOException {
		
		
		BufferedWriter tWriterVizinhos = new BufferedWriter(new FileWriter(outPut));
		
		String line = "id,ibge,regiao,latitude,longitude,tipo,camada,municipio";
		tWriterVizinhos.write(line);
		
		
		for (Setor setor : setores.values()) {
			tWriterVizinhos.newLine();
			line = setor.id+","+setor.ibge6+","+setor.regiao+","+setor.centroMassLat+","+setor.centroMassLng+","+"SETOR-CENSITARIO"+","+setor.camada+","+setor.getMunicipio();
			tWriterVizinhos.write(line);
		}
		tWriterVizinhos.flush();
		tWriterVizinhos.close();
	}
	
	public static void gerarSetoresAsGeoMatriz(String outPut, Map<String,Setor> setores, boolean geoJSON) throws IOException {
		
		
		BufferedWriter tWriterVizinhos = new BufferedWriter(new FileWriter(outPut));
		
		String line = "id,ibge,regiao,latitude,longitude,tipo,municipio,camada";
		if (geoJSON) {
			line = "id,ibge,regiao,geometry,tipo,municipio,camada";
		}
		
		
		tWriterVizinhos.write(line);
		
		
		for (Setor setor : setores.values()) {
			
			for (Point2D point : setor.coordenadas) {
				tWriterVizinhos.newLine();
				if (geoJSON) {
					line = setor.id+","+setor.ibge6+","+setor.regiao+","+setor.geoJson+","+"SETOR-CENSITARIO"+","+setor.getMunicipio()+","+setor.camada;
				} else {
					line = setor.id+","+setor.ibge6+","+setor.regiao+","+point.getY()+","+point.getX()+","+"SETOR-CENSITARIO"+","+setor.getMunicipio()+","+setor.camada;
				}
				tWriterVizinhos.write(line);	
			}
			
		}
		tWriterVizinhos.flush();
		tWriterVizinhos.close();
	}
	
	public static void gerarUnidadesGephiNodes(String outPut, Collection<Estabelecimento> estabelecimentos) throws IOException {
		
		
		BufferedWriter tWriterVizinhos = new BufferedWriter(new FileWriter(outPut));
		
		String line = "id,tipoUnidade,atendAmbulatorial,centroNeoNatal,atendHospitalar,urgEmergencia,centroCirurgico,centroObstetrico,nivelAtendAmb,nivelAtendHos,atencaoBasica,mediaComplexidade,altaComplexidade,servicos,latitude,longitude,tipo,municipio";
		tWriterVizinhos.write(line);
		
		
		for (Estabelecimento estabelecimento : estabelecimentos) {
			tWriterVizinhos.newLine();
			line = estabelecimento.cnes+","+estabelecimento.descricao+","+estabelecimento.atendAmbulatorial+","+estabelecimento.centroNeoNatal+","+estabelecimento.atendHospitalar+","+estabelecimento.urgEmergencia + "," +
				   estabelecimento.centroCirurgico + "," + estabelecimento.centroObstetrico + "," + estabelecimento.nivelAtendAmb + "," + estabelecimento.nivelAtendHos + "," + estabelecimento.atencaoBasica + "," + estabelecimento.mediaComplexidade + "," + estabelecimento.altaComplexidade + "," +
				   estabelecimento.servicos + "," + estabelecimento.latitude + "," + estabelecimento.longitude + "," + "ESTABELECIMENTO"+","+estabelecimento.municipio;
			tWriterVizinhos.write(line);
		}
		tWriterVizinhos.flush();
		tWriterVizinhos.close();
	}
	
	
	public static void traceMatch(String line) throws IOException {
		
		if (tWritterMatch == null)
			tWritterMatch = new BufferedWriter(new FileWriter("C:\\projetos\\mestrado\\logs\\match.log"));
		tWritterMatch.write(line+"\n");
		tWritterMatch.flush();
	}
	
	public static void traceAccepting(String line) throws IOException {
		
		if (tWritterAccepting == null)
			tWritterAccepting = new BufferedWriter(new FileWriter("C:\\projetos\\mestrado\\logs\\accepting.log"));
		tWritterAccepting.write(line+"\n");
		tWritterAccepting.flush();
	}
	
	public static void traceNotFound(String line) throws IOException {
		
		if (tWritterNotFound == null)
			tWritterNotFound = new BufferedWriter(new FileWriter("C:\\projetos\\mestrado\\logs\\notfound.log"));
		tWritterNotFound.write(line+"\n");
		tWritterNotFound.flush();
	}
	
	public static Collection<Estabelecimento> carregarEstabelecimentosMetodo(String dirObjetos) throws IOException {
		File file = new File(dirObjetos);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		Collection<Estabelecimento> estabelecimentosRetorno = new ArrayList<Estabelecimento>();
		
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
			estabelecimento.longitude = lineSplit[24].equalsIgnoreCase("NA")?-1:Double.parseDouble(lineSplit[24]);
			estabelecimento.latitude = lineSplit[25].equalsIgnoreCase("NA")?-1:Double.parseDouble(lineSplit[25]);
			estabelecimento.ano = lineSplit[27].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[27]);
			estabelecimento.atencaoBasica = lineSplit[28].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[28]);
			estabelecimento.mediaComplexidade = lineSplit[30].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[30]);
			estabelecimento.altaComplexidade = lineSplit[31].equalsIgnoreCase("NA")?-1:Integer.parseInt(lineSplit[31]);
			estabelecimento.descricao = lineSplit[29];
			estabelecimento.uf = lineSplit[20];
			
			estabelecimentosRetorno.add(estabelecimento);
		}
		
		return estabelecimentosRetorno;
		
	
}

	public static void main(String[] args) throws IOException, URISyntaxException {
		// TODO Auto-generated method stub
		
		String censoGeoShapeDir = "C:\\projetos\\mestrado\\dados\\geojson-setores\\all";
		String cnesGeoFile = "C:\\projetos\\mestrado\\r-projeto\\mestrado-r\\metodo\\cnes.resultado-filtro-unidades-cov19.csv";
		String cnesEquipamentosFile = "C:\\projetos\\mestrado\\dados\\cnes-processado\\CNES-EQ-12-19-COV19.csv";
		
		String[] ibges = new String[] {"310930","310945","317040","314700", "520010","520017","520025", "520030", "520400", "520549", "520551", "520580", "520620", "520800", "521250", "521305", "521523", "521560", "521730", "521760", 
                "521975", "522185", "522220", "530010", "520790", "522000", "522230", "522060", "520110", "522200", "521530", "521010"};
		
		String[] ibgeRede = new String[] {"520025","520030","520549","520551","520620","520800",
                "521250","521523","521560","521760","521975","522185",
                "530010"};
		
		
		String[] codigos = new String[] {"ALL"};
		
		String[] ufs = new String[] {};
		String[] inputRegiao = new String[] {"MG_NORTE_NORDESTE","MG_LESTE","GO","DF"};
		
		int inputDistanciaInicial = 300;
		double inputPercToleranciaDist = 0.2;
		String inputLayer = "ACESSO-POTENCIAL";
		
		long time = System.currentTimeMillis();
		
		Collection<String> equipamentosProcurados = UnidadeToEquipamento.carregarEquipamentosPesquisados();
		System.out.println("[LOADING DATA] - Equipamentos Procurados carregados em: " + (System.currentTimeMillis() - time) + " (ms)");
		
		time = System.currentTimeMillis();
		Collection<Estabelecimento> estabelecimentos = UnidadeToEquipamento.carregarEstabelecimentosMetodo(cnesGeoFile);
		System.out.println("[LOADING DATA] - Estabelecimentos carregados em: " + (System.currentTimeMillis() - time) + " (ms)");
	
		time = System.currentTimeMillis();
		Map<String, Setor> setores = AbstractGeoProcessor.carregarSetoresMetodo(censoGeoShapeDir, inputRegiao, ibges);
		//Map<String, Setor> setores = null;
		System.out.println("[LOADING DATA] - Setores carregados em: " + (System.currentTimeMillis() - time) + " (ms)");
		
		time = System.currentTimeMillis();
		Map<String, Collection<Equipamento>> equipamentos = AbstractGeoProcessor.carregarEquipamentosMetodo(cnesEquipamentosFile, ufs, ibges, new String[] {"ALL"});
		System.out.println("[LOADING DATA] - Equipamentos carregados em: " + (System.currentTimeMillis() - time) + " (ms)");
		
		Map<String, Setor> setoresRede = new HashMap<String, Setor>();
		for (Setor setor : setores.values()) {
			for (String ibge : ibgeRede) {
				if (setor.ibge6.equalsIgnoreCase(ibge)) {
					setor.camada = 1;
					setoresRede.put(setor.id, setor);
					break;
				}
			}
		}
		
	
		Collection<Collection> particoes = particionar(setoresRede.values(), 1000); 
		int particaoIdx = 0;
		interacoes = setoresRede.size();
		
		for (Collection<Setor> particao : particoes) {
			final int finalParticao = particaoIdx++;
			
			Thread t = new Thread( new Runnable() {
				
				@Override
				public void run() {
					
					HashMap<String, Relacionamento> relacionamentos = null;
					try {
						relacionamentos = UnidadeToEquipamento.processarRelacionamentosMenorDistTolerancia(particao, estabelecimentos, equipamentos, equipamentosProcurados, inputDistanciaInicial, inputPercToleranciaDist, inputLayer, finalParticao);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					try {
						UnidadeToEquipamento.gerarArquivostoGephi("C:\\projetos\\mestrado\\r-projeto\\mestrado-r\\metodo\\rede.particoes\\relacionamentos_PARTICAO_"+finalParticao, relacionamentos);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			});
			
			t.start();
		}
	
		gerarSetoresAsGephiNodes("C:\\projetos\\mestrado\\r-projeto\\mestrado-r\\metodo\\rede\\covid\\setores.csv", setoresRede);
		gerarSetoresAsGeoMatriz("C:\\projetos\\mestrado\\r-projeto\\mestrado-r\\metodo\\rede\\covid\\setores-matriz-geo.csv", setores, true);
		gerarSetoresAsGeoMatriz("C:\\projetos\\mestrado\\r-projeto\\mestrado-r\\metodo\\rede\\covid\\setores-matriz-rede-geo.csv", setoresRede, true);
		gerarSetoresAsGeoMatriz("C:\\projetos\\mestrado\\r-projeto\\mestrado-r\\metodo\\rede\\covid\\setores-matriz.csv", setores, false);
		gerarSetoresAsGeoMatriz("C:\\projetos\\mestrado\\r-projeto\\mestrado-r\\metodo\\rede\\covid\\setores-matriz-rede.csv", setoresRede, false);
		gerarUnidadesGephiNodes("C:\\projetos\\mestrado\\r-projeto\\mestrado-r\\metodo\\rede\\covid\\unidades.csv", estabelecimentos);
		
		
		
	}
	
	private static Collection<String> carregarClassificadoresPesquisados() throws IOException, URISyntaxException {
		Collection<String> classificadores = new ArrayList<String>();
		URL classificadoresURL = ClassLoader.getSystemResource("parametro-rede-classificadores.csv");
		classificadores = FileUtils.readLines(new File(classificadoresURL.toURI()));
		return classificadores;
	}
	
	private static Collection<String> carregarEquipamentosPesquisados() throws IOException, URISyntaxException {
		Collection<String> classificadores = new ArrayList<String>();
		URL classificadoresURL = ClassLoader.getSystemResource("equipamentos.csv");
		classificadores = FileUtils.readLines(new File(classificadoresURL.toURI()));
		boolean firstLine = true;
		Collection<String> codes = new ArrayList<String>();
		for (String line : classificadores) {
			
			if (firstLine) {
				firstLine = false;
				continue;
			}
			
			codes.add(line.split(",")[0]);
		}
		return codes;
	}

	
	private static Collection<Collection> particionar(Collection collection, int particoes) {
		Collection<Collection> retorno = new ArrayList<Collection>();
		Collection innerCollection = new ArrayList();
		
		for (Object obj : collection) {
			innerCollection.add(obj);
			
			if ((innerCollection.size() % particoes) == 0) {
				retorno.add(innerCollection);
				innerCollection = new ArrayList();
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
	 * @throws IOException 
	 */
	
	public static HashMap<String,Relacionamento> processarRelacionamentosMenorDistTolerancia(Collection<Setor> setores, Collection<Estabelecimento> estabelecimentos, Map<String, Collection<Equipamento>> equipamentos, Collection<String> equipamentosAlvo, int distanciaInicial, double PercToleranciaDist, String layer, int particao) throws IOException {
		
		HashMap<String,Relacionamento> relacionamentos = new HashMap<String, Relacionamento>();
		
		
		
		for (Setor setor : setores) {
						
			
			
			for (String equipamentoProcurado : equipamentosAlvo) { 
				
				Collection<Estabelecimento> unidadesSelecionadas = new ArrayList<Estabelecimento>();
				Equipamento equipamentoOferecido = null;
				
				double menorDistancia = 1000000000;
				boolean continuaProcura = true;
				double raioPesquisa = distanciaInicial;
				
				for (Estabelecimento estabelecimento : estabelecimentos) {
					Collection<Equipamento> equipamentosEstabelecimento = equipamentos.get(estabelecimento.cnes);
					if (equipamentosEstabelecimento == null) {
						System.out.println("[WARNING] - CNES: "+ estabelecimento.cnes +" NAO POSSUI EQUIPAMENTOS!");
						continue;
					}
					boolean achou = false;
					
					for (Equipamento equipamento: equipamentosEstabelecimento) {
						if (equipamento.codigo.equalsIgnoreCase(equipamentoProcurado)) {
							equipamentoOferecido = equipamento;
							achou = true;
							break;
						}
					}
					
					if (achou) {
//						System.out.println("[Relacionamento] - SERVICO ENCONTRADO: "+ servicoProcurado +"."+classificadorProcurado + " no CNES: "+estabelecimento.cnes + " DESCRICAO: "+estabelecimento.descricao + " SERVICOS: "+estabelecimento.servicos);
						unidadesSelecionadas.add(estabelecimento);
					}
				}
				
				if (unidadesSelecionadas.size() > 0) {
					boolean expandiuRaio = false;
					do {	

						for (Estabelecimento estabelecimento : unidadesSelecionadas) {
							
							boolean match = false;
							double distance = GeoGeometry.distance(estabelecimento.latitude, estabelecimento.longitude, setor.centroMassLat, setor.centroMassLng);
													
							match = distance < raioPesquisa;
							menorDistancia = Math.min(distance, menorDistancia);
							
							
							if (match) {
								
								RelacionamentoEquipamento relacionamento = new RelacionamentoEquipamento();
								traceMatch("[Relacionamento - PART:"+particao+"]  - MATCH: "+ equipamentoProcurado + " no CNES: "+estabelecimento.cnes + " DESCRICAO: "+estabelecimento.descricao + " DISTANCIA: "+distance + " MENOR DISTANCIA: "+ menorDistancia + " RAIO PESQUISA: "+raioPesquisa);
								
								
								relacionamento.source = setor.id;
								relacionamento.target = estabelecimento.id;
								relacionamento.distancia = distance;
								relacionamento.type = "SETOR-TO-ESTABELECIMENTO-EQUIPAMENTO";
								relacionamento.subType = equipamentoOferecido.tipo +" - "+ equipamentoOferecido.getTipoDescricao();
								relacionamento.classifier = equipamentoOferecido.codigo + " - " + equipamentoOferecido.getDescricao();
										
								relacionamento.layer = layer;
								relacionamento.id = setor.id+"-"+estabelecimento.id+"-"+equipamentoOferecido.tipo+"-"+equipamentoOferecido.codigo;
								relacionamentos.put(relacionamento.id,relacionamento);
								
								traceAccepting("[ACEITANDO - PART:"+particao+"] SETOR: " + setor.id
										+ " RELACIONADO COM: " + estabelecimento.id + " DISTANCIA: " + distance + " EQUIPAMENTO: "+equipamentoProcurado );
								
							} else {
								//System.out.println("[IGNORANDO] SETOR: " + setor.id
								//		+ " RELACIONADO COM: " + estabelecimento.id + " DISTANCIA: " + distance + "RAIO DE PESQUISA: "+ raioPesquisa + " MENOR DISTANCIA: "+menorDistancia);
							}
							
							
						}
						
						raioPesquisa = (int)(menorDistancia + (menorDistancia * 0.20));
						continuaProcura = !expandiuRaio;
						expandiuRaio = true;
						
					} while (continuaProcura);
				} else {
					traceNotFound("[NAO_ENCONTRADO - PART:"+particao+"] - EQUIPAMENTO: "+equipamentoProcurado);
				}
				
			}
			count++;
			System.out.println("[EVOLUCAO PART:"+particao+"] - "+interacoes+" / "+count + " RELACIONAMENTOS: " + relacionamentos.size());
				
		}
		return relacionamentos;
	}
	
}
